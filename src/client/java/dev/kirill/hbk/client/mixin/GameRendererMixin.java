package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.GoshasRageClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow @Final private Minecraft minecraft;

	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"))
	private void hbk$deferRageEntityOutline(LevelRenderer levelRenderer) {
		if (!GoshasRageClient.isActive()) {
			levelRenderer.doEntityOutline();
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V", shift = At.Shift.BEFORE))
	private void hbk$drawRageEntityOutlineAfterPostEffect(DeltaTracker deltaTracker, boolean renderLevel,
			CallbackInfo ci) {
		if (renderLevel && GoshasRageClient.isActive()) {
			this.minecraft.levelRenderer.doEntityOutline();
		}
	}
}
