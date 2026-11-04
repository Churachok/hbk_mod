package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.SkippingOwlScreenEffect;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The final owner-only card must also cover menus and survive the user's F1 setting. */
@Mixin(GameRenderer.class)
public abstract class SkippingOwlGuiMixin {
	@Shadow @Final private Minecraft minecraft;

	@Inject(method = "extract", at = @At("TAIL"))
	private void hbk$extractOwlCardAfterAllGui(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		SkippingOwlScreenEffect.extract(minecraft, ((GameRenderer) (Object) this).gameRenderState().guiRenderState, deltaTracker);
	}
}
