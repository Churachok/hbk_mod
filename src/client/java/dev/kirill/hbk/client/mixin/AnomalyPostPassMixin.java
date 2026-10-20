package dev.kirill.hbk.client.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import dev.kirill.hbk.client.AnomalyScreenEffects;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Post-pass JSON uniforms are immutable; bind a small dynamic buffer for our fade strengths. */
@Mixin(PostPass.class)
public abstract class AnomalyPostPassMixin {
	@Redirect(method = "lambda$addToFrame$1", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBuffer;)V"), require = 2)
	private void hbk$bindAnomalyFade(RenderPass pass, String name, GpuBuffer buffer) {
		pass.setUniform(name, name.equals("AnomalyScreenConfig") ? AnomalyScreenEffects.uniforms(buffer) : buffer);
	}
}
