package dev.kirill.hbk;

import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UniverseEcologyGameTests {
	@GameTest
	public void familySoilGradesThroughLocalLoamAndKeepsItsLoot(GameTestHelper test) throws Exception {
		for (int f = 0; f < 10; f++) {
			var loam = UniverseMaterials.loam(f);
			test.assertTrue(loam.defaultBlockState().is(BlockTags.SUPPORTS_VEGETATION), "Local loam supports renewable vegetation");
			var drops = Block.getDrops(loam.defaultBlockState(), test.getLevel(), test.absolutePos(BlockPos.ZERO), null, null, new ItemStack(Items.IRON_SHOVEL));
			test.assertTrue(drops.size() == 1 && drops.getFirst().is(loam.asItem()), "Each new soil can be collected and reused");
			var input = CraftingInput.of(2, 2, List.of(new ItemStack(UniverseBotany.fruit(f)), new ItemStack(UniverseMaterials.resource(f, 0)), new ItemStack(loam), ItemStack.EMPTY));
			var recipe = test.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
			test.assertTrue(recipe.value().assemble(input).is(UniverseBotany.block(f, "sapling").asItem()), "Saplings remain craftable using local soil without importing dirt");
			String name = UniverseMaterials.FAMILIES.get(f) + "_loam";
			test.assertTrue(getClass().getClassLoader().getResource("assets/hbk/blockstates/" + name + ".json") != null, "Loam has registered assets");
			if (f == 8) continue;
			var level = test.getLevel(); var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 96; y <= 100; y++) chunk.setBlockState(new BlockPos(x, y, z), UniverseMaterials.state(f, 0, 0));
			new UniverseEcology(UniverseProfile.of(new UniverseCode(f + "0011"))).buildSurface(chunk);
			test.assertTrue(chunk.getBlockState(new BlockPos(8, 100, 8)).is(UniverseBotany.block(f, "soil")), "Family turf caps the shelf");
			test.assertTrue(chunk.getBlockState(new BlockPos(8, 99, 8)).is(loam), "Subsoil shares the local palette");
			for (int y = 96; y <= 100; y++) test.assertFalse(chunk.getBlockState(new BlockPos(8, y, 8)).is(Blocks.DIRT), "No unrelated vanilla dirt stripe");
		}
		test.succeed();
	}
	@GameTest
	public void localHazardsAreSeededBoundedAndFadeAtTheirEdges(GameTestHelper test) {
		Set<UniverseHazards.Kind> kinds = new HashSet<>();
		for (int f = 0; f < 10; f++) {
			var p = UniverseProfile.of(new UniverseCode(f + "0011"));
			for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y < 5; y++) {
				var zone = UniverseHazards.cell(p, x, y, z); if (zone == null) continue; kinds.add(zone.kind());
				test.assertTrue(zone.equals(UniverseHazards.at(p, zone.center())), "Same field on both sides, including negative coordinates");
				var outside = zone.center().add(zone.radius() + .01, 0, 0);
				test.assertTrue(UniverseHazards.at(p, outside) == null && UniverseHazards.gravity(p, outside) == 1, "Fields do not leak across their boundary");
				test.assertTrue(UniverseEnvironment.hazardFogEnd(p, outside, 256) == 256, "Outside gas restores the surrounding fog");
				if (zone.kind().gas) {
					float core = UniverseEnvironment.hazardFogEnd(p, zone.center(), 256);
					float edge = UniverseEnvironment.hazardFogEnd(p, zone.center().add(zone.radius() * .9999, 0, 0), 256);
					test.assertTrue(core >= 17.9 && core <= 18.1 && edge > 250, "Gas fades smoothly and retains local visibility");
				}
			}
			for (var kind : List.of(UniverseHazards.gasKind(p), UniverseHazards.Kind.UPDRAFT, UniverseHazards.Kind.HEAVY_FIELD, UniverseHazards.Kind.RIFT))
				test.assertTrue(UniverseHazards.nearest(p, new Vec3(-1, 100, -1), kind) != null, "Every family contains its gas and all anomaly kinds");
		}
		test.assertTrue(kinds.size() == 6 && UniverseHazards.at(null, new Vec3(0, 100, 0)) == null, "Six kinds, with no fields outside parallel worlds");
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void gasRespectsGraceProtectionDistanceAndCreativeMode(GameTestHelper test) {
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var zone = new UniverseHazards.Zone(UniverseHazards.Kind.SPORE_GAS, Vec3.atCenterOf(test.absolutePos(new BlockPos(3, 3, 3))), 20, 12);
		try {
			player.setPos(zone.center());
			UniverseHazards.affect(player, zone, 59, 0);
			test.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Three seconds to recognise and leave the cloud");
			UniverseHazards.affect(player, zone, 60, 0);
			test.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Core exposure has a real gameplay effect");
			player.removeAllEffects(); player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 200));
			UniverseHazards.affect(player, zone, 80, 0);
			test.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Existing breathing rations counter gas");
			player.removeAllEffects(); player.setPos(zone.center().add(zone.radius() + 1, 0, 0));
			UniverseHazards.affect(player, zone, 80, 0);
			test.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Leaving the cloud stops new poisoning");
			var creative = test.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
			try { creative.setPos(zone.center()); UniverseHazards.affect(creative, zone, 80, 0);
				test.assertFalse(creative.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Creative explorers remain immune");
			} finally { creative.discard(); }
		} finally { player.discard(); }
		test.succeed();
	}
	@GameTest
	public void originalBotanyHasCompleteNativeResolutionAssets(GameTestHelper test) throws Exception {
		var resources = getClass().getClassLoader();
		Set<Integer> designs = new HashSet<>();
		for (String species : UniverseBotany.SPECIES) {
			for (String role : List.of("log", "planks", "leaves", "soil", "plant", "sapling", "fruit", "ration")) {
				try (var stream = resources.getResourceAsStream("assets/hbk/textures/block/botany/" + species + "_" + role + ".png")) {
					test.assertTrue(stream != null, "Original texture exists");
					var image = javax.imageio.ImageIO.read(stream); Set<Integer> colours = new HashSet<>(); int hash = 1, transparent = 0;
					test.assertTrue(image.getWidth() == 16 && image.getHeight() == 16, "Actual 16px game texture");
					for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { int pixel = image.getRGB(x, y); colours.add(pixel); hash = hash * 31 + pixel; if ((pixel >>> 24) == 0) transparent++; }
					test.assertTrue(colours.size() > 5 && designs.add(hash), "Original distinct artwork: " + species + "_" + role);
					if (List.of("leaves", "plant", "sapling", "fruit", "ration").contains(role)) test.assertTrue(transparent > 8, "Cutout sprite has true alpha: " + species + "_" + role);
				}
			}
			for (String role : UniverseBotany.ROLES) {
				String name = species + "_" + role;
				test.assertTrue(resources.getResource("assets/hbk/blockstates/" + name + ".json") != null && resources.getResource("assets/hbk/items/" + name + ".json") != null
						&& resources.getResource("data/hbk/loot_table/blocks/" + name + ".json") != null, "Complete registered block resources: " + name);
			}
		}
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void originalSaplingsRegrowTheirOwnTreesAndPreserveObstructions(GameTestHelper test) {
		var level = test.getLevel();
		// This test intentionally grows above the small native template; clean its own old canopy too.
		for (int x = -2; x <= 12; x++) for (int z = -2; z <= 12; z++) for (int y = 20; y <= 280; y++) {
			BlockPos old = test.absolutePos(new BlockPos(x, y, z));
			if (!level.getBlockState(old).isAir()) level.setBlock(old, Blocks.AIR.defaultBlockState(), 18);
		}
		for (int f = 0; f < 10; f++) {
			BlockPos pos = test.absolutePos(new BlockPos(5, 40 + f * 24, 5));
			var sapling = (UniverseSaplingBlock) UniverseBotany.block(f, "sapling");
			var state = sapling.defaultBlockState().setValue(UniverseSaplingBlock.HANGING, f == 8);
			level.setBlock(f == 8 ? pos.above() : pos.below(), UniverseBotany.block(f, "soil").defaultBlockState(), 2);
			level.setBlock(pos, state, 2);
			test.assertTrue(sapling.growTree(level, pos, state), "Regrow original family " + f);
			test.assertTrue(level.getBlockState(pos).is(UniverseBotany.block(f, "log")), "Sapling must produce its own wood");
		}
		BlockPos pos = test.absolutePos(new BlockPos(3, 22, 3));
		var sapling = (UniverseSaplingBlock) UniverseBotany.block(0, "sapling");
		level.setBlock(pos, sapling.defaultBlockState(), 2); level.setBlock(pos.above(2), Blocks.CHEST.defaultBlockState(), 2);
		test.assertFalse(sapling.growTree(level, pos, sapling.defaultBlockState()), "Tree growth must not overwrite a player's container");
		test.assertTrue(level.getBlockState(pos.above(2)).is(Blocks.CHEST), "Keep the obstruction intact");
		test.succeed();
	}
	@GameTest
	public void harvestsRegrowAndCraftedRationsApplyFamilyBenefits(GameTestHelper test) {
		var level = test.getLevel(); var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var pos = test.absolutePos(new BlockPos(3, 3, 3));
		try {
			for (int f = 0; f < 10; f++) {
				var crop = (UniverseCropBlock) UniverseBotany.block(f, "plant");
				level.setBlock(pos.below(), UniverseBotany.block(f, "soil").defaultBlockState(), 2);
				var state = crop.defaultBlockState().setValue(UniverseCropBlock.AGE, 3); level.setBlock(pos, state, 2);
				state.useWithoutItem(level, player, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
				test.assertTrue(level.getBlockState(pos).getValue(UniverseCropBlock.AGE) == 1, "Harvest leaves a living crop");
				final int family = f;
				test.assertTrue(test.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).stream().anyMatch(e -> e.getItem().is(UniverseBotany.fruit(family)) && e.getItem().getCount() >= 2), "Harvest yields family produce");
				for (var item : test.getEntities(net.minecraft.world.entity.EntityTypes.ITEM)) item.discard();
				crop.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos)); crop.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos));
				test.assertTrue(level.getBlockState(pos).getValue(UniverseCropBlock.AGE) == 3, "Renewable crops grow back");
				var input = CraftingInput.of(2, 2, List.of(new ItemStack(UniverseBotany.fruit(f)), new ItemStack(UniverseBotany.fruit(f)), new ItemStack(Items.WHEAT), new ItemStack(UniverseMaterials.resource(f, 0))));
				var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
				var ration = recipe.value().assemble(input); test.assertTrue(ration.is(UniverseBotany.ration(f)), "Family harvest and mineral craft a unique ration");
				player.removeAllEffects(); ration.finishUsingItem(level, player);
				for (var benefit : UniverseBotany.benefits(f)) test.assertTrue(player.hasEffect(benefit.getEffect()) && player.getEffect(benefit.getEffect()).getDuration() == benefit.getDuration(), "The crafted ration grants its promised effect");
			}
		} finally { player.discard(); }
		test.succeed();
	}
	private static UniverseChunkGenerator generator(GameTestHelper test, String code) {
		return new UniverseChunkGenerator(new FixedBiomeSource(test.getLevel().getBiome(test.absolutePos(BlockPos.ZERO))), UniverseProfile.of(new UniverseCode(code)));
	}
	private static ProtoChunk generate(GameTestHelper test, UniverseChunkGenerator generator, int x, int z) {
		var level = test.getLevel();
		var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
		generator.fillFromNoise(Blender.empty(), level.getChunkSource().randomState(), level.structureManager(), chunk).join();
		var ecology = new UniverseEcology(generator.profile()); ecology.buildSurface(chunk); ecology.decorate(chunk, generator);
		return chunk;
	}
	@GameTest
	public void allTimberHasOriginalIdsLootAndCraftsFourUsefulPlanks(GameTestHelper test) {
		var level = test.getLevel(); Set<Block> species = new HashSet<>(); Set<Long> shapes = new HashSet<>();
		for (int family = 0; family < 10; family++) {
			var ecology = new UniverseEcology(UniverseProfile.of(new UniverseCode(family + "0011")));
			var tree = ecology.tree(0, 140, 0, family == 8 ? -1 : 1, 1234);
			test.assertTrue(ecology.tree(0, 140, 0, -1, 1234).keySet().stream().allMatch(pos -> pos.getY() <= 140), "A ceiling sapling grows every part below its root");
			var habitat = ecology.habitat(); species.add(habitat.log());
			test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(habitat.log()).getNamespace().equals("hbk"), "Original family wood, not a renamed vanilla tree");
			var input = CraftingInput.of(1, 1, List.of(new ItemStack(habitat.log())));
			var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
			var planks = recipe.value().assemble(input);
			test.assertTrue(planks.is(net.minecraft.tags.ItemTags.PLANKS) && planks.getCount() == 4, "Each family supplies real planks for vanilla crafting");
			test.assertTrue(level.fuelValues().isFuel(new ItemStack(habitat.log())) && level.fuelValues().isFuel(planks), "Original timber works as ordinary furnace fuel");
			var sticks = CraftingInput.of(1, 2, List.of(planks.copyWithCount(1), planks.copyWithCount(1)));
			var stickRecipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, sticks, level).orElseThrow();
			test.assertTrue(stickRecipe.value().assemble(sticks).is(Items.STICK), "Family planks work in ordinary recipes");
			var drops = Block.getDrops(habitat.log().defaultBlockState(), level, test.absolutePos(BlockPos.ZERO), null, null, new ItemStack(Items.IRON_AXE));
			test.assertTrue(drops.size() == 1 && drops.getFirst().is(habitat.log().asItem()), "Logs are harvestable");
			var leafDrops = Block.getDrops(habitat.leaves().defaultBlockState(), level, test.absolutePos(BlockPos.ZERO), null, null, new ItemStack(Items.SHEARS));
			test.assertTrue(leafDrops.stream().anyMatch(stack -> stack.is(habitat.leaves().asItem())), "Shears preserve the family's own foliage");
			long shape = 1; int logs = 0, leaves = 0;
			for (var entry : tree.entrySet()) {
				if (entry.getValue().is(BlockTags.LOGS)) logs++;
				if (entry.getValue().getBlock() instanceof LeavesBlock) {
					leaves++;
				test.assertFalse(entry.getValue().getValue(LeavesBlock.PERSISTENT), "Natural leaves decay and drop their own saplings after harvesting");
					test.assertTrue(entry.getValue().getValue(LeavesBlock.DISTANCE) <= 6, "Natural crowns remain supported until harvested");
				}
				shape = shape * 31 + entry.getKey().asLong();
			}
			test.assertTrue(logs >= 4 && leaves >= 20, "Complete trunk and crown in every family");
			test.assertTrue(shapes.add(shape), "Ten distinct tree silhouettes");
		}
		test.assertTrue(species.size() == 10, "Ten separately registered original wood species");
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void everyGeometryActuallyGeneratesTreesPlantsAndUsefulOres(GameTestHelper test) {
		Set<String> substrates = new HashSet<>();
		BlockPos plantPos = test.absolutePos(new BlockPos(2, 3, 2));
		for (int family = 0; family < 10; family++) {
			var generator = generator(test, family + "0011"); int logs = 0, plants = 0, ores = 0, chunks = 0;
			long start = System.nanoTime();
			for (int cx = -3; cx <= 3 && (logs < 4 || plants < 4 || ores < 4); cx += 2)
				for (int cz = -3; cz <= 3 && (logs < 4 || plants < 4 || ores < 4); cz += 2) {
					var chunk = generate(test, generator, cx, cz); chunks++;
					var pos = new BlockPos.MutableBlockPos();
					for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = -64; y < 320; y++) {
						var state = chunk.getBlockState(pos.set(cx * 16 + x, y, cz * 16 + z));
						if (state.is(BlockTags.LOGS)) logs++;
						if (state.getBlock() instanceof VegetationBlock || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) plants++;
						if (state.getBlock() instanceof VegetationBlock && state.getFluidState().isEmpty()) {
							var substrate = chunk.getBlockState(pos.below());
							var roof = chunk.getBlockState(pos.above());
							if (substrates.add(state.toString() + substrate + roof)) {
								test.getLevel().setBlock(plantPos.below(), substrate, 18);
								for (int h = 12; h >= 0; h--) test.getLevel().setBlock(plantPos.above(h), chunk.getBlockState(pos.above(h)), 18);
								test.assertTrue(state.canSurvive(test.getLevel(), plantPos), "Plant support: " + state + " on " + substrate + ", roof=" + roof);
							}
						}
						if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)) ores++;
					}
				}
			HbkMod.LOGGER.info("Ecology {}: {} chunks, logs={}, plants={}, common ores={}, full generation mean={} ms", family, chunks, logs, plants, ores, (System.nanoTime() - start) / 1_000_000.0 / chunks);
			test.assertTrue(logs >= 4 && plants >= 4 && ores >= 4, "Populated geometry " + family + ": logs=" + logs + ", plants=" + plants + ", ores=" + ores);
		}
		test.succeed();
	}
	private static long fingerprint(ChunkAccess chunk) {
		long result = 1; var pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = -64; y < 320; y++)
			result = result * 31 + Block.getId(chunk.getBlockState(pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z)));
		return result;
	}
	@GameTest(maxTicks = 200)
	public void floodedIslandsSupplyHarvestableUnderwaterVegetation(GameTestHelper test) {
		var generator = generator(test, "13857"); int kelp = 0, light = 0;
		for (int cx = -1; cx <= 1 && (kelp == 0 || light == 0); cx++) for (int cz = -1; cz <= 1 && (kelp == 0 || light == 0); cz++) {
			var chunk = generate(test, generator, cx, cz); var pos = new BlockPos.MutableBlockPos();
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = -63; y < 213; y++) {
				var state = chunk.getBlockState(pos.set(cx * 16 + x, y, cz * 16 + z));
				if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) kelp++;
				if (state.is(Blocks.SEA_PICKLE)) light++;
			}
		}
		test.assertTrue(kelp > 0 && light > 0, "Flooded worlds contain kelp forests and underwater light sources");
		var drops = Block.getDrops(Blocks.KELP.defaultBlockState(), test.getLevel(), test.absolutePos(BlockPos.ZERO), null, null, ItemStack.EMPTY);
		test.assertTrue(drops.stream().anyMatch(stack -> stack.is(Items.KELP)), "Kelp can be harvested for drying, food and fuel");
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void decoratedChunksRemainIdenticalAcrossNegativeBordersAndLoadOrder(GameTestHelper test) {
		var a = generator(test, "00011"); var b = generator(test, "00011");
		var west = generate(test, a, -1, 0); var east = generate(test, a, 0, 0);
		var eastAgain = generate(test, b, 0, 0); var westAgain = generate(test, b, -1, 0);
		test.assertTrue(fingerprint(west) == fingerprint(westAgain) && fingerprint(east) == fingerprint(eastAgain), "Soil, trunks, crowns and plants do not depend on neighbour load order");
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void allAddressesHaveColourfulReadableAtmospheres(GameTestHelper test) {
		Set<Integer> colours = new HashSet<>();
		for (int address = 0; address < 100000; address++) {
			var profile = UniverseProfile.create(UniverseCode.of(address)); colours.add(UniverseEnvironment.palette(profile, false));
			for (long time : new long[]{0, 6000, 18000}) {
				int sky = UniverseEnvironment.skyColor(profile, time), fog = UniverseEnvironment.fogColor(profile, time);
				test.assertTrue(Math.max(sky >> 16 & 255, Math.max(sky >> 8 & 255, sky & 255)) >= 80, "Even night skies retain visible colour");
				test.assertTrue(Math.max(fog >> 16 & 255, Math.max(fog >> 8 & 255, fog & 255)) >= 110, "No black fog wall");
				test.assertTrue(UniverseEnvironment.illumination(profile, time) >= .18f, "Brightness combinations remain playable");
				test.assertTrue(UniverseEnvironment.fogEnd(profile, new Vec3(0, 100, 0), time) >= 64, "Geometry can be seen beyond a few blocks");
			}
		}
		test.assertTrue(colours.size() > 25000, "A continuous palette, rather than ten recurring colours: " + colours.size());
		test.succeed();
	}
}
