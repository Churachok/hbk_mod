package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.FlyingCarpetPassengerRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	@Shadow @Final public ModelPart leftLeg;
	@Shadow @Final public ModelPart rightLeg;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
	private void hbk$restLegsOnFlyingCarpet(HumanoidRenderState state, CallbackInfo ci) {
		if (!((FlyingCarpetPassengerRenderState) state).hbk$isOnFlyingCarpet()) {
			return;
		}
		// The normal riding pose angles the feet downward through the flat carpet.
		// Player clothing is parented to these legs; armor uses the same render state.
		this.leftLeg.xRot = this.rightLeg.xRot = -(float) Math.PI / 2.0f;
		this.leftLeg.yRot = 0.1f;
		this.rightLeg.yRot = -0.1f;
		this.leftLeg.zRot = this.rightLeg.zRot = 0.0f;
	}
}
