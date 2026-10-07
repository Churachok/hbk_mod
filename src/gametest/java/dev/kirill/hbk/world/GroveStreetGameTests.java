package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.world.structure.GroveStreetPiece;
import dev.kirill.hbk.world.structure.GroveStreetStructure;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;

public final class GroveStreetGameTests {
	@GameTest
	public void enteringStreetGrantsAchievementWithoutWeapons(GameTestHelper test) {
		var level = test.getLevel();
		var advancement = level.getServer().getAdvancements().get(HbkMod.id("grove_street_visit"));
		test.assertTrue(advancement != null, "Grove Street advancement must load");
		var display = advancement.value().display().orElseThrow();
		test.assertTrue(display.getTitle().getString().equals("Ah shit, here we go again"),
				"The title must be identical in every language");
		var player = test.makeMockServerPlayerInLevel();
		BlockPos origin = test.absolutePos(new BlockPos(2, 160, 2));
		var chunk = level.getChunkAt(origin);
		var savedStarts = new HashMap<>(chunk.getAllStarts());
		var savedReferences = new HashMap<>(chunk.getAllReferences());
		try {
			var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.getOrThrow(GroveStreetPopulation.STRUCTURE).value();
			var piece = new GroveStreetPiece(level.getStructureManager(), origin, 0);
			chunk.setStartForStructure(structure, new StructureStart(structure, chunk.getPos(), 0,
						new PiecesContainer(List.of(piece))));
			chunk.addReferenceForStructure(structure, chunk.getPos().pack());
			player.setPos(Vec3.atCenterOf(origin.offset(-1, 1, 0)));
			CriteriaTriggers.LOCATION.trigger(player);
			test.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(),
					"Standing outside the street must not grant its achievement");
			player.setPos(Vec3.atCenterOf(origin.above()));
			CriteriaTriggers.LOCATION.trigger(player);
			test.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
					"Entering the street must grant its achievement without a parent achievement");
		} finally {
			chunk.setAllStarts(savedStarts);
			chunk.setAllReferences(savedReferences);
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void overworldSitesRemainFrequentAfterTerrainFiltering(GameTestHelper test) {
		var level = test.getLevel();
		var registries = level.registryAccess();
		var structure = registries.lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(GroveStreetPopulation.STRUCTURE).value();
		var placement = (RandomSpreadStructurePlacement) registries.lookupOrThrow(Registries.STRUCTURE_SET)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, HbkMod.id("grove_streets"))).value().placement();
		var biomes = MultiNoiseBiomeSource.createFromPreset(registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
		var generator = new NoiseBasedChunkGenerator(biomes,
				registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));
		var height = LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth());
		int eligible = 0, placed = 0, savannaEligible = 0, savannaPlaced = 0;
		for (long seed : new long[] {42, 12345, 8675309}) {
			var random = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, seed);
			for (int rx = -8; rx < 8; rx++) for (int rz = -8; rz < 8; rz++) {
				var chunk = placement.getPotentialStructureChunk(seed, rx * placement.spacing() * 2, rz * placement.spacing() * 2);
				int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
				int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
				var biome = biomes.getNoiseBiome(x >> 2, y >> 2, z >> 2, random.sampler());
				if (!structure.biomes().contains(biome)) continue;
				eligible++;
				boolean savanna = biome.is(Biomes.SAVANNA);
				if (savanna) savannaEligible++;
				var context = new Structure.GenerationContext(registries, generator, biomes, random,
						level.getStructureManager(), seed, chunk, height, structure.biomes()::contains);
				if (structure.findValidGenerationPoint(context).isPresent()) {
					placed++;
					if (savanna) savannaPlaced++;
				}
			}
		}
		double typicalSpacing = placement.spacing() * 16.0 * Math.sqrt((double) eligible / Math.max(1, placed));
		HbkMod.LOGGER.info("Grove Street density: {} / {} suitable-biome candidates; area-equivalent spacing {} blocks",
				placed, eligible, Math.round(typicalSpacing));
		HbkMod.LOGGER.info("Grove Street biome samples: plains {} / {}, savanna {} / {}",
				placed - savannaPlaced, eligible - savannaEligible, savannaPlaced, savannaEligible);
		test.assertTrue(eligible >= 30, "Sample enough real plains and savanna sites");
		test.assertTrue(placed > 0 && typicalSpacing <= 1800,
				"Terrain filtering must not make streets much rarer than the 1500-block target: " + typicalSpacing);
		test.assertTrue(savannaPlaced > 0 && placed > savannaPlaced, "Both plains and savannas must actually generate streets");
		for (double successRate : new double[] {(double) savannaPlaced / Math.max(1, savannaEligible),
				(double) (placed - savannaPlaced) / Math.max(1, eligible - savannaEligible)}) {
			test.assertTrue(placement.spacing() * 16.0 / Math.sqrt(successRate) <= 2000,
					"Neither plains nor savanna may hide a much lower generation rate");
		}
		test.succeed();
	}

	@GameTest(structure = "hbk:grove_street_test", maxTicks = 200)
	public void fullNeighbourhoodPlacesAcrossChunksWithOneBonusChest(GameTestHelper test) {
		var level = test.getLevel();
		var template = level.getStructureManager().getOrCreate(HbkMod.id("grove_street"));
		test.assertTrue(template.getSize().equals(new net.minecraft.core.Vec3i(98, 40, 94)), "Preserve supplied template dimensions");
		BlockPos origin = test.absolutePos(new BlockPos(6, 6, 6));
		var piece = new GroveStreetPiece(level.getStructureManager(), origin, 7);
		var context = StructurePieceSerializationContext.fromLevel(level);
		var tag = piece.createTag(context);
		var restored = new GroveStreetPiece(level.getStructureManager(), tag);
		test.assertTrue(restored.createTag(context).getIntOr("BonusChest", -1) == 7,
				"Unique bonus chest choice must survive structure serialization");
		BoundingBox box = restored.getBoundingBox();
		for (ChunkPos chunk : box.intersectingChunks().toList()) {
			BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
					chunk.getMaxBlockX(), level.getMaxY() - 1, chunk.getMaxBlockZ());
			restored.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
					level.getRandom(), chunkBox, chunk, origin);
		}
		int bonus = 0, eggs = 0, bottles = 0;
		var chests = template.filterBlocks(origin, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), Blocks.CHEST);
		test.assertTrue(chests.size() == 12, "Grove Street must contain twelve room chests");
		for (var info : chests) {
			test.assertTrue(level.getBlockState(info.pos().above()).isAir(), "Room chest must be accessible");
			ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(info.pos());
			test.assertTrue(chest != null && chest.getLootTable() != null, "Every chest must receive a loot table");
			if (chest.getLootTable().identifier().equals(HbkMod.id("chests/grove_street_bonus"))) bonus++;
			chest.unpackLootTable(null);
			int localEggs = 0, localBottles = 0;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				var stack = chest.getItem(slot);
				if (stack.is(ModItems.CJ_SPAWN_EGG)) localEggs += stack.getCount();
				if (stack.is(ModItems.GOSHAS_RAGE_BOTTLE)) localBottles += stack.getCount();
			}
			if (localEggs > 0) test.assertTrue(localBottles == 2, "The egg and both bottles must share one chest");
			eggs += localEggs;
			bottles += localBottles;
		}
		test.assertTrue(bonus == 1 && eggs == 1 && bottles == 2, "Exactly one CJ egg and two rage bottles per neighbourhood");
		for (int x = box.minX(); x <= box.maxX(); x++) {
			for (int z = box.minZ(); z <= box.maxZ(); z++) {
				test.assertTrue(level.getBlockState(new BlockPos(x, origin.getY() - 1, z)).isSolid(),
						"Every street column must have solid support");
			}
		}
		test.succeed();
	}

	@GameTest
	public void lootBooksAlwaysUseMaximumEnchantmentLevels(GameTestHelper test) {
		var level = test.getLevel();
		var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, HbkMod.id("chests/grove_street")));
		var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(test.absolutePos(new BlockPos(2, 2, 2)))).create(LootContextParamSets.CHEST);
		for (int seed = 0; seed < 100; seed++) {
			var loot = table.getRandomItems(params, seed);
			int books = 0;
			for (var stack : loot) {
				if (!stack.is(Items.ENCHANTED_BOOK)) continue;
				books++;
				var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
				test.assertTrue(enchantments != null && !enchantments.entrySet().isEmpty(), "A loot book must actually be enchanted");
				for (var entry : enchantments.entrySet()) test.assertTrue(entry.getIntValue() == entry.getKey().value().getMaxLevel(), "Loot books must carry maximum-level enchantments");
			}
			test.assertTrue(books == 1, "Each room chest must include one max-level enchanted book");
		}
		test.succeed();
	}

	@GameTest
	public void residentsRespawnInFoursWithoutExceedingTwelve(GameTestHelper test) {
		for (int x = 1; x <= 8; x++) for (int z = 1; z <= 8; z++) test.setBlock(x, 1, z, Blocks.ANDESITE);
		BoundingBox box = BoundingBox.fromCorners(test.absolutePos(new BlockPos(1, 1, 1)), test.absolutePos(new BlockPos(8, 5, 8)));
		for (int i = 0; i < 3; i++) test.assertTrue(GroveStreetPopulation.spawnGroup(test.getLevel(), box) == 4, "Each arrival must be four fellas");
		test.assertTrue(test.getEntities(ModEntityTypes.FELLAS).size() == 12, "Three groups must total twelve");
		test.assertTrue(GroveStreetPopulation.spawnGroup(test.getLevel(), box) == 0, "A fourth group must not spawn");
		var residents = test.getEntities(ModEntityTypes.FELLAS);
		residents.get(0).discard();
		test.assertTrue(GroveStreetPopulation.spawnGroup(test.getLevel(), box) == 0, "Eleven residents leave insufficient space for another group");
		for (int i = 1; i < 4; i++) residents.get(i).discard();
		test.assertTrue(GroveStreetPopulation.spawnGroup(test.getLevel(), box) == 4, "Residents must replenish without reloading chunks");
		test.assertTrue(test.getEntities(ModEntityTypes.FELLAS).size() == 12, "Replenishment must keep the twelve-resident cap");
		test.succeed();
	}

	@GameTest
	public void chestOpeningOnlyAngersVisibleWitnesses(GameTestHelper test) {
		var player = new net.minecraft.server.level.ServerPlayer(test.getLevel().getServer(), test.getLevel(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "grove-survival-test"),
				net.minecraft.server.level.ClientInformation.createDefault()) {
			@Override
			public net.minecraft.world.level.GameType gameMode() { return net.minecraft.world.level.GameType.SURVIVAL; }
		};
		player.getAbilities().invulnerable = false;
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		player.snapTo(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5, 0, 0);
		test.setBlock(2, 2, 3, Blocks.CHEST);
		var witness = test.spawn(ModEntityTypes.FELLAS, 3, 2, 2);
		var hidden = test.spawn(ModEntityTypes.FELLAS, 7, 2, 2);
		witness.setNoAi(true);
		hidden.setNoAi(true);
		for (int y = 1; y <= 5; y++) for (int z = 0; z <= 9; z++) test.setBlock(5, y, z, Blocks.STONE);
		test.assertTrue(witness.getTarget() == null && witness.getMainHandItem().isEmpty(), "Residents must start neutral and unarmed");
		test.assertTrue(witness.hasLineOfSight(player) && !hidden.hasLineOfSight(player), "Test wall must obscure one witness");
		ChestBlockEntity chest = (ChestBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(2, 2, 3)));
		chest.startOpen(player);
		test.assertTrue(witness.getTarget() == player && witness.getMainHandItem().is(Items.CROSSBOW), "Opening a real chest must arm its witness");
		test.assertTrue(witness.getMainHandItem().isEnchanted(), "A provoked fella must carry an enchanted crossbow");
		test.assertTrue(hidden.getTarget() == null, "Residents behind walls must remain neutral");
		test.succeed();
	}

	@GameTest(maxTicks = 200)
	public void provokedFellaActuallyShootsCrossbow(GameTestHelper test) {
		for (int x = 1; x <= 8; x++) for (int z = 1; z <= 8; z++) test.setBlock(x, 1, z, Blocks.STONE);
		var fella = test.spawn(ModEntityTypes.FELLAS, 2, 2, 2);
		fella.getRandom().setSeed(42L);
		var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 7, 2, 2);
		float health = cow.getHealth();
		fella.hurtServer(test.getLevel(), test.getLevel().damageSources().mobAttack(cow), 1);
		test.assertTrue(fella.getTarget() == cow, "Attacking a neutral fella must provoke retaliation");
		test.succeedWhen(() -> test.assertTrue(cow.getHealth() < health, "The charged enchanted crossbow must fire a damaging projectile"));
	}

	@GameTest
	public void naturalNpcCategoryAndPersistenceAllowNewSpawns(GameTestHelper test) {
		for (var type : java.util.List.of(ModEntityTypes.ANTON, ModEntityTypes.GOSHA, ModEntityTypes.GRISHA, ModEntityTypes.SASHA, ModEntityTypes.PINK_FURRY_WOLF)) {
			test.assertTrue(type.getCategory() == MobCategory.MONSTER, "Roaming NPCs must use the continuous monster spawn pool");
			var npc = type.create(test.getLevel(), EntitySpawnReason.NATURAL);
			npc.finalizeSpawn(test.getLevel(), test.getLevel().getCurrentDifficultyAt(npc.blockPosition()), EntitySpawnReason.NATURAL, null);
			test.assertTrue(!npc.isPersistenceRequired() && npc.removeWhenFarAway(10000), "Natural NPCs must release the mob cap when distant");
		}
		var resident = ModEntityTypes.GRISHA.create(test.getLevel(), EntitySpawnReason.STRUCTURE);
		resident.finalizeSpawn(test.getLevel(), test.getLevel().getCurrentDifficultyAt(resident.blockPosition()), EntitySpawnReason.STRUCTURE, null);
		test.assertTrue(resident.isPersistenceRequired(), "Structure residents must remain persistent");
		test.succeed();
	}

	@GameTest(skyAccess = true)
	public void exploredChunkCanKeepSpawningAtNight(GameTestHelper test) {
		var level = test.getLevel();
		test.setBlock(2, 1, 2, Blocks.GRASS_BLOCK);
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		ModWorldData.get(level).markNpcChunkHandled(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4).pack());
		long oldTime = level.getOverworldClockTime();
		try {
			test.setTime(6000);
			level.updateSkyBrightness();
			for (var type : java.util.List.of(ModEntityTypes.ANTON, ModEntityTypes.GRISHA, ModEntityTypes.SASHA, ModEntityTypes.PINK_FURRY_WOLF)) {
				test.assertFalse(SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos,
						net.minecraft.util.RandomSource.create(42)), "Daylight must prevent nocturnal NPC spawning");
			}
			test.setTime(18000);
			level.updateSkyBrightness();
			for (var type : java.util.List.of(ModEntityTypes.ANTON, ModEntityTypes.GRISHA, ModEntityTypes.SASHA, ModEntityTypes.PINK_FURRY_WOLF)) {
				for (int visit = 0; visit < 3; visit++) {
					boolean allowed = false;
					for (int seed = 0; seed < 32; seed++) allowed |= SpawnPlacements.checkSpawnRules(type, level,
							EntitySpawnReason.NATURAL, pos, net.minecraft.util.RandomSource.create(seed));
					test.assertTrue(allowed, "An already handled chunk must remain eligible for repeated night spawns: " + type);
				}
			}
		} finally {
			test.setTime(oldTime);
			level.updateSkyBrightness();
		}
		test.succeed();
	}

	@GameTest
	public void terrainFitRejectsMountainsAndFillsOnlyCurrentChunk(GameTestHelper test) {
		test.assertTrue(GroveStreetStructure.foundationHeight(new int[] {70, 71, 72, 73, 74}) == 71, "Use the median surface for a gently sloping site");
		test.assertTrue(GroveStreetStructure.foundationHeight(new int[] {64, 65, 90}) == Integer.MIN_VALUE, "Reject unsuitable steep sites");
		BlockPos low = test.absolutePos(new BlockPos(2, 1, 2));
		BlockPos high = test.absolutePos(new BlockPos(4, 5, 4));
		BoundingBox area = BoundingBox.fromCorners(low, high);
		for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
			test.setBlock(x, 1, z, Blocks.STONE);
			for (int y = 2; y <= 4; y++) test.setBlock(x, y, z, Blocks.AIR);
		}
		BoundingBox oneColumn = new BoundingBox(low.getX(), low.getY(), low.getZ(), low.getX(), high.getY(), low.getZ());
		GroveStreetPiece.fillFoundation(test.getLevel(), area, oneColumn, high.getY());
		test.assertTrue(test.getBlockState(new BlockPos(2, 4, 2)).isSolid(), "Fill the air below the neighbourhood");
		test.assertTrue(test.getBlockState(new BlockPos(3, 4, 2)).isAir(), "Do not write outside the current chunk bounds");
		test.succeed();
	}
}
