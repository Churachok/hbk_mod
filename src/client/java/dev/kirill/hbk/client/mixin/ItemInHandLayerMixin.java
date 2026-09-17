package dev.kirill.hbk.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the normal held-item layer from drawing the weapon in either hand. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
	@Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
	private void hbk$hideAttackingMemberInHand(
			ArmedEntityRenderState state,
			ItemStackRenderState itemState,
			ItemStack stack,
			HumanoidArm arm,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int light,
			CallbackInfo ci) {
		if (ModItems.isMemberWeapon(stack)) {
			ci.cancel();
		}
	}
}
