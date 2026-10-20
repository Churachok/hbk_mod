package dev.kirill.hbk;

import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;

public final class UniverseMinesGameTests {
	private static UniverseMines.Site find(UniverseMines mines) {
		for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
			var site = mines.site(x, z); if (site.isPresent()) return site.get();
		}
		throw new AssertionError("No mine within 640 blocks");
	}
	@GameTest(maxTicks = 200)
	public void everyFamilyHasConnectedSeededMinesWithItsOwnTimber(GameTestHelper test) {
		for (int f = 0; f < 10; f++) {
			var profile = UniverseProfile.of(new UniverseCode(f + "0011"));
			var generator = new UniverseChunkGenerator(new FixedBiomeSource(test.getLevel().getBiome(test.absolutePos(BlockPos.ZERO))), profile);
			var mines = generator.mines();
			HbkMod.LOGGER.info("Checking parallel mine family {}", f); var site = find(mines);
			test.assertTrue(mines.site(Math.floorDiv(site.entrance().getX(), 128), Math.floorDiv(site.entrance().getZ(), 128))
					.equals(new UniverseMines(profile).site(Math.floorDiv(site.entrance().getX(), 128), Math.floorDiv(site.entrance().getZ(), 128))), "Stable negative-coordinate plan");
			for (int u = -2; u <= 40; u++) {
				var floor = site.pos(u, site.floor(u) - site.entrance().getY() - 1, 0);
				var state = mines.material(List.of(site), floor.getX(), floor.getY(), floor.getZ());
				test.assertTrue(state != null && state.is(UniverseBotany.block(f, u > 0 && u < 12 ? "stairs" : "planks")), "Continuous local timber floor " + f);
				for (int h = 1; h <= 2; h++) {
					var head = floor.above(h + 1); var clear = mines.material(List.of(site), head.getX(), head.getY(), head.getZ());
					test.assertTrue(clear != null && clear.isAir(), "Main route has player headroom");
				}
			}
			for (int v = -9; v <= 9; v++) {
				var pos = site.pos(24, site.slope() * 12 + 1, v);
				test.assertTrue(mines.material(List.of(site), pos.getX(), pos.getY(), pos.getZ()).isAir(), "Cross passage is not blocked by support posts");
			}
			int exposed = 0;
			for (int v = -3; v <= 3; v++) for (int h = 0; h <= 3; h++) {
				var pos = site.pos(43, site.slope() * 12 + h, v); var state = generator.baseColumn(pos.getX(), pos.getZ()).getBlock(pos.getY());
				if (state != null && state.is(UniverseMaterials.block(f, 4))) exposed++;
			}
			test.assertTrue(exposed >= 5, "Working face exposes a worthwhile local ore vein in family " + f);
		}
		test.succeed();
	}
	@GameTest
	public void cavesExistEvenInOtherwiseMonolithicGeology(GameTestHelper test) {
		var profile = UniverseProfile.of(new UniverseCode("00000")); var terrain = new UniverseDensity(profile); var mines = new UniverseMines(profile);
		int buriedAir = 0, remainingRock = 0;
		for (int x = -64; x < 64; x += 4) for (int z = -64; z < 64; z += 4) for (int y = -40; y < 70; y += 4) {
			if (terrain.sample(x, y, z) < 8) continue;
			if (mines.caveDensity(x, y, z) < 0) buriedAir++; else remainingRock++;
		}
		test.assertTrue(buriedAir > 120 && remainingRock > buriedAir * 3, "Explorable cave network keeps most of the massif intact");
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void generatedMineContainersAndSpiderNestsSurviveChunkPromotion(GameTestHelper test) {
		var level = test.getLevel(); var profile = UniverseProfile.of(new UniverseCode("00011"));
		var generator = new UniverseChunkGenerator(new FixedBiomeSource(level.getBiome(test.absolutePos(BlockPos.ZERO))), profile);
		UniverseMines.Site site = null;
		for (int x = -3; x <= 3 && site == null; x++) for (int z = -3; z <= 3; z++) {
			var candidate = generator.mines().site(x, z); if (candidate.isPresent() && candidate.get().spiders()) { site = candidate.get(); break; }
		}
		test.assertTrue(site != null, "Some mines contain a dangerous side chamber");
		for (BlockPos pos : List.of(site.supplies(), site.cache(), site.nest())) {
			var chunk = new ProtoChunk(new ChunkPos(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16)), UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
			generator.fillFromNoise(Blender.empty(), level.getChunkSource().randomState(), level.structureManager(), chunk).join();
			if (!pos.equals(site.nest())) test.assertTrue(chunk.getBlockState(pos.above()).is(Blocks.CAVE_AIR), "Mine headroom keeps its cave-air marker in the actual chunk");
			generator.applyBiomeDecoration(level, chunk, level.structureManager());
			if (!pos.equals(site.nest())) test.assertTrue(chunk.getBlockState(pos.above()).is(Blocks.CAVE_AIR), "Ceiling vegetation must not fill mine headroom");
			var data = chunk.getBlockEntityNbt(pos);
			test.assertTrue(data != null, "Pending native block entity exists");
			var restored = BlockEntity.loadStatic(pos, chunk.getBlockState(pos), data, level.registryAccess());
			if (pos.equals(site.nest())) test.assertTrue(restored instanceof SpawnerBlockEntity, "Native spider spawner survives save/load");
			else {
				test.assertTrue(restored instanceof ChestBlockEntity chest && chest.getLootTable() != null
						&& chest.getLootTable().identifier().equals(HbkMod.id("chests/parallel_mine_primordial")), "Native chest retains its seeded loot table");
			}
			var column = generator.baseColumn(pos.getX(), pos.getZ());
			for (int y = -64; y < 320; y++) test.assertTrue(column.getBlock(y).equals(chunk.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())))
					|| chunk.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).is(net.minecraft.tags.BlockTags.LEAVES)
					|| column.getBlock(y).isAir(), "Mine structure agrees with pure column queries");
		}
		test.succeed();
	}
	@GameTest
	public void mineLootIsUsefulLocalAndDoesNotBypassAlloysOrSpatialShards(GameTestHelper test) {
		var level = test.getLevel();
		var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(test.absolutePos(BlockPos.ZERO))).create(LootContextParamSets.CHEST);
		for (int f = 0; f < 10; f++) {
			var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, HbkMod.id("chests/parallel_mine_" + UniverseMaterials.FAMILIES.get(f))));
			for (int seed = 1; seed <= 32; seed++) {
				var loot = table.getRandomItems(params, seed); final int family = f;
				test.assertTrue(loot.stream().anyMatch(s -> s.is(UniverseMaterials.resource(family, 0))) && loot.size() >= 3, "Local mineral and expedition supplies");
				for (ItemStack stack : loot) {
					test.assertFalse(stack.is(dev.kirill.hbk.registry.ModItems.SPATIAL_SHARD), "Spatial shards remain redstone-pickaxe-only");
					for (var alloy : UniverseAlloys.CATALOG) test.assertFalse(stack.is(UniverseAlloys.item(alloy.id() + "_ingot")), "Travel and alloy manufacture retain their purpose");
				}
			}
		}
		test.succeed();
	}
	@GameTest
	public void newAlloyArtHasNativeResolutionDistinctPalettesAndRealCutouts(GameTestHelper test) throws Exception {
		var resources = getClass().getClassLoader(); var hashes = new HashSet<Integer>();
		for (var alloy : UniverseAlloys.CATALOG) for (String role : List.of("sample", "ingot", "block")) {
			String path = "assets/hbk/textures/" + (role.equals("block") ? "block" : "item") + "/alloys/" + alloy.id() + "_" + role + ".png";
			try (var stream = resources.getResourceAsStream(path)) {
				test.assertTrue(stream != null, "Original alloy texture exists: " + path);
				var image = javax.imageio.ImageIO.read(stream); int transparent = 0, hash = 1; var colours = new HashSet<Integer>();
				test.assertTrue(image.getWidth() == 16 && image.getHeight() == 16, "Native vanilla pixel density");
				for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { int rgba = image.getRGB(x, y); hash = hash * 31 + rgba; colours.add(rgba); if (rgba >>> 24 == 0) transparent++; }
				test.assertTrue(colours.size() >= 4 && hashes.add(hash), "Distinct shaded alloy art: " + path + " colours=" + colours.size());
				test.assertTrue(role.equals("block") ? transparent == 0 : transparent >= 20, "Opaque block or transparent inventory cutout");
			}
		}
		test.succeed();
	}
}
