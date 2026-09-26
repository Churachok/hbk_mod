package dev.kirill.hbk;

import dev.kirill.hbk.entity.ImprovedBoatEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public final class ImprovedBoatGameTests {
	@GameTest
	public void improvedBoatsKeepWoodAndRecipes(GameTestHelper test) {
		var level = test.getLevel();
		var types = List.of(
				ModEntityTypes.IMPROVED_OAK_BOAT,
				ModEntityTypes.IMPROVED_SPRUCE_BOAT,
				ModEntityTypes.IMPROVED_BIRCH_BOAT,
				ModEntityTypes.IMPROVED_JUNGLE_BOAT,
				ModEntityTypes.IMPROVED_ACACIA_BOAT,
				ModEntityTypes.IMPROVED_CHERRY_BOAT,
				ModEntityTypes.IMPROVED_DARK_OAK_BOAT,
				ModEntityTypes.IMPROVED_PALE_OAK_BOAT,
				ModEntityTypes.IMPROVED_MANGROVE_BOAT,
				ModEntityTypes.IMPROVED_BAMBOO_RAFT
		);
		var sourceItems = List.of(
				Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT,
				Items.ACACIA_BOAT, Items.CHERRY_BOAT, Items.DARK_OAK_BOAT, Items.PALE_OAK_BOAT,
				Items.MANGROVE_BOAT, Items.BAMBOO_RAFT
		);
		List<Item> improvedItems = List.of(
				ModItems.IMPROVED_OAK_BOAT, ModItems.IMPROVED_SPRUCE_BOAT,
				ModItems.IMPROVED_BIRCH_BOAT, ModItems.IMPROVED_JUNGLE_BOAT,
				ModItems.IMPROVED_ACACIA_BOAT, ModItems.IMPROVED_CHERRY_BOAT,
				ModItems.IMPROVED_DARK_OAK_BOAT, ModItems.IMPROVED_PALE_OAK_BOAT,
				ModItems.IMPROVED_MANGROVE_BOAT, ModItems.IMPROVED_BAMBOO_RAFT
		);

		for (int index = 0; index < types.size(); index++) {
			ImprovedBoatEntity boat = types.get(index).create(level, EntitySpawnReason.COMMAND);
			test.assertTrue(boat != null, "Improved boat entity must be constructible");
			test.assertTrue(boat.getPickResult().is(improvedItems.get(index)),
					"Improved boat must keep its wood variant when picked or dropped");

			CraftingInput input = CraftingInput.of(2, 1, List.of(
					new ItemStack(sourceItems.get(index)),
					new ItemStack(Items.BLAST_FURNACE)
			));
			var recipe = level.getServer().getRecipeManager()
					.getRecipeFor(RecipeType.CRAFTING, input, level)
					.orElseThrow();
			test.assertTrue(recipe.value().assemble(input).is(improvedItems.get(index)),
					"Each wood variant must craft into its matching improved boat");
		}

		test.assertTrue(ImprovedBoatEntity.SPEED_MULTIPLIER == 1.6,
				"Improved boats must be exactly 60 percent faster");
		test.succeed();
	}
}
