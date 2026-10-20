package dev.kirill.hbk;

import dev.kirill.hbk.block.TimeFurnaceBlock;
import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import dev.kirill.hbk.menu.TimeFurnaceMenu;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TimeFurnaceGameTests {
	@GameTest
	public void recipeUsesExactGrid(GameTestHelper test) {
		var shard = ModItems.SPATIAL_SHARD;
		var input = CraftingInput.of(3, 3, List.of(new ItemStack(shard), new ItemStack(Items.SMOKER), new ItemStack(shard),
				new ItemStack(Items.REDSTONE), new ItemStack(Items.FURNACE), new ItemStack(Items.REDSTONE),
				new ItemStack(shard), new ItemStack(Items.BLAST_FURNACE), new ItemStack(shard)));
		var recipe = test.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
		var output = recipe.value().assemble(input);
		test.assertTrue(output.is(ModItems.TIME_FURNACE) && output.getCount() == 1, "Exact grid must craft one time furnace");
		test.succeed();
	}

	@GameTest
	public void placementNeedsRoomAndMiningEitherHalfNeverDuplicates(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		player.setPos(Vec3.atCenterOf(pos.north(2)));
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
		for (boolean chimney : List.of(false, true)) {
			var stack = new ItemStack(ModItems.TIME_FURNACE, 2);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
			ModItems.TIME_FURNACE.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
			test.assertTrue(level.getBlockState(pos).isAir() && stack.getCount() == 2, "Blocked chimney must prevent placement and item consumption");
			level.removeBlock(pos.above(), false);
			ModItems.TIME_FURNACE.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
			test.assertTrue(stack.getCount() == 1 && level.getBlockState(pos).is(ModBlocks.TIME_FURNACE)
					&& level.getBlockState(pos.above()).getValue(TimeFurnaceBlock.HALF) == DoubleBlockHalf.UPPER,
					"One item must place both body and chimney");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
			player.gameMode.destroyBlock(chimney ? pos.above() : pos);
			test.assertTrue(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir(), "Mining either half must remove both");
		}
		var drops = level.getEntities(EntityTypes.ITEM, entity -> entity.getItem().is(ModItems.TIME_FURNACE));
		test.assertTrue(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 2, "Two mined furnaces must drop exactly two items");
		for (var drop : drops) drop.discard();
		// Creative breaks of the chimney must not generate the body's loot.
		level.setBlockAndUpdate(pos, ModBlocks.TIME_FURNACE.defaultBlockState());
		ModBlocks.TIME_FURNACE.setPlacedBy(level, pos, level.getBlockState(pos), player, new ItemStack(ModItems.TIME_FURNACE));
		player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
		player.gameMode.destroyBlock(pos.above());
		test.assertTrue(level.getEntities(EntityTypes.ITEM, entity -> entity.getItem().is(ModItems.TIME_FURNACE)).isEmpty(), "Creative must not drop a furnace");
		level.getServer().getPlayerList().remove(player);
		test.succeed();
	}

	@GameTest
	public void destinationSurvivesReloadAndRejectsInvalidOrRemoteEdits(GameTestHelper test) {
		var level = test.getLevel();
		var pos = test.absolutePos(new BlockPos(2, 2, 2));
		level.setBlockAndUpdate(pos, ModBlocks.TIME_FURNACE.defaultBlockState());
		var player = test.makeMockServerPlayerInLevel();
		player.setPos(Vec3.atCenterOf(pos.north(2)));
		ModBlocks.TIME_FURNACE.setPlacedBy(level, pos, level.getBlockState(pos), player, new ItemStack(ModItems.TIME_FURNACE));
		var furnace = (TimeFurnaceBlockEntity) level.getBlockEntity(pos);
		var menu = new TimeFurnaceMenu(1, furnace);
		test.assertTrue(menu.saveCode(player, "00123"), "Five ASCII digits, including leading zeros, must save");
		for (String invalid : List.of("", "1234", "123456", "12a45", "-1234", "１２３４５")) {
			test.assertFalse(menu.saveCode(player, invalid), "Invalid code must not save: " + invalid);
		}
		var loaded = (TimeFurnaceBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos),
				furnace.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
		test.assertTrue(loaded != null && loaded.dimensionCode().equals("00123"), "World save must preserve the exact destination string");
		player.setPos(Vec3.atCenterOf(pos.offset(30, 0, 0)));
		test.assertFalse(menu.saveCode(player, "99999"), "Remote edits must fail");
		test.assertTrue(furnace.dimensionCode().equals("00123"), "Rejected edits must leave the selected dimension unchanged");
		player.setPos(Vec3.atCenterOf(pos.north(2)));
		level.removeBlock(pos, false);
		test.assertFalse(menu.stillValid(player), "Removing the furnace must invalidate its menu");
		level.getServer().getPlayerList().remove(player);
		test.succeed();
	}
}
