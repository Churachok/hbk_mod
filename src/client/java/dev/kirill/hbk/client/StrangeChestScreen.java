package dev.kirill.hbk.client;

import dev.kirill.hbk.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class StrangeChestScreen extends ConfirmScreen {
	public StrangeChestScreen(BlockPos pos) {
		super(
				isThief -> {
					ClientPlayNetworking.send(new ModNetworking.AnswerStrangeChestPayload(pos, isThief));
					Minecraft.getInstance().setScreenAndShow(null);
				},
				Component.translatable("screen.hbk.strange_chest.title"),
				Component.translatable("screen.hbk.strange_chest.question"),
				Component.translatable("screen.hbk.strange_chest.yes"),
				Component.translatable("screen.hbk.strange_chest.no")
		);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape()) {
			return true;
		}
		return super.keyPressed(event);
	}
}
