package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class WorkerMobLogic {
	public static final int MAX_HUNGER_TICKS = 20 * 60 * 10;
	private static final double SEARCH_RADIUS = 16.0;
	private static final double PICKUP_DISTANCE_SQR = 2.25;

	private WorkerMobLogic() {
	}

	public static void makeConscious(Mob mob, ServerPlayer owner) {
		WorkerMobData data = (WorkerMobData) mob;
		data.hbk$setWorkerOwner(owner.getUUID());
		data.hbk$setWorkerHunger(MAX_HUNGER_TICKS);
		mob.setPersistenceRequired();
		mob.setTarget(null);
		mob.setCustomName(Component.translatable("entity.hbk.conscious_worker"));
		mob.setCustomNameVisible(true);
		owner.sendSystemMessage(Component.translatable("message.hbk.worker.awakened", mob.getName()));
	}

	public static void tick(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) {
			return;
		}
		WorkerMobData data = (WorkerMobData) mob;
		UUID ownerId = data.hbk$getWorkerOwner();
		if (ownerId == null) {
			return;
		}

		mob.setTarget(null);
		int hunger = Math.max(0, data.hbk$getWorkerHunger() - 1);
		data.hbk$setWorkerHunger(hunger);
		if (hunger == 0) {
			mob.getNavigation().stop();
			if (mob.tickCount % (20 * 10) == 0) {
				Entity owner = level.getEntityInAnyDimension(ownerId);
				if (mob.getHealth() <= 2.0f && owner instanceof ServerPlayer player) {
					player.sendSystemMessage(Component.translatable("message.hbk.worker.starved", mob.getName()));
				}
				mob.hurtServer(level, mob.damageSources().starve(), 2.0f);
			}
			return;
		}

		if (mob.tickCount % 10 != 0) {
			return;
		}

		List<? extends ItemEntity> resources = level.getEntities(
				EntityTypeTest.forClass(ItemEntity.class),
				mob.getBoundingBox().inflate(SEARCH_RADIUS),
				item -> item.isAlive() && !item.getItem().isEmpty() && !item.getItem().is(ModItems.SICKLE_AND_HAMMER)
		);
		ItemEntity nearest = resources.stream()
				.min(Comparator.comparingDouble(mob::distanceToSqr))
				.orElse(null);
		if (nearest != null) {
			if (mob.distanceToSqr(nearest) <= PICKUP_DISTANCE_SQR) {
				giveResourceToOwner(level, ownerId, nearest);
			} else {
				mob.getNavigation().moveTo(nearest, 1.2);
			}
			return;
		}

		Entity owner = level.getEntityInAnyDimension(ownerId);
		if (owner instanceof ServerPlayer player && player.level() == level && mob.distanceToSqr(player) > 36.0) {
			mob.getNavigation().moveTo(player, 1.05);
		}
	}

	private static void giveResourceToOwner(ServerLevel level, UUID ownerId, ItemEntity itemEntity) {
		Entity owner = level.getEntityInAnyDimension(ownerId);
		if (!(owner instanceof ServerPlayer player) || player.level() != level) {
			return;
		}
		ItemStack resource = itemEntity.getItem().copy();
		player.getInventory().add(resource);
		if (!resource.isEmpty()) {
			player.drop(resource, false);
		}
		itemEntity.discard();
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, itemEntity.getX(), itemEntity.getY() + 0.25, itemEntity.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
	}
}
