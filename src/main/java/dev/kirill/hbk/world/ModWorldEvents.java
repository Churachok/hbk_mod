package dev.kirill.hbk.world;

import dev.kirill.hbk.entity.StalinEntity;
import dev.kirill.hbk.mechanic.UraniumArmorEffects;
import dev.kirill.hbk.player.MechanicsPlayerData;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class ModWorldEvents {
	private static final int VILLAGE_INTERVAL = 40;
	private static final int STRUCTURE_INTERVAL = 80;
	private static final int RATION_INTERVAL = 20 * 60 * 10;

	private ModWorldEvents() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(ModWorldEvents::onEndLevelTick);
	}

	private static void onEndLevelTick(ServerLevel level) {
		if (!level.getServer().isReady()) {
			return;
		}
		if (level.players().isEmpty()) {
			return;
		}

		long time = level.getGameTime();
		for (ServerPlayer player : level.players()) {
			tickCondensedMilk(player);
			tickRation(player);
			tickGooseSounds(player);
			tickRadioactiveWasteland(level, player, time);
			if (time % 5L == 0L && isSilhouette(player)) {
				panicNearbyMobs(level, player);
			}
		}
		StrangeChestManager.tick(level);
		if (!level.dimension().equals(Level.OVERWORLD)) {
			return;
		}

		if (time % VILLAGE_INTERVAL == 0) {
			for (ServerPlayer player : level.players()) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(player.chunkPosition().x(), player.chunkPosition().z());
				if (chunk != null) {
					VillageNkvdSpawner.trySpawnInChunk(level, chunk);
				}
			}
		}

		if (time % STRUCTURE_INTERVAL == 0) {
			for (ServerPlayer player : level.players()) {
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						LevelChunk nearby = level.getChunkSource().getChunkNow(player.chunkPosition().x() + dx, player.chunkPosition().z() + dz);
						if (nearby != null) {
							ReferenceNpcSpawning.trySpawnSasha(level, nearby);
						}
					}
				}
			}
			for (ServerPlayer player : level.players()) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(player.chunkPosition().x(), player.chunkPosition().z());
				if (chunk != null) {
					ModStructurePopulator.tryPopulateInChunk(level, chunk);
				}
			}
		}
	}

	private static void tickRadioactiveWasteland(ServerLevel level, ServerPlayer player, long time) {
		if (time % 40L != 0L || player.isCreative() || player.isSpectator()
				|| !level.getBiome(player.blockPosition()).is(ModWorldgen.RADIOACTIVE_WASTELAND)
				|| UraniumArmorEffects.hasRadiationProtection(player)) {
			return;
		}

		float damage = 1.0f;
		if (level.isRainingAt(player.blockPosition())) {
			damage += 1.5f;
		}
		if (isNearUranium(level, player.blockPosition(), 5)) {
			damage += 2.0f;
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, true, true));
		}
		if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			damage *= 0.35f;
		}
		player.hurtServer(level, level.damageSources().magic(), damage);
	}

	private static boolean isNearUranium(ServerLevel level, BlockPos center, int radius) {
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
				center.offset(radius, radius, radius))) {
			if (level.getBlockState(pos).is(dev.kirill.hbk.registry.ModBlocks.URANIUM_ORE)) {
				return true;
			}
		}
		return false;
	}

	private static void tickCondensedMilk(ServerPlayer player) {
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		int remaining = data.hbk$getSweetLifeTicks();
		boolean countdownActive = remaining > 0;
		if (remaining > 0) {
			remaining--;
			data.hbk$setSweetLifeTicks(remaining);
		}
		if (countdownActive && remaining == 0 && !player.hasEffect(ModEffects.DROWSINESS)) {
			// A finished countdown is marked negative so sleep is triggered only once.
			data.hbk$setSweetLifeTicks(-1);
			player.addEffect(new MobEffectInstance(ModEffects.DROWSINESS, 20 * 5, 0, false, true, true));
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.hbk.condensed_milk.asleep"));
		}
		if (player.hasEffect(ModEffects.DROWSINESS)) {
			Vec3 movement = player.getDeltaMovement();
			player.setDeltaMovement(0.0, movement.y, 0.0);
		}
	}

	private static boolean isSilhouette(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WESTERN_CHESTPLATE);
	}

	private static void panicNearbyMobs(ServerLevel level, ServerPlayer player) {
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), player.getBoundingBox().inflate(16.0), Entity::isAlive)) {
			if (mob instanceof StalinEntity || mob.hasEffect(ModEffects.SOULFULNESS)) {
				continue;
			}
			Vec3 away = mob.position().subtract(player.position());
			if (away.lengthSqr() < 0.01) {
				away = new Vec3(level.getRandom().nextDouble() - 0.5, 0.0, level.getRandom().nextDouble() - 0.5);
			}
			away = away.normalize().scale(12.0);
			mob.setTarget(null);
			mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.55);
		}
	}

	private static void tickRation(ServerPlayer player) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		int ticks = data.hbk$getRationTicks() + 1;
		if (ticks >= RATION_INTERVAL) {
			ticks -= RATION_INTERVAL;
			ItemStack ration = new ItemStack(ModItems.RATION);
			if (!player.getInventory().add(ration)) {
				player.drop(ration, false);
			}
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.hbk.ration.received"));
		}
		data.hbk$setRationTicks(ticks);
	}

	private static void tickGooseSounds(ServerPlayer player) {
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		int ticks = data.hbk$getGooseTicks();
		if (ticks <= 0) {
			return;
		}
		if (ticks % 20 == 0) {
			player.playSound(ModSounds.GOOSE_HONK, 1.2f, 0.88f + player.getRandom().nextFloat() * 0.24f);
		}
		data.hbk$setGooseTicks(ticks - 1);
	}
}
