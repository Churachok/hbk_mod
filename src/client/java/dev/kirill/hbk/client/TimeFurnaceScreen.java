package dev.kirill.hbk.client;

import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import dev.kirill.hbk.menu.TimeFurnaceMenu;
import dev.kirill.hbk.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class TimeFurnaceScreen extends AbstractContainerScreen<TimeFurnaceMenu> {
	private EditBox codeInput;
	private Button saveButton;
	private Button travelButton;

	public TimeFurnaceScreen(TimeFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 276, 180);
	}

	@Override
	protected void init() {
		String draft = codeInput == null ? menu.initialCode() : codeInput.getValue();
		super.init();
		codeInput = addRenderableWidget(new EditBox(font, leftPos + 136, topPos + 54, 104, 24,
				Component.translatable("screen.hbk.time_furnace.code")) {
			@Override
			public void insertText(String text) {
				if (text.chars().allMatch(c -> c >= '0' && c <= '9')) super.insertText(text);
			}
		});
		codeInput.setMaxLength(5);
		codeInput.setHint(Component.literal("00000"));
		saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.hbk.time_furnace.save"),
				button -> save()).bounds(leftPos + 30, topPos + 116, 104, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
				.bounds(leftPos + 142, topPos + 116, 104, 20).build());
		travelButton = addRenderableWidget(Button.builder(Component.translatable("screen.hbk.time_furnace.travel"), button -> travel(false))
				.bounds(leftPos + 30, topPos + 145, 104, 20).build());
		Button home = addRenderableWidget(Button.builder(Component.translatable("screen.hbk.time_furnace.return"), button -> travel(true))
				.bounds(leftPos + 142, topPos + 145, 104, 20).build());
		home.active = minecraft.level != null && dev.kirill.hbk.universe.UniverseProfile.of(minecraft.level) != null;
		codeInput.setResponder(code -> { saveButton.active = TimeFurnaceBlockEntity.isValidCode(code); travelButton.active = saveButton.active; });
		codeInput.setValue(draft);
		saveButton.active = TimeFurnaceBlockEntity.isValidCode(draft);
		travelButton.active = saveButton.active;
		setInitialFocus(codeInput);
	}

	private void travel(boolean home) {
		if (!home && !TimeFurnaceBlockEntity.isValidCode(codeInput.getValue())) return;
		ClientPlayNetworking.send(new ModNetworking.TravelTimeFurnacePayload(menu.containerId, codeInput.getValue(), home));
	}

	private void save() {
		if (!TimeFurnaceBlockEntity.isValidCode(codeInput.getValue())) return;
		ClientPlayNetworking.send(new ModNetworking.SetTimeFurnaceCodePayload(menu.containerId, codeInput.getValue()));
		onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (codeInput.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
			save();
			return true;
		}
		if (!event.isEscape() && codeInput.isFocused() && event.key() != GLFW.GLFW_KEY_TAB) {
			codeInput.keyPressed(event);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF292929);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFB5B3AE);
		graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 28, 0xFF555451);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, 12, 12, 0xFFFFFFFF, false);
		graphics.text(font, Component.translatable("screen.hbk.time_furnace.choose"), 18, 36, 0xFF292929, false);
		graphics.text(font, "Xyi -", 84, 62, 0xFF292929, false);
	}
}
