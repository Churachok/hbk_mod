package dev.kirill.hbk.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.client.ProgenitorClientState;
import dev.kirill.hbk.client.ProgenitorModel;
import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Unique
	private boolean hbk$renderingProgenitor;

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
	private void hbk$beginProgenitorRender(LivingEntityRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
		this.hbk$renderingProgenitor = state instanceof AvatarRenderState avatar
				&& ProgenitorClientState.isActive(avatar);
	}

	@ModifyArg(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
			index = 0)
	private Model<?> hbk$replaceHumanModel(Model<?> original) {
		return this.hbk$renderingProgenitor ? ProgenitorModel.INSTANCE : original;
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"))
	private void hbk$endProgenitorRender(LivingEntityRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
		this.hbk$renderingProgenitor = false;
	}

	@Inject(method = "getBoundingBoxForCulling(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
	private void hbk$includeLongBodyInCulling(LivingEntity entity, CallbackInfoReturnable<AABB> cir) {
		if (entity instanceof Player player && ProgenitorTransformation.isActive(player)) {
			cir.setReturnValue(cir.getReturnValue().inflate(45.0, 2.0, 45.0));
		}
	}
}
