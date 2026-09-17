package dev.kirill.hbk.block;

import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A small wasteland plant that survives only on the biome's poisoned ground. */
public final class WastelandPlantBlock extends BushBlock {
	public WastelandPlantBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
		return floor.is(ModBlocks.INFECTED_DIRT)
				|| floor.is(ModBlocks.ASH_SOIL)
				|| floor.is(ModBlocks.SEDIMENT_MUD)
				|| floor.is(ModBlocks.RADIOACTIVE_SAND);
	}
}
