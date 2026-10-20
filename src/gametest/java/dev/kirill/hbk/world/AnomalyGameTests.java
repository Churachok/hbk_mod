package dev.kirill.hbk.world;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public final class AnomalyGameTests {
	private static AnomalyZones.Zone zone(GameTestHelper test, int kind) {
		return AnomalyZones.nearest(AnomalyZones.layoutSeed(test.getLevel().getSeed()), 0, 0, kind);
	}

	@GameTest
	public void gravityScalesPlayersAndProjectilesAndMobsFreezeWithoutChangingNoAi(GameTestHelper test) {
		var level = test.getLevel();
		var zone = zone(test, 1);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
		var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
		for (var entity : java.util.List.of(player, arrow, cow)) {
			entity.setPos(0, 180, 0);
			double normal = entity.getGravity();
			entity.setPos(zone.minX() + 8.5, 180, zone.minZ() + 8.5);
			test.assertTrue(Math.abs(entity.getGravity() - normal * AnomalyZones.GRAVITY) < 1e-8, "Gravity must scale for " + entity.getType());
			entity.setPos(zone.minX() + 96, 180, zone.minZ() + 96);
			test.assertTrue(Math.abs(entity.getGravity() - normal * AnomalyZones.CENTER_GRAVITY) < 1e-8, "The center must have the weakest gravity");
			entity.setPos(zone.minX() + 144, 180, zone.minZ() + 96);
			double middleGravity = entity.getGravity();
			test.assertTrue(middleGravity > normal * AnomalyZones.CENTER_GRAVITY && middleGravity < normal * AnomalyZones.GRAVITY,
					"Gravity between the center and edge must interpolate");
			entity.setPos(zone.minX() + 144, -20, zone.minZ() + 96);
			test.assertTrue(Math.abs(entity.getGravity() - middleGravity) < 1e-8, "Height must not change the horizontal gravity field");
			entity.setPos(0, 180, 0);
			test.assertTrue(entity.getGravity() == normal, "Gravity must restore on exit");
		}
		cow.setPos(zone.minX() + 8.5, 180, zone.minZ() + 8.5);
		var original = cow.position();
		cow.setDeltaMovement(1, -1, 1);
		cow.move(MoverType.SELF, new Vec3(1, -1, 1));
		test.assertTrue(cow.position().equals(original) && cow.getDeltaMovement().equals(Vec3.ZERO), "Mobs must stay in place");
		test.assertFalse(cow.isNoAi(), "Freezing must not permanently change a mob's saved AI flag");
		player.setPos(zone.minX() + 8.5, 180, zone.minZ() + 8.5);
		player.causeFallDamage(100, 1, level.damageSources().fall());
		test.assertTrue(player.getHealth() == player.getMaxHealth(), "Falling inside an anomaly must be harmless");
		player.setPos(0, 180, 0);
		player.causeFallDamage(8, 1, level.damageSources().fall());
		test.assertTrue(player.getHealth() < player.getMaxHealth(), "Fall damage must return outside");
		test.succeed();
	}

	@GameTest
	public void suspendedBlocksFluidsRedstoneAndVehiclesObeySharpBoundary(GameTestHelper test) {
		var level = test.getLevel();
		var zone = zone(test, 2);
		var pos = new BlockPos(zone.minX() + 8, 240, zone.minZ() + 8);
		level.getChunkAt(pos);
		for (var block : java.util.List.of(Blocks.SAND, Blocks.GRAVEL, Blocks.ANVIL)) {
			level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(pos, block.defaultBlockState(), 2);
			level.getBlockState(pos).tick(level, pos, level.getRandom());
			test.assertTrue(level.getBlockState(pos).is(block), "Falling blocks must remain suspended");
		}
		for (var block : java.util.List.of(Blocks.WATER, Blocks.LAVA)) {
			level.setBlock(pos, block.defaultBlockState(), 2);
			level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
			((net.minecraft.world.level.material.FlowingFluid) level.getFluidState(pos).getType()).tick(level, pos, level.getBlockState(pos), level.getFluidState(pos));
			test.assertTrue(level.getBlockState(pos.below()).isAir(), "Liquids must not flow down");
		}
		level.setBlock(pos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
		test.assertTrue(level.getSignal(pos, Direction.UP) == 0, "Redstone sources must emit no power");
		var boundary = new BlockPos(zone.minX(), 270, zone.minZ() + 8);
		level.setBlock(boundary.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
		test.assertFalse(level.hasNeighborSignal(boundary), "Outside power must not enter a zone");
		test.assertTrue(level.getSignal(boundary.west(), Direction.UP) == 15, "Outside redstone must still work");
		var boat = EntityTypes.OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
		boat.setPos(pos.getX(), 275, pos.getZ());
		var original = boat.position();
		boat.move(MoverType.SELF, new Vec3(2, 1, 2));
		test.assertTrue(boat.position().equals(original), "Transport must not move inside an anomaly");
		test.succeed();
	}

	@GameTest
	public void allThreeTerrainShapesUseNativeBlocksAndHaveBedrockBorders(GameTestHelper test) {
		var level = test.getLevel();
		for (int kind = 1; kind <= 3; kind++) {
			var zone = zone(test, kind);
			// Sample the border and a section through each shape, without loading world chunks.
			for (int offset : new int[]{0, 96}) {
				var chunk = new ProtoChunk(new ChunkPos((zone.minX() + offset) >> 4, (zone.minZ() + 80) >> 4),
						UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
				var pos = new BlockPos.MutableBlockPos();
				for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
					int wx = chunk.getPos().getMinBlockX() + x, wz = chunk.getPos().getMinBlockZ() + z;
					for (int y = 48; y < 64; y++) chunk.setBlockState(pos.set(wx, y, wz), Blocks.STONE.defaultBlockState());
					chunk.setBlockState(pos.set(wx, 64, wz), Blocks.SAND.defaultBlockState());
				}
				Heightmap.primeHeightmaps(chunk, java.util.Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
				AnomalyTerrain.decorate(level, chunk);
				int wx = chunk.getPos().getMinBlockX(), wz = chunk.getPos().getMinBlockZ();
				if (offset == 0) {
					test.assertTrue(chunk.getBlockState(new BlockPos(wx, 65, wz)).is(Blocks.BEDROCK), "Every zone must have a bedrock border on the ground");
				} else {
					boolean extraTerrain = false;
					for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 80; y < 280; y++) {
						var state = chunk.getBlockState(pos.set(wx + x, y, wz + z));
						extraTerrain |= !state.isAir();
						test.assertTrue(state.isAir() || state.is(Blocks.STONE) || state.is(Blocks.SAND), "Terrain must inherit native palette");
					}
					test.assertTrue(extraTerrain, "Zone " + kind + " must generate elevated terrain");
				}
			}
		}
		test.succeed();
	}

	@GameTest
	public void craterCutsThroughBedrockAndFluidsWhileKeepingWallsAndBorder(GameTestHelper test) {
		var level = test.getLevel();
		var zone = zone(test, 4);
		for (int offset : new int[]{0, 48, 96}) {
			var chunk = new ProtoChunk(new ChunkPos((zone.minX() + offset) >> 4, (zone.minZ() + 80) >> 4),
					UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
			int wx = chunk.getPos().getMinBlockX(), wz = chunk.getPos().getMinBlockZ(), minY = level.getMinY();
			var pos = new BlockPos.MutableBlockPos();
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				for (int y = minY; y < 64; y++) {
					chunk.setBlockState(pos.set(wx + x, y, wz + z), (y < minY + 5 ? Blocks.BEDROCK
							: y < 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState());
				}
				chunk.setBlockState(pos.set(wx + x, 64, wz + z), Blocks.GRASS_BLOCK.defaultBlockState());
			}
			// A pre-existing fluid column and tall vegetation must not hang across the opening.
			if (offset == 96) {
				for (int y = 65; y < 80; y++) chunk.setBlockState(pos.set(wx, y, wz), Blocks.WATER.defaultBlockState());
				chunk.setBlockState(pos.set(wx, level.getMaxY() - 1, wz), Blocks.OAK_LEAVES.defaultBlockState());
				chunk.setBlockState(pos.set(wx + 1, 100, wz), Blocks.CHEST.defaultBlockState());
			}
			Heightmap.primeHeightmaps(chunk, java.util.Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
			AnomalyTerrain.decorate(level, chunk);
			if (offset == 0) {
				test.assertTrue(chunk.getBlockState(pos.set(wx, 65, wz)).is(Blocks.BEDROCK), "The outer border must remain above the intact rim");
				test.assertTrue(chunk.getBlockState(pos.set(wx, 64, wz)).is(Blocks.GRASS_BLOCK), "The biome surface outside the funnel must survive");
			} else if (offset == 48) {
				test.assertTrue(chunk.getBlockState(pos.set(wx, 64, wz)).isAir(), "Crater walls must descend below the native surface");
				test.assertTrue(chunk.getBlockState(pos.set(wx, minY + 8, wz)).is(Blocks.DEEPSLATE), "Walls must retain their natural underground strata");
			} else {
				for (int y = minY; y < level.getMaxY(); y++) {
					test.assertTrue(chunk.getBlockState(pos.set(wx, y, wz)).isAir(), "The central opening must remove even bedrock, water and high leaves");
				}
				test.assertTrue(chunk.getBlockState(pos.set(wx + 1, 100, wz)).is(Blocks.CHEST), "World generation must preserve structure containers");
			}
		}
		test.succeed();
	}

	@GameTest
	public void invertedTerrainIncludesAFlatWorldAtMinimumHeight(GameTestHelper test) {
		var level = test.getLevel();
		var zone = zone(test, 1);
		var chunk = new ProtoChunk(new ChunkPos((zone.minX() + 96) >> 4, (zone.minZ() + 96) >> 4),
				UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
		int wx = chunk.getPos().getMinBlockX(), wz = chunk.getPos().getMinBlockZ();
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			chunk.setBlockState(new BlockPos(wx + x, level.getMinY(), wz + z), Blocks.BEDROCK.defaultBlockState());
			chunk.setBlockState(new BlockPos(wx + x, level.getMinY() + 1, wz + z), Blocks.DIRT.defaultBlockState());
			chunk.setBlockState(new BlockPos(wx + x, level.getMinY() + 2, wz + z), Blocks.GRASS_BLOCK.defaultBlockState());
		}
		Heightmap.primeHeightmaps(chunk, java.util.Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
		AnomalyTerrain.decorate(level, chunk);
		test.assertTrue(chunk.getBlockState(new BlockPos(wx, 260, wz)).is(Blocks.GRASS_BLOCK),
				"The inverted copy must include terrain within the first few buildable layers");
		test.assertFalse(chunk.getBlockState(new BlockPos(wx, 262, wz)).is(Blocks.BEDROCK), "Do not duplicate the world's bedrock floor overhead");
		test.succeed();
	}

	@GameTest(maxTicks = 80)
	public void transientTerrainReturnsAfterFortyTicksAndProtectsSpecialBlocks(GameTestHelper test) {
		var level = test.getLevel();
		var zone = zone(test, 2);
		var pos = new BlockPos(zone.minX() + 24, 230, zone.minZ() + 24);
		level.getChunkAt(pos);
		// The test save can contain an unfinished gap from a previous, interrupted run.
		AnomalyTransientBlocks.restore(level, true);
		for (var block : java.util.List.of(Blocks.BEDROCK, Blocks.DIAMOND_ORE, Blocks.CHEST)) {
			level.setBlock(pos, block.defaultBlockState(), 2);
			test.assertFalse(AnomalyTransientBlocks.vanish(level, pos), "Protected blocks must not disappear");
		}
		level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
		test.assertTrue(AnomalyTransientBlocks.vanish(level, pos), "Natural stone inside an anomaly must be eligible");
		test.assertTrue(level.getBlockState(pos).isAir(), "A temporary gap must have no collision block");
		AnomalyTransientBlocks.restore(level, false);
		test.assertTrue(level.getBlockState(pos).isAir(), "A gap must not close before its deadline");
		test.runAfterDelay(39, () -> {
			AnomalyTransientBlocks.restore(level, false);
			test.assertTrue(level.getBlockState(pos).isAir(), "A gap must remain for the first 39 ticks");
		});
		test.runAfterDelay(41, () -> {
			AnomalyTransientBlocks.restore(level, false);
			test.assertTrue(level.getBlockState(pos).is(Blocks.STONE), "The original stone must return after 40 ticks");
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			test.succeed();
		});
	}

	@GameTest
	public void fastFlightCannotTunnelThroughCeilingWhileCrossingTheZoneEdge(GameTestHelper test) {
		var zone = zone(test, 1);
		var player = test.makeMockServerPlayerInLevel();
		player.setPos(zone.minX() - 2, 310, zone.minZ() + 96);
		test.assertTrue(AnomalyZones.touchesCeiling(player, new Vec3(zone.minX() + 2, 300, zone.minZ() + 96)),
				"A flight segment crossing the ceiling must be lethal even if it ends outside the zone");
		test.assertFalse(AnomalyZones.touchesCeiling(player, player.position()), "Flying outside the zone must be safe");
		player.setPos(zone.minX() + 4, 303, zone.minZ() + 96);
		test.assertTrue(AnomalyZones.touchesCeiling(player, player.position()), "Standing against the image must count as contact");
		player.setPos(0, 80, 0);
		test.succeed();
	}
}
