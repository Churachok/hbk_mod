package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.GoshasRageClient;
import dev.kirill.hbk.client.KirillGlassesClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@ModifyArg(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"), index = 0)
	private Identifier hbk$useRageEntityOutline(Identifier original) {
		if (GoshasRageClient.isActive()) {
			return GoshasRageClient.ENTITY_MASK_EFFECT;
		}
		return KirillGlassesClient.isActive() ? KirillGlassesClient.ENTITY_MASK_EFFECT : original;
	}
}
