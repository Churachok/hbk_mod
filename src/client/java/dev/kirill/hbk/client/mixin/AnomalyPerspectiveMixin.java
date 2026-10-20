package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class AnomalyPerspectiveMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void hbk$warpPerspective(CameraRenderState state, float partialTick, CallbackInfo ci) {
		var client = Minecraft.getInstance();
		var player = client.player;
		if (player == null || client.level == null || !player.isAlive() || state.isPanoramicMode) return;
		var zone = AnomalyZones.at(client.level, player.getX(), player.getZ());
		if (zone == null) return;
		double scale = client.options.screenEffectScale().get();
		if (scale <= 0) return;
		double t = (client.level.getGameTime() % 72000 + partialTick) / 20.0;
		float strength = (float) ((0.03 + 0.10 * zone.intensity(player.getX(), player.getZ())) * scale);
		float squeeze = strength * (float) Math.sin(t * 0.73);
		var warp = new Matrix4f().scaling((float) Math.exp(squeeze), (float) Math.exp(-squeeze * 0.8), 1);
		warp.m10(strength * 0.35f * (float) Math.sin(t * 0.51));
		warp.m01(strength * 0.25f * (float) Math.cos(t * 0.43));
		// Distort the world's projection only: the HUD, aim direction and saved FOV are unaffected.
		state.projectionMatrix.mul(warp);
		if (!state.isFrustumCaptured) {
			// Rendering uses reverse depth; culling must use vanilla's forward-depth projection.
			// Feeding reverse depth to the camera-cube offset can loop forever on the render thread.
			var cullingProjection = ((CameraInvoker) this).hbk$createProjectionMatrixForCulling().mul(warp);
			var frustum = new Frustum(state.viewRotationMatrix, cullingProjection);
			frustum.prepare(state.pos.x, state.pos.y, state.pos.z);
			state.cullFrustum.set(frustum);
		}
	}
}
