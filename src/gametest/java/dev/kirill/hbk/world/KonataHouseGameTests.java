package dev.kirill.hbk.world;

import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KonataEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class KonataHouseGameTests {
	@GameTest
	public void rotatedHouseCarvesTheRoomButPreservesSoilAroundItsHiddenShaft(GameTestHelper test) {
		var level = test.getLevel();
		for (var rotation : Rotation.values()) {
			BlockPos origin = origin(test, 0, rotation);
			BoundingBox box = placeHouse(test, origin, rotation, true);
			try {
				for (int x = 0; x <= 8; x++) {
					for (int z = 0; z <= 8; z++) {
						test.assertTrue(level.getBlockState(at(origin, rotation, x, 0, z)).is(Blocks.CHERRY_PLANKS),
								"Every basement floor block must be cherry planks");
						var ceiling = level.getBlockState(at(origin, rotation, x, 5, z));
						test.assertTrue(x == 2 && z == 2 ? ceiling.is(Blocks.LADDER)
								: x == 2 && z == 5 || x == 6 && z == 2 ? ceiling.is(Blocks.GLOWSTONE)
								: ceiling.is(Blocks.CHERRY_PLANKS), "The room must have a closed, lit cherry ceiling");
						for (int y = 1; y <= 4; y++) {
							var state = level.getBlockState(at(origin, rotation, x, y, z));
							if (x == 0 || x == 8 || z == 0 || z == 8) {
								test.assertTrue(state.is(Blocks.CHERRY_PLANKS), "The basement shell must be cherry on every side");
							} else if (!(x == 2 && (z == 1 || z == 2))
									&& !(y == 2 && z == 5 && x >= 5)
									&& !(y == 1 && z == 5 && (x == 3 || x == 4))) {
								test.assertTrue(state.isAir(), "The seven-by-seven chamber must actually carve out pre-existing stone");
							}
						}
					}
				}
				for (int y = 1; y <= 10; y++) {
					var ladder = level.getBlockState(at(origin, rotation, 2, y, 2));
					test.assertTrue(ladder.is(Blocks.LADDER)
							&& ladder.getValue(BlockStateProperties.HORIZONTAL_FACING) == rotation.rotate(Direction.SOUTH),
							"A correctly rotated ladder must continuously connect room and house");
				}
				var hatch = level.getBlockState(at(origin, rotation, 2, 11, 2));
				test.assertTrue(hatch.is(Blocks.CHERRY_TRAPDOOR)
						&& hatch.getValue(BlockStateProperties.HORIZONTAL_FACING) == rotation.rotate(Direction.SOUTH)
						&& hatch.getValue(BlockStateProperties.HALF) == Half.TOP
						&& !hatch.getValue(BlockStateProperties.OPEN)
						&& level.getBlockState(at(origin, rotation, 2, 12, 2)).is(Blocks.CARPET.pink()),
						"The top-half cherry hatch must remain hidden under the original pink carpet");
				test.assertTrue(level.getBlockState(at(origin, rotation, 5, 8, 5)).is(Blocks.STONE)
						&& level.getBlockState(at(origin, rotation, 1, 8, 2)).is(Blocks.CHERRY_PLANKS),
						"Only the lined access shaft, not all soil between house and basement, may be replaced");
				assertMechanism(test, origin, rotation);
			} finally {
				clear(level, box);
			}
		}
		test.succeed();
	}

	@GameTest(maxTicks = 240)
	public void eachRotationSeatsOnePersistentWhiteAdultSheepAndNeverRespawnsIt(GameTestHelper test) {
		runInTickingChunks(test, 1, false, () -> checkEachRotationSeatsOnePersistentWhiteAdultSheep(test));
	}

	private void checkEachRotationSeatsOnePersistentWhiteAdultSheep(GameTestHelper test) {
		var level = test.getLevel();
		for (var rotation : Rotation.values()) {
			BlockPos origin = origin(test, 1, rotation);
			BoundingBox box = placeHouse(test, origin, rotation, false);
			var data = new ModWorldData();
			String key = "test_konata_basement@" + origin.asLong();
			try {
				ModStructurePopulator.populateKonataBasement(level, origin, rotation, data, key);
				var boats = boats(level, box);
				var sheep = sheep(level, box);
				test.assertTrue(boats.size() == 1 && sheep.size() == 1 && data.isStructurePopulated(key),
						"The basement must create one boat and one sheep, and then mark its independent population key");
				Boat boat = boats.getFirst();
				Sheep passenger = sheep.getFirst();
				Vec3 expected = StructureTemplate.transform(new Vec3(3.5, 1.5, 5.5), Mirror.NONE,
						rotation, BlockPos.ZERO).add(Vec3.atLowerCornerOf(origin));
				test.assertTrue(boat.position().distanceToSqr(expected) < 0.00001
						&& Math.abs(Mth.wrapDegrees(boat.getYRot() - yaw(rotation))) < 0.001
						&& boat.getType() == EntityTypes.CHERRY_BOAT,
						"The cherry boat must rest at the rotated half-block platform height and face the piston");
				test.assertTrue(passenger.getVehicle() == boat && boat.getPassengers().size() == 1
						&& passenger.getColor() == DyeColor.WHITE && !passenger.isBaby()
						&& !passenger.isSheared() && passenger.isPersistenceRequired(),
						"The already-seated sheep must be adult, white, unsheared and persistent");
				ModStructurePopulator.populateKonataBasement(level, origin, rotation, data, key);
				test.assertTrue(boats(level, box).size() == 1 && sheep(level, box).size() == 1,
						"A repeated visit must not duplicate the boat or its passenger");
				var saved = ModWorldData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
				var loaded = ModWorldData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
				passenger.discard();
				boat.discard();
				ModStructurePopulator.populateKonataBasement(level, origin, rotation, loaded, key);
				test.assertTrue(boats(level, box).isEmpty() && sheep(level, box).isEmpty(),
						"A saved population flag must prevent replacement after the original boat and sheep are destroyed");
			} finally {
				clear(level, box);
			}
		}
		test.succeed();
	}

	@GameTest(maxTicks = 240)
	public void nativeBoatSavingRestoresTheSeatedWhiteSheep(GameTestHelper test) {
		runInTickingChunks(test, 2, false, () -> checkNativeBoatSavingRestoresTheSeatedWhiteSheep(test));
	}

	private void checkNativeBoatSavingRestoresTheSeatedWhiteSheep(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos origin = origin(test, 2, Rotation.NONE);
		BoundingBox box = placeHouse(test, origin, Rotation.NONE, false);
		Entity restored = null;
		try {
			ModStructurePopulator.populateKonataBasement(level, origin, Rotation.NONE,
					new ModWorldData(), "test_konata_save@" + origin.asLong());
			var boat = boats(level, box).getFirst();
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			test.assertTrue(boat.save(output), "A normal cherry boat with a passenger must be saved");
			var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult());
			restored = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
			test.assertTrue(restored instanceof Boat && restored.getType() == EntityTypes.CHERRY_BOAT
					&& Math.abs(restored.getY() - origin.getY() - 1.5) < 0.00001
					&& restored.getPassengers().size() == 1 && restored.getPassengers().getFirst() instanceof Sheep,
					"Vanilla entity loading must restore the cherry boat and its sheep passenger together");
			var sheep = (Sheep) restored.getPassengers().getFirst();
			test.assertTrue(sheep.getVehicle() == restored && sheep.getColor() == DyeColor.WHITE
					&& !sheep.isBaby() && !sheep.isSheared() && sheep.isPersistenceRequired(),
					"Reloading must preserve the seating relationship and the persistent adult white coat");
		} finally {
			if (restored != null) restored.getSelfAndPassengers().toList().forEach(Entity::discard);
			clear(level, box);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 240)
	public void blockedBoatPlacementRetriesWithoutMarkingOrCreatingOrphans(GameTestHelper test) {
		runInTickingChunks(test, 3, false, () -> checkBlockedBoatPlacementRetriesWithoutOrphans(test));
	}

	private void checkBlockedBoatPlacementRetriesWithoutOrphans(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos origin = origin(test, 3, Rotation.NONE);
		BoundingBox box = placeHouse(test, origin, Rotation.NONE, false);
		var data = new ModWorldData();
		String key = "test_konata_blocked@" + origin.asLong();
		try {
			level.setBlockAndUpdate(at(origin, Rotation.NONE, 3, 1, 5), Blocks.STONE.defaultBlockState());
			ModStructurePopulator.populateKonataBasement(level, origin, Rotation.NONE, data, key);
			test.assertTrue(!data.isStructurePopulated(key) && boats(level, box).isEmpty() && sheep(level, box).isEmpty(),
					"A blocked room must remain retryable and must not leave a lone boat or sheep");
			level.setBlockAndUpdate(at(origin, Rotation.NONE, 3, 1, 5), Blocks.CHERRY_SLAB.defaultBlockState()
					.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
			ModStructurePopulator.populateKonataBasement(level, origin, Rotation.NONE, data, key);
			test.assertTrue(data.isStructurePopulated(key) && boats(level, box).size() == 1 && sheep(level, box).size() == 1,
					"A later safe visit must successfully create the boat and passenger once");
		} finally {
			clear(level, box);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 240)
	public void konataSpawnsOnlyUpstairsAndLegacySevenBlockHousesStillWork(GameTestHelper test) {
		runInTickingChunks(test, 4, true, () -> checkKonataSpawnsUpstairsAndLegacyHousesWork(test));
	}

	private void checkKonataSpawnsUpstairsAndLegacyHousesWork(GameTestHelper test) {
		var level = test.getLevel();
		for (var rotation : Rotation.values()) {
			BlockPos origin = origin(test, 4, rotation);
			BoundingBox box = placeHouse(test, origin, rotation, false);
			try {
				test.assertTrue(ModStructurePopulator.spawnKonata(level, box), "Konata must still spawn in every rotated house");
				var residents = level.getEntitiesOfClass(KonataEntity.class, AABB.of(box), Entity::isAlive);
				test.assertTrue(residents.size() == 1 && residents.getFirst().getY() >= origin.getY() + 12
						&& residents.getFirst().getY() < origin.getY() + 15 && residents.getFirst().isPersistenceRequired(),
						"Konata must live upstairs, never in the newly added basement");
			} finally {
				clear(level, box);
			}
		}
		BlockPos legacyOrigin = origin(test, 4, Rotation.NONE).offset(120, 0, 0);
		BoundingBox legacyBox = BoundingBox.fromCorners(legacyOrigin, legacyOrigin.offset(8, 6, 8));
		try {
			for (var pos : BlockPos.betweenClosed(legacyOrigin, legacyOrigin.offset(8, 6, 8))) {
				level.setBlock(pos, pos.getY() == legacyOrigin.getY()
						? Blocks.CHERRY_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
			}
			test.assertTrue(ModStructurePopulator.spawnKonata(level, legacyBox),
					"Already-generated seven-block-tall houses must retain Konata's population support");
			var residents = level.getEntitiesOfClass(KonataEntity.class, AABB.of(legacyBox), Entity::isAlive);
			test.assertTrue(residents.size() == 1 && residents.getFirst().getY() >= legacyOrigin.getY() + 1
					&& residents.getFirst().getY() < legacyOrigin.getY() + 4,
					"Legacy houses must use their original floor rather than expect a nonexistent basement offset");
		} finally {
			clear(level, legacyBox);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 80)
	public void leverExtendsAndRetractsTheStickyPistonInEveryRotation(GameTestHelper test) {
		var level = test.getLevel();
		var boxes = new ArrayList<BoundingBox>();
		var forced = new ArrayList<BlockPos>();
		var player = test.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Runnable cleanup = () -> {
			level.getServer().getPlayerList().remove(player);
			boxes.forEach(box -> clear(level, box));
			forced.forEach(chunk -> level.setChunkForced(chunk.getX(), chunk.getZ(), false));
		};
		try {
			for (var rotation : Rotation.values()) {
				BlockPos origin = origin(test, 5, rotation);
				BoundingBox box = bounds(level, origin, rotation);
				for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
					for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
						if (level.setChunkForced(x, z, true)) forced.add(new BlockPos(x, 0, z));
					}
				}
				boxes.add(placeHouse(test, origin, rotation, false));
				assertMechanism(test, origin, rotation);
				var leverPos = at(origin, rotation, 7, 2, 5);
				test.assertTrue(level.getBlockState(leverPos).useWithoutItem(level, player,
						new BlockHitResult(Vec3.atCenterOf(leverPos), rotation.rotate(Direction.WEST), leverPos, false))
						.consumesAction(), "The unpowered wall lever must accept a real player interaction");
			}
			test.runAfterDelay(12, () -> {
				try {
					for (var rotation : Rotation.values()) {
						BlockPos origin = origin(test, 5, rotation);
						var piston = level.getBlockState(at(origin, rotation, 6, 2, 5));
						test.assertTrue(piston.is(Blocks.STICKY_PISTON) && piston.getValue(BlockStateProperties.EXTENDED)
								&& level.getBlockState(at(origin, rotation, 5, 2, 5)).is(Blocks.PISTON_HEAD)
								&& level.getBlockState(at(origin, rotation, 4, 2, 5)).is(Blocks.END_ROD),
								"Powering the lever must extend the correctly rotated piston and move its rod one block");
						var leverPos = at(origin, rotation, 7, 2, 5);
						level.getBlockState(leverPos).useWithoutItem(level, player,
								new BlockHitResult(Vec3.atCenterOf(leverPos), rotation.rotate(Direction.WEST), leverPos, false));
					}
					test.runAfterDelay(12, () -> {
						try {
							for (var rotation : Rotation.values()) {
								BlockPos origin = origin(test, 5, rotation);
								var piston = level.getBlockState(at(origin, rotation, 6, 2, 5));
								test.assertTrue(piston.is(Blocks.STICKY_PISTON) && !piston.getValue(BlockStateProperties.EXTENDED)
											&& level.getBlockState(at(origin, rotation, 5, 2, 5)).is(Blocks.END_ROD)
											&& level.getBlockState(at(origin, rotation, 4, 2, 5)).isAir(),
										"Switching off must retract the sticky piston and pull its rod back to the original cell");
							}
							test.succeed();
						} finally {
							cleanup.run();
						}
					});
				} catch (RuntimeException | Error error) {
					cleanup.run();
					throw error;
				}
			});
		} catch (RuntimeException | Error error) {
			cleanup.run();
			throw error;
		}
	}

	private static void assertMechanism(GameTestHelper test, BlockPos origin, Rotation rotation) {
		var level = test.getLevel();
		var piston = level.getBlockState(at(origin, rotation, 6, 2, 5));
		var rod = level.getBlockState(at(origin, rotation, 5, 2, 5));
		var lever = level.getBlockState(at(origin, rotation, 7, 2, 5));
		test.assertTrue(piston.is(Blocks.STICKY_PISTON) && !piston.getValue(BlockStateProperties.EXTENDED)
				&& piston.getValue(BlockStateProperties.FACING) == rotation.rotate(Direction.WEST)
				&& rod.is(Blocks.END_ROD) && rod.getValue(BlockStateProperties.FACING) == rotation.rotate(Direction.WEST)
				&& lever.is(Blocks.LEVER) && !lever.getValue(BlockStateProperties.POWERED)
				&& lever.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL
				&& lever.getValue(BlockStateProperties.HORIZONTAL_FACING) == rotation.rotate(Direction.EAST),
				"Piston, rod and initially unpowered east-wall lever must rotate together");
		for (int x = 3; x <= 4; x++) {
			var slab = level.getBlockState(at(origin, rotation, x, 1, 5));
			test.assertTrue(slab.is(Blocks.CHERRY_SLAB)
					&& slab.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM
					&& !slab.getValue(BlockStateProperties.WATERLOGGED),
					"Two dry bottom cherry slabs must support the boat along the template X axis");
		}
	}

	private static BoundingBox placeHouse(GameTestHelper test, BlockPos origin, Rotation rotation, boolean stone) {
		var level = test.getLevel();
		var box = bounds(level, origin, rotation);
		for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			level.setBlock(pos, stone ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
		}
		var pool = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, HbkMod.id("konata_house"))).value();
		var element = pool.getRandomTemplate(RandomSource.create(4));
		test.assertTrue(element.getType() == StructurePoolElementType.SINGLE,
				"Konata's pool must use a single element; the legacy element would ignore basement air");
		test.assertTrue(element.place(level.getStructureManager(), level, level.structureManager(),
				level.getChunkSource().getGenerator(), origin, origin, rotation, box,
				RandomSource.create(4), LiquidSettings.IGNORE_WATERLOGGING, false),
				"The real world-generation pool element must place the full eighteen-block-tall house");
		return box;
	}

	private static BoundingBox bounds(ServerLevel level, BlockPos origin, Rotation rotation) {
		var template = level.getStructureManager().get(HbkMod.id("konata_house")).orElseThrow();
		return template.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), origin);
	}

	private static BlockPos origin(GameTestHelper test, int section, Rotation rotation) {
		return test.absolutePos(new BlockPos(1200 + rotation.ordinal() * 24, 180, 1200 + section * 32));
	}

	private static BlockPos at(BlockPos origin, Rotation rotation, int x, int y, int z) {
		return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, rotation, BlockPos.ZERO).offset(origin);
	}

	private static float yaw(Rotation rotation) {
		return 90 + switch (rotation) {
			case NONE -> 0;
			case CLOCKWISE_90 -> 90;
			case CLOCKWISE_180 -> 180;
			case COUNTERCLOCKWISE_90 -> -90;
		};
	}

	private static List<Boat> boats(ServerLevel level, BoundingBox box) {
		return level.getEntitiesOfClass(Boat.class, AABB.of(box), entity -> entity.isAlive() && entity.getType() == EntityTypes.CHERRY_BOAT);
	}

	private static List<Sheep> sheep(ServerLevel level, BoundingBox box) {
		return level.getEntitiesOfClass(Sheep.class, AABB.of(box), Entity::isAlive);
	}

	/** FULL block chunks alone leave far-away entities in HIDDEN sections until ticking tickets settle. */
	private static void runInTickingChunks(GameTestHelper test, int section, boolean legacy, Runnable checks) {
		var level = test.getLevel();
		var required = new LinkedHashSet<ChunkPos>();
		var fixtures = new ArrayList<BoundingBox>();
		for (var rotation : Rotation.values()) {
			var box = bounds(level, origin(test, section, rotation), rotation);
			fixtures.add(box);
			addChunks(required, box);
		}
		if (legacy) {
			var first = origin(test, section, Rotation.NONE).offset(120, 0, 0);
			var box = BoundingBox.fromCorners(first, first.offset(8, 6, 8));
			fixtures.add(box);
			addChunks(required, box);
		}
		var owned = new ArrayList<ChunkPos>();
		for (var chunk : required) {
			if (level.setChunkForced(chunk.x(), chunk.z(), true)) owned.add(chunk);
		}
		Runnable release = () -> owned.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), false));
		awaitTickingChunks(test, List.copyOf(required), () -> {
			fixtures.forEach(box -> clear(level, box));
			checks.run();
		}, release, 0);
	}

	private static void addChunks(java.util.Set<ChunkPos> chunks, BoundingBox box) {
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
			for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
		}
	}

	private static void awaitTickingChunks(GameTestHelper test, List<ChunkPos> chunks,
			Runnable checks, Runnable release, int attempts) {
		test.runAfterDelay(1, () -> {
			if (chunks.stream().allMatch(test.getLevel()::areEntitiesActuallyLoadedAndTicking)) {
				try {
					checks.run();
				} finally {
					release.run();
				}
			} else if (attempts >= 200) {
				release.run();
				throw test.assertionException("Konata's test chunks must become entity-ticking before population checks");
			} else {
				awaitTickingChunks(test, chunks, checks, release, attempts + 1);
			}
		});
	}

	private static void clear(ServerLevel level, BoundingBox box) {
		level.getEntitiesOfClass(Entity.class, AABB.of(box).inflate(1), entity -> !(entity instanceof net.minecraft.world.entity.player.Player))
				.forEach(Entity::discard);
		for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
	}
}
