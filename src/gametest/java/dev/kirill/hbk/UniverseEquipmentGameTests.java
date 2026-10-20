package dev.kirill.hbk;

import dev.kirill.hbk.block.FracturedBlock;
import dev.kirill.hbk.block.TimeFurnaceBlock;
import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

public final class UniverseEquipmentGameTests {
	@GameTest
	public void allNinetyEquipmentRecipesHaveVanillaStatisticsAndLocalRepairs(GameTestHelper test) {
		String[][] grids = {{"M  ", "M  ", "S  "}, {"MMM", " S ", " S "}, {"MM ", "MS ", " S "}, {"M  ", "S  ", "S  "},
				{"MM ", " S ", " S "}, {"MMM", "M M", "   "}, {"M M", "MMM", "MMM"}, {"MMM", "M M", "M M"}, {"M M", "M M", "   "}};
		for (int f = 0; f < 10; f++) for (int r = 0; r < UniverseEquipment.ROLES.size(); r++) {
			String role = UniverseEquipment.ROLES.get(r), family = UniverseMaterials.FAMILIES.get(f);
			List<ItemStack> slots = new ArrayList<>();
			for (String row : grids[r]) for (char c : row.toCharArray()) slots.add(c == 'M' ? new ItemStack(UniverseMaterials.resource(f, 0)) : c == 'S' ? new ItemStack(Items.STICK) : ItemStack.EMPTY);
			var input = CraftingInput.of(3, 3, slots);
			var recipe = test.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
			var output = recipe.value().assemble(input);
			test.assertTrue(output.is(UniverseEquipment.item(f, role)) && output.getCount() == 1, "Real recipe produces the corresponding mineral equipment");
			var vanilla = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("diamond_" + role)));
			test.assertTrue(output.getMaxDamage() == vanilla.getMaxDamage(), "Vanilla diamond durability");
			test.assertTrue(output.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(vanilla.get(DataComponents.ATTRIBUTE_MODIFIERS)), "Vanilla diamond attack/armor attributes");
			test.assertTrue(output.isEnchantable() && output.isValidRepairItem(new ItemStack(UniverseMaterials.resource(f, 0))), "Native enchanting and local repair work");
			test.assertFalse(output.isValidRepairItem(new ItemStack(UniverseMaterials.resource((f + 1) % 10, 0))), "A different family cannot repair the equipment");
			if (r < 5) test.assertTrue(output.getDestroySpeed(Blocks.STONE.defaultBlockState()) == vanilla.getDestroySpeed(Blocks.STONE.defaultBlockState()), "Vanilla mining speed");
			else test.assertTrue(output.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(HbkMod.id(family)), "Native armor rendering uses the matching family asset");
			test.assertTrue(getClass().getClassLoader().getResource("assets/hbk/items/" + family + "_" + role + ".json") != null, "Equipment has an inventory model");
		}
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void oreRestrictionsApplyToDropsWithoutAffectingOrdinaryBlocksOrOverworld(GameTestHelper test) {
		var level = UniverseManager.load(test.getLevel().getServer(), new UniverseCode("23456"));
		var pos = new BlockPos(1, 180, 1);
		List<ItemStack> allowed = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(ModItems.REDSTONE_PICKAXE)));
		for (int f = 0; f < 10; f++) allowed.add(new ItemStack(UniverseEquipment.item(f, "pickaxe")));
		var forbidden = List.of(ItemStack.EMPTY, new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.COPPER_PICKAXE),
				new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.GOLDEN_PICKAXE), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.DIAMOND_SWORD));
		List<net.minecraft.world.level.block.state.BlockState> ores = new ArrayList<>();
		for (int f = 0; f < 10; f++) ores.add(UniverseMaterials.state(f, 4, 0));
		for (var block : List.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.EMERALD_ORE,
				Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_DIAMOND_ORE)) ores.add(block.defaultBlockState());
		for (var state : ores) {
			for (var tool : allowed) test.assertFalse(Block.getDrops(state, level, pos, null, null, tool).isEmpty(), "Every diamond-tier pickaxe harvests ores in a parallel world");
			for (var tool : forbidden) test.assertTrue(Block.getDrops(state, level, pos, null, null, tool).isEmpty(), "Wrong tools and explosions cannot produce ore drops");
		}
		test.assertFalse(Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), test.getLevel(), test.absolutePos(BlockPos.ZERO), null, null, new ItemStack(Items.IRON_PICKAXE)).isEmpty(), "Normal Overworld iron mining remains usable");
		for (var state : List.of(UniverseMaterials.state(2, 0, 0), UniverseMaterials.state(2, 2, 0), UniverseMaterials.state(2, 3, 0), UniverseBotany.block(2, "log").defaultBlockState(), UniverseMaterials.loam(2).defaultBlockState()))
			test.assertTrue(UniverseMining.permits(level, state, new ItemStack(Items.IRON_PICKAXE)), "The extra tier applies only to ores");
		test.succeed();
	}
	@GameTest
	public void spatialShardsRequireTheRedstonePickaxeAndCannotBeCrafted(GameTestHelper test) {
		var player = test.makeMockPlayer(GameType.SURVIVAL); var pos = test.absolutePos(new BlockPos(3, 3, 3));
		try {
			for (var phase : FracturedBlock.Phase.values()) for (var tool : List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.NETHERITE_PICKAXE),
					new ItemStack(UniverseEquipment.item(7, "pickaxe")), new ItemStack(ModItems.REDSTONE_PICKAXE))) {
				var state = ModBlocks.FRACTURED_BLOCK.defaultBlockState().setValue(FracturedBlock.PHASE, phase);
				player.setItemInHand(InteractionHand.MAIN_HAND, tool);
				var drops = Block.getDrops(state, test.getLevel(), pos, null, player, tool);
				boolean valid = phase == FracturedBlock.Phase.MATERIAL && tool.is(ModItems.REDSTONE_PICKAXE);
				test.assertTrue(valid ? drops.size() == 1 && drops.getFirst().is(ModItems.SPATIAL_SHARD) && drops.getFirst().getCount() == 1 : drops.isEmpty(), "Only the redstone pickaxe in material phase gives exactly one spatial shard");
				test.assertTrue((state.getDestroyProgress(player, test.getLevel(), pos) > 0) == valid, "Other tools and non-material phases do not advance fractured mining");
			}
			for (String family : UniverseMaterials.FAMILIES) test.assertTrue(test.getLevel().getServer().getRecipeManager().byKey(
					ResourceKey.create(Registries.RECIPE, HbkMod.id(family + "_spatial_shard"))).isEmpty(), "Legacy recipes cannot bypass the pickaxe gate");
		} finally { player.discard(); }
		test.succeed();
	}
	@GameTest
	public void arrivalFurnaceIsCompleteReusedAndPreservesExistingContainers(GameTestHelper test) {
		var level = test.getLevel(); var arrival = test.absolutePos(new BlockPos(3, 4, 3));
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
			level.setBlock(arrival.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
			level.setBlock(arrival.offset(x, 0, z), Blocks.AIR.defaultBlockState(), 2); level.setBlock(arrival.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 2);
		}
		var chest = arrival.offset(-2, 0, -2); level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
		var station = UniverseArrival.ensureReturnFurnace(level, arrival);
		test.assertTrue(station != null && !station.equals(arrival) && station.distSqr(arrival) <= 16, "Return machine stands next to a clear spawn");
		test.assertTrue(level.getBlockState(station.above()).getValue(TimeFurnaceBlock.HALF) == DoubleBlockHalf.UPPER, "Both machine halves exist");
		var furnace = (TimeFurnaceBlockEntity) level.getBlockEntity(station); furnace.setDimensionCode("00011");
		test.assertTrue(UniverseArrival.ensureReturnFurnace(level, arrival).equals(station) && furnace.dimensionCode().equals("00011"), "Repeated arrival reuses the machine and preserves its address");
		test.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "Arrival never overwrites a container");
		test.succeed();
	}
}
