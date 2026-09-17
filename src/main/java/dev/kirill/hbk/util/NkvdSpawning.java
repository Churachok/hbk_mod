package dev.kirill.hbk.util;

import dev.kirill.hbk.entity.NkvdEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;

public final class NkvdSpawning {
	private NkvdSpawning() {
	}

	public static int spawnSquad(ServerLevel level, BlockPos origin, int count) {
		return spawnAround(level, origin, 8, count, EntitySpawnReason.REINFORCEMENT);
	}

	public static int spawnSquadInArea(ServerLevel level, BlockPos origin, Vec3i size, int count) {
		int spawned = 0;
		int width = Math.max(1, size.getX());
		int depth = Math.max(1, size.getZ());
		int height = Math.max(1, size.getY());
		for (int attempt = 0; attempt < count * 12 && spawned < count; attempt++) {
			int x = origin.getX() + level.getRandom().nextInt(width);
			int z = origin.getZ() + level.getRandom().nextInt(depth);
			BlockPos pos = findStandable(level, x, origin.getY(), z, height);
			if (pos != null && ModEntityTypes.NKVD.spawn(level, pos, EntitySpawnReason.STRUCTURE) != null) {
				spawned++;
			}
		}
		if (spawned > 0) {
			level.playSound(null, origin, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3.0f, 0.7f);
		}
		return spawned;
	}

	public static NkvdEntity spawnVillageRaider(ServerLevel level, BlockPos pos) {
		NkvdEntity nkvd = ModEntityTypes.NKVD.spawn(level, pos, EntitySpawnReason.STRUCTURE);
		if (nkvd != null) {
			nkvd.setVillageRaider(true);
		}
		return nkvd;
	}

	private static int spawnAround(ServerLevel level, BlockPos origin, int radius, int count, EntitySpawnReason reason) {
		int spawned = 0;
		int span = radius * 2 + 1;
		for (int attempt = 0; attempt < count * 4 && spawned < count; attempt++) {
			int ox = level.getRandom().nextInt(span) - radius;
			int oz = level.getRandom().nextInt(span) - radius;
			BlockPos pos = origin.offset(ox, 0, oz);
			if (ModEntityTypes.NKVD.spawn(level, pos, reason) != null) {
				spawned++;
			}
		}
		if (spawned > 0) {
			level.playSound(null, origin, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3.0f, 0.7f);
		}
		return spawned;
	}

	private static BlockPos findStandable(ServerLevel level, int x, int minY, int z, int height) {
		int maxY = minY + height;
		for (int y = maxY; y >= minY; y--) {
			BlockPos feet = new BlockPos(x, y, z);
			BlockPos below = feet.below();
			BlockState ground = level.getBlockState(below);
			BlockState air = level.getBlockState(feet);
			if (!ground.isAir() && ground.isSolid() && air.isAir()) {
				return feet;
			}
		}
		return new BlockPos(x, minY + 1, z);
	}
}
