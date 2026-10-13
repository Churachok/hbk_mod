package dev.kirill.hbk.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.client.FlowerSkyRenderer;
import dev.kirill.hbk.client.YoungLiberalClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Shadow @Final private RenderTarget renderTarget;
	@Unique private FlowerSkyRenderer hbk$flowers;

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void hbk$recolorLiberalSky(ClientLevel level, float partialTick, Camera camera,
			SkyRenderState state, CallbackInfo ci) {
		if (YoungLiberalClient.isActive() && state.skybox == DimensionType.Skybox.OVERWORLD) {
			state.skyColor = YoungLiberalClient.skyColor(state.sunAngle);
			state.starBrightness = YoungLiberalClient.isNight(state.sunAngle) ? 1.0f : 0.0f;
			state.rainBrightness = 1.0f;
			state.sunriseAndSunsetColor = 0;
			state.shouldRenderDarkDisc = false;
		}
	}

	@Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
	private void hbk$renderFlowersInsteadOfStars(float brightness, PoseStack poses, CallbackInfo ci) {
		if (YoungLiberalClient.isActive()) {
			if (this.hbk$flowers == null) this.hbk$flowers = new FlowerSkyRenderer();
			this.hbk$flowers.render(this.renderTarget, brightness, poses);
			ci.cancel();
		}
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void hbk$closeFlowerBuffers(CallbackInfo ci) {
		if (this.hbk$flowers != null) {
			this.hbk$flowers.close();
			this.hbk$flowers = null;
		}
	}
}
