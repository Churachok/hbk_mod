package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	private static final Identifier HBK_BLACK_SKIN = HbkMod.id("textures/entity/black_silhouette.png");

	@Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
	private void hbk$useSilhouetteTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
		if (state.chestEquipment.is(ModItems.WESTERN_CHESTPLATE)) {
			cir.setReturnValue(HBK_BLACK_SKIN);
		}
	}

	@Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z", at = @At("HEAD"), cancellable = true)
	private void hbk$hideSilhouetteLayers(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
		if (state.chestEquipment.is(ModItems.WESTERN_CHESTPLATE)) {
			cir.setReturnValue(false);
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
