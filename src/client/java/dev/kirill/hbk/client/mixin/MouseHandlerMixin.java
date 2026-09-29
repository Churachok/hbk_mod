package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.UnknownEncounterClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void hbk$lockViewOnUnknown(double frameTime, CallbackInfo ci) {
		if (UnknownEncounterClient.isActive()) {
			ci.cancel();
		}
	}
}
