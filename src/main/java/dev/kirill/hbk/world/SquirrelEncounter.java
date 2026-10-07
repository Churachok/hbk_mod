package dev.kirill.hbk.world;

import dev.kirill.hbk.entity.SquirrelEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SquirrelEncounter {
	private static final double CHANCE_PER_SECOND = 0.002;
	private static final Map<UUID, SquirrelEntity> ACTIVE = new HashMap<>();

	private SquirrelEncounter() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SquirrelEncounter::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}

	private static void tick(MinecraftServer server) {
		ACTIVE.entrySet().removeIf(entry -> !entry.getValue().isAlive() || entry.getValue().isRemoved());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.tickCount % 20 == 0 && player.isAlive() && !player.isSpectator()
					&& player.onGround() && !player.isPassenger() && !UnknownEncounter.isActive(player)
					&& !ACTIVE.containsKey(player.getUUID()) && player.getRandom().nextDouble() < CHANCE_PER_SECOND) {
				start(player);
			}
		}
	}

	public static StartResult start(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) {
			return StartResult.UNAVAILABLE;
		}
		SquirrelEntity existing = ACTIVE.get(player.getUUID());
		if (existing != null && existing.isAlive() && !existing.isRemoved()) {
			return StartResult.ALREADY_ACTIVE;
		}
		ServerLevel level = player.level();
		SquirrelEntity squirrel = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		if (squirrel == null) {
			return StartResult.UNAVAILABLE;
		}
		squirrel.setEncounterOwner(player);
		// Search around the player's current height, including caves, without generating chunks.
		float initialAngle = player.getRandom().nextFloat() * (float) (Math.PI * 2.0);
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = initialAngle + attempt * Math.PI / 8.0;
			double radius = 4.0 + attempt % 4;
			BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * radius,
					player.getY() + 2, player.getZ() + Math.sin(angle) * radius);
			if (!level.hasChunkAt(column)) {
				continue;
			}
			for (int offset = 0; offset <= 5; offset++) {
				BlockPos feet = column.below(offset);
				var ground = level.getBlockState(feet.below());
				if (!ground.isFaceSturdy(level, feet.below(), Direction.UP)
						|| ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CAMPFIRE)
						|| ground.is(Blocks.SOUL_CAMPFIRE) || ground.is(Blocks.CACTUS)
						|| level.getBlockState(feet).is(Blocks.FIRE) || level.getBlockState(feet).is(Blocks.SOUL_FIRE)
						|| level.getBlockState(feet).is(Blocks.SWEET_BERRY_BUSH)
						|| !level.getFluidState(feet).isEmpty()) {
					continue;
				}
				squirrel.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
						player.getYRot() + 180.0f, 0.0f);
				if (level.getWorldBorder().isWithinBounds(squirrel.getBoundingBox()) && level.noCollision(squirrel)
						&& level.addFreshEntity(squirrel)) {
					ACTIVE.put(player.getUUID(), squirrel);
					return StartResult.STARTED;
				}
			}
		}
		squirrel.discard();
		return StartResult.BLOCKED;
	}

	public enum StartResult {
		STARTED, BLOCKED, ALREADY_ACTIVE, UNAVAILABLE
	}
}
