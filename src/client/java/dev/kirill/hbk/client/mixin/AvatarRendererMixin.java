package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.client.ProgenitorClientState;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	private static final Identifier HBK_BLACK_SKIN = HbkMod.id("textures/entity/black_silhouette.png");
	private static final Identifier HBK_PROGENITOR_SKIN = HbkMod.id("textures/entity/progenitor.png");

	@Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
	private void hbk$useSilhouetteTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
		if (ProgenitorClientState.isActive(state)) {
			cir.setReturnValue(HBK_PROGENITOR_SKIN);
		} else if (state.chestEquipment.is(ModItems.WESTERN_CHESTPLATE)) {
			cir.setReturnValue(HBK_BLACK_SKIN);
		}
	}

	@Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z", at = @At("HEAD"), cancellable = true)
	private void hbk$hideSilhouetteLayers(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
		if (ProgenitorClientState.isActive(state) || state.chestEquipment.is(ModItems.WESTERN_CHESTPLATE)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
	private void hbk$hideHumanHand(PoseStack poseStack, SubmitNodeCollector collector, int light,
			Identifier texture, ModelPart arm, boolean rightHand, CallbackInfo ci) {
		var player = Minecraft.getInstance().player;
		if (player != null && dev.kirill.hbk.mechanic.ProgenitorTransformation.isActive(player)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Identifier hbk$blackFirstPersonHand(Identifier original) {
		var player = Minecraft.getInstance().player;
		return player != null && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(ModItems.WESTERN_CHESTPLATE)
				? HBK_BLACK_SKIN
				: original;
	}
}
