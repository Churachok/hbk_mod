package dev.kirill.hbk.world;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ReferenceNpcSpawning {
	private ReferenceNpcSpawning() {}

	public static void register() {
		// Use vanilla's continuously retried monster spawner and cap, including already explored chunks.
		for (var type : java.util.List.of(ModEntityTypes.ANTON, ModEntityTypes.GRISHA)) {
			BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, type, 10, 4, 4);
			SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					Monster::checkSurfaceMonstersSpawnRules);
		}
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.MONSTER, ModEntityTypes.GOSHA, 10, 4, 4);
		SpawnPlacements.register(ModEntityTypes.GOSHA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(Blocks.SAND)
						&& Monster.checkSurfaceMonstersSpawnRules(type, level, reason, pos, random));
		BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(net.minecraft.world.entity.EntityTypes.SHEEP),
				MobCategory.MONSTER, ModEntityTypes.PINK_FURRY_WOLF, 14, 4, 4);
		SpawnPlacements.register(ModEntityTypes.PINK_FURRY_WOLF, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
						&& Monster.checkSurfaceMonstersSpawnRules(type, level, reason, pos, random));
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ModEntityTypes.SASHA, 5, 1, 1);
		SpawnPlacements.register(ModEntityTypes.SASHA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkSurfaceMonstersSpawnRules);
	}
}
