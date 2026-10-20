package dev.kirill.hbk.universe;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.block.FracturedBlock;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** World-specific ore gate; ordinary rock, flora and Overworld ores keep vanilla rules. */
public final class UniverseMining {
	public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, HbkMod.id("parallel_ores"));
	public static final TagKey<Item> ORE_PICKAXES = TagKey.create(Registries.ITEM, HbkMod.id("parallel_ore_pickaxes"));
	private UniverseMining() {}
	public static boolean permits(Level level, BlockState state, ItemInstance tool) {
		if (state.getBlock() instanceof FracturedBlock) return tool.is(ModItems.REDSTONE_PICKAXE);
		if (state.is(ORES) && (state.getBlock() instanceof UniverseRockBlock || UniverseProfile.of(level) != null)) return tool.is(ORE_PICKAXES);
		return true;
	}
}
