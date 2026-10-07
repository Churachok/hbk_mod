package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.UnknownDeathClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void hbk$renderUnknownDeath(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo ci) {
		DeathScreen screen = (DeathScreen) (Object) this;
		UnknownDeathClient.render(graphics, screen.width, screen.height);
	}
}
