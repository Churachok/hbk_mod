package dev.kirill.hbk.universe;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class AlloyLoot {
	private AlloyLoot() {}
	/** Transform native loot once: Fortune has already run; Silk Touch keeps its original block. */
	public static List<ItemStack> smelt(ServerLevel level, BlockState state, ItemInstance tool, List<ItemStack> drops) {
		if (tool == null || !tool.is(UniverseAlloys.item("smelting_pickaxe")) || !state.is(UniverseMining.ORES)) return drops;
		var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		if (EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0) return drops;
		List<ItemStack> result = new ArrayList<>();
		for (var drop : drops) {
			var input = new SingleRecipeInput(drop.copyWithCount(1));
			var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
			if (recipe.isEmpty()) { result.add(drop); continue; }
			var cooked = recipe.get().value().assemble(input); int count = cooked.getCount() * drop.getCount();
			while (count > 0) { int batch = Math.min(count, cooked.getMaxStackSize()); result.add(cooked.copyWithCount(batch)); count -= batch; }
		}
		return result;
	}
}
