package dev.kirill.hbk.world;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ReferenceNpcSpawning {
	private ReferenceNpcSpawning() {
	}

	public static void register() {
		// Match the vanilla pig/wolf biome lists without replacing their spawns.
		BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(net.minecraft.world.entity.EntityTypes.PIG),
				MobCategory.CREATURE, ModEntityTypes.ANTON, 3, 1, 2);
		BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(net.minecraft.world.entity.EntityTypes.WOLF),
				MobCategory.CREATURE, ModEntityTypes.GRISHA, 2, 1, 2);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT),
				MobCategory.CREATURE, ModEntityTypes.GOSHA, 2, 1, 1);
		// Copy the vanilla sheep spawn entry: the same biomes, weight 12 and groups of four.
		BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(net.minecraft.world.entity.EntityTypes.SHEEP),
				MobCategory.CREATURE, ModEntityTypes.PINK_FURRY_WOLF, 12, 4, 4);
		for (var type : java.util.List.of(ModEntityTypes.ANTON, ModEntityTypes.GRISHA)) {
			SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
							&& level.getRawBrightness(pos, 0) > 8);
		}
		SpawnPlacements.register(ModEntityTypes.GOSHA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SAND)
						&& level.getRawBrightness(pos, 0) > 8);
		SpawnPlacements.register(ModEntityTypes.PINK_FURRY_WOLF, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
						&& level.getRawBrightness(pos, 0) > 8);
	}

	/** One saved 10% roll per 20 explored chunks, followed by a 50% roll for Lesha. */
	public static void trySpawnSasha(ServerLevel level, LevelChunk chunk) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)
				|| !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
			return;
		}
		ModWorldData data = ModWorldData.get(level);
		long key = chunk.getPos().pack();
		if (data.isNpcChunkHandled(key)) {
			return;
		}
		// Leave chunks close to a player pending until a safe spawning distance is available.
		BlockPos center = chunk.getPos().getMiddleBlockPosition(level.getSeaLevel());
		for (Player player : level.players()) {
			double dx = player.getX() - center.getX();
			double dz = player.getZ() - center.getZ();
			if (dx * dx + dz * dz < 40.0 * 40.0) {
				return;
			}
		}
		data.markNpcChunkHandled(key);
		if (!data.isSashaSpawnRollDue() || level.getRandom().nextFloat() >= 0.10f) {
			return;
		}
		for (int attempt = 0; attempt < 16; attempt++) {
			int x = chunk.getPos().getMinBlockX() + level.getRandom().nextInt(16);
			int z = chunk.getPos().getMinBlockZ() + level.getRandom().nextInt(16);
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
			if (!canStand(level, pos)) {
				continue;
			}
			var sasha = ModEntityTypes.SASHA.spawn(level, pos, EntitySpawnReason.NATURAL);
			if (sasha == null) {
				continue;
			}
			if (level.getRandom().nextFloat() < 0.50f) {
				for (BlockPos companionPos : java.util.List.of(pos.east(), pos.west(), pos.north(), pos.south())) {
					if (level.hasChunkAt(companionPos) && canStand(level, companionPos)
							&& ModEntityTypes.LESHA.spawn(level, companionPos, EntitySpawnReason.NATURAL) != null) {
						break;
					}
				}
			}
			return;
		}
	}

	private static boolean canStand(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos.below()).isSolid();
	}
}
