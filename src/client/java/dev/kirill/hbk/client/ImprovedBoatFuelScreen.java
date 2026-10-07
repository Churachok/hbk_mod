package dev.kirill.hbk.client;

import dev.kirill.hbk.menu.ImprovedBoatFuelMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The vanilla furnace panel, with its smelting controls replaced by one fuel slot. */
public final class ImprovedBoatFuelScreen extends AbstractContainerScreen<ImprovedBoatFuelMenu> {
	private static final Identifier FURNACE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	public ImprovedBoatFuelScreen(ImprovedBoatFuelMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = this.leftPos;
		int y = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, x, y, 0, 0, 176, 166, 256, 256);
		// Erase the input, result and arrow from the vanilla furnace's top half.
		graphics.fill(x + 42, y + 14, x + 146, y + 73, 0xFFC6C6C6);
		graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
				x + 79, y + 34, 54, 51, 18, 18, 256, 256);
		int bar = this.menu.burnPercent() * 40 / 100;
		graphics.fill(x + 109, y + 45, x + 151, y + 51, 0xFF555555);
		if (bar > 0) graphics.fill(x + 110, y + 46, x + 110 + bar, y + 50, 0xFFFF9A24);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, this.title, 8, 7, 0xFF404040, false);
		graphics.text(this.font, Component.translatable("screen.hbk.improved_boat.fuel"),
				103, 34, 0xFF404040, false);
		graphics.text(this.font, Component.translatable("screen.hbk.improved_boat.remaining",
				this.menu.remainingSeconds()), 8, 60, 0xFF404040, false);
		graphics.text(this.font, this.playerInventoryTitle, 8, 72, 0xFF404040, false);
	}
}
