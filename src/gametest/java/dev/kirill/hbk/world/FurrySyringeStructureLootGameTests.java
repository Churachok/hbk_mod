package dev.kirill.hbk.world;

import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** The two structure inventories which are populated directly rather than by loot tables. */
public final class FurrySyringeStructureLootGameTests {
	@GameTest
	public void manualDungeonSuppliesHaveOneOptionalSyringeWithoutReplacingExistingItems(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos chestPos = test.absolutePos(new BlockPos(2, 2, 2));
		BlockPos barrelPos = test.absolutePos(new BlockPos(5, 2, 2));
		level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(barrelPos, Blocks.BARREL.defaultBlockState());
		var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
		var barrel = (BarrelBlockEntity) level.getBlockEntity(barrelPos);
		var chestBox = new BoundingBox(chestPos);
		var barrelBox = new BoundingBox(barrelPos);
		int dachaHits = 0;
		int gulagHits = 0;
		for (int seed = 0; seed < 512; seed++) {
			chest.clearContent();
			barrel.clearContent();
			chest.setItem(0, new ItemStack(Items.COBBLESTONE, 17));
			barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 17));
			level.getRandom().setSeed(seed);
			test.assertTrue(ModStructurePopulator.fillStalinDachaChests(level, chestBox),
					"Dacha supplies and both unique items must still fit alongside the optional syringe");
			int dachaCount = syringeCount(chest);
			level.getRandom().setSeed(seed);
			ModStructurePopulator.fillGulagBarrels(level, barrelBox);
			int gulagCount = syringeCount(barrel);
			test.assertTrue(dachaCount <= 1 && gulagCount <= 1,
					"Each manual container must roll once for exactly one syringe, not a stack or repeated pool");
			test.assertTrue(chest.getItem(0).is(Items.COBBLESTONE) && chest.getItem(0).getCount() == 17
					&& barrel.getItem(0).is(Items.COBBLESTONE) && barrel.getItem(0).getCount() == 17,
					"The new drop must use an empty slot without replacing existing container items");
			dachaHits += dachaCount;
			gulagHits += gulagCount;
		}
		// Fixed seeds exercise the real inventory-population paths, allowing a broad deterministic margin.
		test.assertTrue(dachaHits >= 40 && dachaHits <= 115 && gulagHits >= 40 && gulagHits <= 115,
				"Manual structure drops must remain moderate and optional at 15%: dacha=" + dachaHits + ", gulag=" + gulagHits);
		chest.clearContent();
		barrel.clearContent();
		test.succeed();
	}

	private static int syringeCount(Container container) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			var stack = container.getItem(slot);
			if (stack.is(ModItems.FURRY_SYRINGE)) count += stack.getCount();
		}
		return count;
	}
}
