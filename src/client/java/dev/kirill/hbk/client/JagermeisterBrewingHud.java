package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.block.JagermeisterCauldronBlock;
import dev.kirill.hbk.block.entity.JagermeisterCauldronBlockEntity;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/** An ingredient checklist for the actual cauldron under the crosshair, not a separate inventory. */
public final class JagermeisterBrewingHud {
	private static final int COLUMNS = 8;
	private static final int SLOT_SIZE = 18;
	private static final int WIDTH = COLUMNS * SLOT_SIZE + 12;
	private static final int HEIGHT = 102;

	private JagermeisterBrewingHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				HbkMod.id("jagermeister_brewing"), JagermeisterBrewingHud::render);
	}

	public static Optional<PanelState> currentPanel() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null || client.gui.screen() != null || client.gui.hud.isHidden()
				|| !(client.hitResult instanceof BlockHitResult hit)) return Optional.empty();
		var state = client.level.getBlockState(hit.getBlockPos());
		if (!state.is(ModBlocks.JAGERMEISTER_CAULDRON)
				|| !(client.level.getBlockEntity(hit.getBlockPos()) instanceof JagermeisterCauldronBlockEntity brew)
				|| brew.getIngredientsMask() == 0) return Optional.empty();
		return Optional.of(new PanelState(hit.getBlockPos().immutable(), brew.getIngredientsMask(),
				state.getValue(LayeredCauldronBlock.LEVEL), state.getValue(JagermeisterCauldronBlock.READY)));
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		currentPanel().ifPresent(panel -> renderPanel(graphics, panel));
	}

	private static void renderPanel(GuiGraphicsExtractor graphics, PanelState panel) {
		var font = Minecraft.getInstance().font;
		int left = Math.max(4, graphics.guiWidth() - WIDTH - 8);
		int top = Math.max(8, (graphics.guiHeight() - HEIGHT) / 2);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xDD141D17);
		graphics.outline(left, top, WIDTH, HEIGHT, 0xFF899565);
		graphics.text(font, Component.translatable("hud.hbk.jagermeister.title",
				JagermeisterIngredients.count(panel.ingredientsMask()), JagermeisterIngredients.items().size()),
				left + 6, top + 6, 0xFFF3DEAA, false);
		for (int i = 0; i < JagermeisterIngredients.items().size(); i++) {
			int x = left + 6 + i % COLUMNS * SLOT_SIZE;
			int y = top + 21 + i / COLUMNS * SLOT_SIZE;
			boolean present = (panel.ingredientsMask() & JagermeisterIngredients.ingredientBit(i)) != 0;
			graphics.fill(x, y, x + 17, y + 17, present ? 0xFF42653B : 0xFF333C34);
			graphics.item(new ItemStack(JagermeisterIngredients.items().get(i)), x, y);
			if (present) {
				graphics.text(font, "+", x + 11, y + 9, 0xFFB7FF84, true);
			} else {
				graphics.fill(x, y, x + 16, y + 16, 0x88313A33);
			}
		}
		if (panel.ready()) {
			graphics.text(font, Component.translatable("hud.hbk.jagermeister.ready"),
					left + 6, top + 79, 0xFFB7FF84, false);
			graphics.text(font, Component.translatable("hud.hbk.jagermeister.bottles", panel.bottles()),
					left + 6, top + 90, 0xFFA8B9B0, false);
		}
	}

	public record PanelState(BlockPos pos, int ingredientsMask, int bottles, boolean ready) {
	}
}
