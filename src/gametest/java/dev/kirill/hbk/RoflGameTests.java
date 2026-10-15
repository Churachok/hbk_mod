package dev.kirill.hbk;

import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.world.ModWorldgen;
import dev.kirill.hbk.world.RoflMushroomGeneration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RoflGameTests {
	@GameTest
	public void mushroomDropsExactlyOneBlockOrItselfWithShears(GameTestHelper test) {
		var pos = test.absolutePos(new BlockPos(2, 2, 2));
		var enchantments = test.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var fortune = new ItemStack(Items.DIAMOND_PICKAXE);
		fortune.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
		var silk = new ItemStack(Items.DIAMOND_PICKAXE);
		silk.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
		for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.SHEARS), fortune, silk)) {
			var drops = Block.getDrops(ModBlocks.ROFL_MUSHROOM.defaultBlockState(), test.getLevel(), pos, null, null, tool);
			var expected = tool.is(Items.SHEARS) ? ModItems.ROFL_MUSHROOM : ModItems.ROFL_BLOCK;
			test.assertTrue(drops.size() == 1 && drops.getFirst().is(expected) && drops.getFirst().getCount() == 1,
					"A mushroom must drop exactly one correct item with tool: " + tool);
		}
		var drops = Block.getDrops(ModBlocks.ROFL_BLOCK.defaultBlockState(), test.getLevel(), pos, null);
		test.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.ROFL_BLOCK) && drops.getFirst().getCount() == 1,
				"A mined trampoline must drop itself");
		test.succeed();
	}

	@GameTest
	public void safeFallSurvivesEntitySaveAndReload(GameTestHelper test) {
		test.setBlock(2, 1, 2, ModBlocks.ROFL_BLOCK);
		var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 2);
		cow.setPos(test.absoluteVec(new Vec3(2.5, 2, 2.5)));
		cow.setDeltaMovement(0, -0.2, 0);
		cow.move(MoverType.SELF, new Vec3(0, -0.2, 0));
		var level = test.getLevel();
		var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		cow.saveWithoutId(saved);
		cow.discard();
		var restored = EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		restored.load(net.minecraft.world.level.storage.TagValueInput.create(
				net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		test.assertTrue(Boolean.TRUE.equals(restored.getAttached(ModAttachments.ROFL_SAFE_FALL)),
				"Unloading/reloading a mid-flight entity must preserve its safe landing");
		test.succeed();
	}

	@GameTest
	public void mushroomGrowsOnSolidSurfacesAndIsGeneratedInPlainsAndForestsOnlyOutsideExcludedBiomes(GameTestHelper test) {
		var pos = test.absolutePos(new BlockPos(2, 2, 2));
		for (Block ground : List.of(Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.SNOW_BLOCK, Blocks.STONE)) {
			test.getLevel().setBlockAndUpdate(pos.below(), ground.defaultBlockState());
			test.assertTrue(ModBlocks.ROFL_MUSHROOM.defaultBlockState().canSurvive(test.getLevel(), pos),
					"Mushrooms must survive on solid surface: " + ground);
		}
		test.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
		test.assertFalse(ModBlocks.ROFL_MUSHROOM.defaultBlockState().canSurvive(test.getLevel(), pos),
				"Mushrooms must not float without support");
		var registry = test.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		for (var key : List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.DESERT, Biomes.SNOWY_PLAINS,
				ModWorldgen.RADIOACTIVE_WASTELAND, ModWorldgen.PAST_GORNOSLAVYANSK, Biomes.OCEAN, Biomes.RIVER, Biomes.NETHER_WASTES)) {
			boolean expected = !List.of(ModWorldgen.RADIOACTIVE_WASTELAND, ModWorldgen.PAST_GORNOSLAVYANSK,
					Biomes.NETHER_WASTES).contains(key);
			boolean present = registry.getOrThrow(key).value().getGenerationSettings().features().stream()
					.flatMap(set -> set.stream()).anyMatch(feature -> feature.is(RoflMushroomGeneration.PATCH));
			test.assertTrue(present == expected, "Unexpected mushroom generation in " + key.identifier());
		}
		test.succeed();
	}

	@GameTest
	public void trampolineLaunchesPlayersMobsAndItemsAndSneakingPreventsBounce(GameTestHelper test) {
		test.setBlock(2, 1, 2, ModBlocks.ROFL_BLOCK);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 2);
		var item = test.spawn(EntityTypes.ITEM, 2, 2, 2);
		item.setItem(new ItemStack(Items.DIAMOND));
		for (Entity entity : List.of(player, cow, item)) {
			entity.setPos(test.absoluteVec(new Vec3(2.5, 2, 2.5)));
			entity.setDeltaMovement(0.1, -0.2, 0);
			entity.move(MoverType.SELF, new Vec3(0, -0.2, 0));
			test.assertTrue(entity.getDeltaMovement().y > 2 && !entity.onGround(),
					"Standing on a trampoline must launch: " + entity.getType());
			test.assertTrue(Boolean.TRUE.equals(entity.getAttached(ModAttachments.ROFL_SAFE_FALL)),
					"Every launched entity must have safe next landing");
		}
		player.setShiftKeyDown(true);
		player.setPos(test.absoluteVec(new Vec3(2.5, 2, 2.5)));
		player.setDeltaMovement(0, -0.2, 0);
		player.move(MoverType.SELF, new Vec3(0, -0.2, 0));
		test.assertTrue(player.onGround() && player.getDeltaMovement().y <= 0, "Shift must suppress the bounce");
		float health = player.getHealth();
		ModBlocks.ROFL_BLOCK.fallOn(test.getLevel(), ModBlocks.ROFL_BLOCK.defaultBlockState(),
				test.absolutePos(new BlockPos(2, 1, 2)), player, 50);
		test.assertTrue(player.getHealth() == health, "Even sneaking must not take fall damage on rofl blocks");
		test.succeed();
	}

	@GameTest(maxTicks = 180)
	public void realMobFlightReachesTwentyToThirtyBlocksAndLandingAwayIsSafeOnlyOnce(GameTestHelper test) {
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
				for (int y = 2; y < 38; y++) test.setBlock(x, y, z, Blocks.AIR);
			}
		}
		test.setBlock(2, 1, 2, ModBlocks.ROFL_BLOCK);
		var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 5, 2);
		cow.setPos(test.absoluteVec(new Vec3(2.5, 5, 2.5)));
		double floorY = test.absolutePos(new BlockPos(2, 2, 2)).getY();
		double[] peak = {floorY};
		int[] phase = {0};
		test.onEachTick(() -> {
			peak[0] = Math.max(peak[0], cow.getY());
			if (phase[0] == 0 && cow.getDeltaMovement().y > 2) {
				phase[0] = 1;
				// Replace the launch block while airborne; the landing must still be safe.
				test.setBlock(2, 1, 2, Blocks.STONE);
			} else if (phase[0] == 1 && cow.onGround()) {
				test.assertTrue(peak[0] - floorY >= 20 && peak[0] - floorY <= 30,
						"Actual flight apex must be 20-30 blocks; got " + (peak[0] - floorY));
				test.assertTrue(cow.getHealth() == cow.getMaxHealth(), "Landing on stone after a rofl bounce must be safe");
				test.assertFalse(Boolean.TRUE.equals(cow.getAttached(ModAttachments.ROFL_SAFE_FALL)),
						"Protection must end at first landing");
				phase[0] = 2;
				cow.setPos(cow.getX(), floorY + 8, cow.getZ());
				cow.setOnGround(false);
				cow.setDeltaMovement(Vec3.ZERO);
			} else if (phase[0] == 2 && cow.onGround()) {
				test.assertTrue(cow.getHealth() < cow.getMaxHealth(), "A later ordinary fall must deal damage again");
				test.succeed();
			}
		});
	}
}
