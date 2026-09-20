package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.FoundingPenisProjectileEntity;
import dev.kirill.hbk.item.MemberDestruction;
import dev.kirill.hbk.player.MechanicsPlayerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Keeps the temporary giant form on the player, so their inventory and controls remain intact. */
public final class ProgenitorTransformation {
	public static final int DURATION_TICKS = 60 * 20;
	public static final float SCALE = 16.0f;
	public static final float WIDTH = 0.6f * SCALE;
	public static final float HEIGHT = 1.8f * SCALE;
	public static final float TRANSFORMATION_EXPLOSION_POWER = 20.0f;
	public static final float PROJECTILE_EXPLOSION_POWER = 12.0f;
	public static final float BLAST_EXPLOSION_POWER = 10.0f;
	public static final int BLAST_COOLDOWN_TICKS = 60 * 20;
	public static final Identifier SCALE_MODIFIER_ID = HbkMod.id("progenitor_scale");
	private static final AttributeModifier SCALE_MODIFIER = new AttributeModifier(
			SCALE_MODIFIER_ID, SCALE - 1.0, AttributeModifier.Operation.ADD_VALUE
	);

	private ProgenitorTransformation() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	public static boolean isActive(Player player) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		return scale != null && scale.hasModifier(SCALE_MODIFIER_ID);
	}

	public static boolean start(ServerPlayer player) {
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		if (player.isSpectator() || !player.isAlive() || player.isPassenger()
				|| data.hbk$getProgenitorTicks() > 0 || isActive(player)) {
			return false;
		}
		double halfWidth = WIDTH / 2.0;
		AABB giantBounds = new AABB(
				player.getX() - halfWidth, player.getY(), player.getZ() - halfWidth,
				player.getX() + halfWidth, player.getY() + HEIGHT, player.getZ() + halfWidth
		);
		if (!player.level().noCollision(player, giantBounds)) {
			player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.no_space"));
			return false;
		}
		data.hbk$setProgenitorTicks(DURATION_TICKS);
		setScaled(player, true);
		player.setIgnoreFallDamageFromCurrentImpulse(true, player.position());
		player.applyPostImpulseGraceTime(40);
		MemberDestruction.explode((ServerLevel) player.level(), player,
				player.position().add(0.0, HEIGHT / 2.0, 0.0), TRANSFORMATION_EXPLOSION_POWER);
		var advancement = player.level().getServer().getAdvancements().get(HbkMod.id("founding_penis"));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "transform");
		}
		player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.started"));
		return true;
	}

	public static boolean fireProjectile(ServerPlayer player) {
		if (!isActive(player) || player.isSpectator() || !player.isAlive()) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		Vec3 direction = player.getLookAngle();
		Vec3 origin = player.getEyePosition().add(direction.scale(5.5));
		FoundingPenisProjectileEntity projectile = new FoundingPenisProjectileEntity(level, player);
		projectile.setPos(origin.x, origin.y, origin.z);
		projectile.shoot(direction.x, direction.y, direction.z, 2.5f, 0.0f);
		if (!level.addFreshEntity(projectile)) {
			return false;
		}
		level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
				SoundSource.PLAYERS, 2.0f, 0.5f);
		return true;
	}

	public static boolean blast(ServerPlayer player) {
		if (!isActive(player) || player.isSpectator() || !player.isAlive()) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		long now = level.getServer().overworld().getGameTime();
		long readyAt = data.hbk$getFoundingBlastReadyTick();
		if (now < readyAt) {
			long remainingSeconds = (readyAt - now + 19) / 20;
			player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.cooldown", remainingSeconds));
			return false;
		}
		Vec3 target = MemberDestruction.aimedPosition(level, player, 48.0);
		BlockPos targetBlock = BlockPos.containing(target);
		if (!level.isLoaded(targetBlock) || !level.getWorldBorder().isWithinBounds(targetBlock)) {
			return false;
		}
		MemberDestruction.explode(level, player, target, BLAST_EXPLOSION_POWER);
		data.hbk$setFoundingBlastReadyTick(now + BLAST_COOLDOWN_TICKS);
		return true;
	}

	public static void tick(ServerPlayer player) {
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		int ticks = data.hbk$getProgenitorTicks();
		if (ticks <= 0 || !player.isAlive() || player.isSpectator()) {
			if (ticks > 0) {
				data.hbk$setProgenitorTicks(0);
			}
			setScaled(player, false);
			return;
		}
		if (ticks == 1) {
			data.hbk$setProgenitorTicks(0);
			setScaled(player, false);
			player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.ended"));
		} else {
			setScaled(player, true);
			data.hbk$setProgenitorTicks(ticks - 1);
		}
	}

	private static void setScaled(ServerPlayer player, boolean enabled) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale == null || scale.hasModifier(SCALE_MODIFIER_ID) == enabled) {
			return;
		}
		if (enabled) {
			scale.addOrUpdateTransientModifier(SCALE_MODIFIER);
		} else {
			scale.removeModifier(SCALE_MODIFIER_ID);
		}
		player.refreshDimensions();
	}
}
