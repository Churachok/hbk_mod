package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.GoshasRageClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
	private void hbk$markRageOutline(T entity, S state, float tickProgress, CallbackInfo ci) {
		if (GoshasRageClient.isActive() && entity instanceof LivingEntity) {
			state.outlineColor = hbk$encodeProjectedVerticalBounds(state);
		}
	}

	/**
	 * Stores the projected bottom and top of this entity in the red and green
	 * channels of the outline mask. The post shader uses those bounds to give
	 * every glowing outline its own three horizontal flag stripes without
	 * covering the entity's texture.
	 */
	private static int hbk$encodeProjectedVerticalBounds(EntityRenderState state) {
		var camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
		if (!camera.initialized || camera.pos == null || camera.viewRotationMatrix == null
				|| camera.projectionMatrix == null) {
			return 0xFF00FFFF;
		}

		float halfWidth = state.boundingBoxWidth * 0.5f;
		float minScreenY = Float.POSITIVE_INFINITY;
		float maxScreenY = Float.NEGATIVE_INFINITY;
		for (int xSign = -1; xSign <= 1; xSign += 2) {
			for (int ySide = 0; ySide <= 1; ySide++) {
				for (int zSign = -1; zSign <= 1; zSign += 2) {
					Vector4f clip = new Vector4f(
							(float) (state.x - camera.pos.x) + xSign * halfWidth,
							(float) (state.y - camera.pos.y) + ySide * state.boundingBoxHeight,
							(float) (state.z - camera.pos.z) + zSign * halfWidth,
							1.0f);
					camera.viewRotationMatrix.transform(clip);
					camera.projectionMatrix.transform(clip);
					if (clip.w <= 0.001f) {
						continue;
					}
					float screenY = clip.y / clip.w * 0.5f + 0.5f;
					minScreenY = Math.min(minScreenY, screenY);
					maxScreenY = Math.max(maxScreenY, screenY);
				}
			}
		}

		if (!Float.isFinite(minScreenY) || !Float.isFinite(maxScreenY)) {
			return 0xFF00FFFF;
		}
		int bottom = Math.clamp(Math.round(minScreenY * 255.0f), 0, 255);
		int top = Math.clamp(Math.round(maxScreenY * 255.0f), 0, 255);
		if (top <= bottom) {
			top = Math.min(bottom + 1, 255);
			bottom = Math.max(top - 1, 0);
		}
		return 0xFF0000FF | bottom << 16 | top << 8;
	}
}
