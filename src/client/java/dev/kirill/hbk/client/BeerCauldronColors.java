package dev.kirill.hbk.client;

import dev.kirill.hbk.block.BeerCauldronBlock;
import dev.kirill.hbk.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.Set;

/** Keeps the animated vanilla water surface and cauldron, changing only the brew's tint. */
public final class BeerCauldronColors {
	public static final int FIRST_WHEAT_COLOR = 0xFFDCE7AF;
	public static final int SECOND_WHEAT_COLOR = 0xFFE8DBA0;
	public static final int READY_COLOR = 0xFFD9A438;

	private BeerCauldronColors() {
	}

	public static void register() {
		BlockColorRegistry.register(List.of(new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return surfaceColor(state);
			}

			@Override
			public Set<Property<?>> relevantProperties() {
				// A wheat change must invalidate the renderer's cached color even at the same water level.
				return Set.of(LayeredCauldronBlock.LEVEL, BeerCauldronBlock.WHEAT);
			}
		}), ModBlocks.BEER_CAULDRON);
	}

	public static int surfaceColor(BlockState state) {
		int wheat = state.getValue(BeerCauldronBlock.WHEAT);
		if (wheat >= state.getValue(LayeredCauldronBlock.LEVEL)) return READY_COLOR;
		return wheat == 1 ? FIRST_WHEAT_COLOR : SECOND_WHEAT_COLOR;
	}
}
