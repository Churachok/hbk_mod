package dev.kirill.hbk.universe;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Four baked textures per material, selected in worldgen without dynamic registration. */
public final class UniverseRockBlock extends Block {
	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
	public UniverseRockBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }
}
