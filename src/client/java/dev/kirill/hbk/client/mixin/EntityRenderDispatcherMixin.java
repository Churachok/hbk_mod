package dev.kirill.hbk.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kirill.hbk.client.FunnySpinRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rotate the entire entity, including held items and armor; leave its shadow/hitbox alone. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "submit", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
	private void hbk$beginSpin(EntityRenderState state, CameraRenderState camera, double x, double y, double z,
			PoseStack poses, SubmitNodeCollector collector, CallbackInfo ci) {
		poses.pushPose();
		if (((FunnySpinRenderState) state).hbk$isFunnySpinning()) {
			float angle = state.ageInTicks * 72.0f;
			poses.translate(0, state.boundingBoxHeight * 0.5, 0);
			poses.mulPose(Axis.XP.rotationDegrees(angle));
			poses.mulPose(Axis.YP.rotationDegrees(angle * 1.3f));
			poses.mulPose(Axis.ZP.rotationDegrees(angle * 1.7f));
			poses.translate(0, -state.boundingBoxHeight * 0.5, 0);
		}
	}

	@Inject(method = "submit", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", shift = At.Shift.AFTER))
	private void hbk$endSpin(EntityRenderState state, CameraRenderState camera, double x, double y, double z,
			PoseStack poses, SubmitNodeCollector collector, CallbackInfo ci) {
		poses.popPose();
	}
}
