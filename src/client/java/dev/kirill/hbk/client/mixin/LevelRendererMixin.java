package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.GoshasRageClient;
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
		return GoshasRageClient.isActive() ? GoshasRageClient.ENTITY_MASK_EFFECT : original;
	}
}
