package dev.kirill.hbk.client;

import dev.kirill.hbk.block.JagermeisterCauldronBlock;
import dev.kirill.hbk.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.Set;

/** Vanilla animated water, with a herbal tint until the batch is ready. */
public final class JagermeisterCauldronColors {
	public static final int BREWING_COLOR = 0xFF889251;
	public static final int READY_COLOR = 0xFF6B321C;

	private JagermeisterCauldronColors() {
	}

	public static void register() {
		BlockColorRegistry.register(List.of(new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return surfaceColor(state);
			}

			@Override
			public Set<Property<?>> relevantProperties() {
				return Set.of(JagermeisterCauldronBlock.READY);
			}
		}), ModBlocks.JAGERMEISTER_CAULDRON);
	}

	public static int surfaceColor(BlockState state) {
		return state.getValue(JagermeisterCauldronBlock.READY) ? READY_COLOR : BREWING_COLOR;
	}
}
