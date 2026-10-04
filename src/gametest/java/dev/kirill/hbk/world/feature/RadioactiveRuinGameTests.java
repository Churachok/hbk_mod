package dev.kirill.hbk.world.feature;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class RadioactiveRuinGameTests {
	@GameTest
	public void ruinChestUsesLootTableAndContainsSupplies(GameTestHelper test) {
		BlockPos origin = test.absolutePos(new BlockPos(1, 2, 1));
		for (int x = 0; x < 7; x++) {
			for (int z = 0; z < 7; z++) {
				test.getLevel().setBlockAndUpdate(origin.offset(x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
		RadioactiveRuinFeature.placeLootChest(test.getLevel(), test.getLevel().getRandom(),
				origin.getX(), origin.getZ(), 7, 7, origin.getY());
		ChestBlockEntity chest = null;
		for (int x = 1; x < 6; x++) {
			for (int z = 1; z < 6; z++) {
				if (test.getLevel().getBlockEntity(origin.offset(x, 1, z)) instanceof ChestBlockEntity found) {
					chest = found;
				}
			}
		}
		test.assertTrue(chest != null, "A radioactive house must contain a chest");
		ChestBlockEntity lootChest = chest;
		test.assertTrue(lootChest.getLootTable() != null
				&& lootChest.getLootTable().identifier().equals(HbkMod.id("chests/radioactive_ruin")),
				"The chest must have radioactive ruin loot");
		boolean hasSupplies = false;
		for (int slot = 0; slot < lootChest.getContainerSize(); slot++) {
			var stack = lootChest.getItem(slot);
			hasSupplies |= stack.is(ModItems.BANDAGE) || stack.is(ModItems.RATION)
					|| stack.is(Items.REDSTONE) || stack.is(Items.EMERALD) || stack.is(Items.DIAMOND);
		}
		test.assertTrue(hasSupplies, "Radioactive ruin loot must generate common supplies");
		test.succeed();
	}
}
