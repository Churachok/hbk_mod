package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.client.CargoMinecartRenderer;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractMinecartRenderer.class)
public abstract class AlloyMinecartTextureMixin {
	@ModifyArg(method = "submit(Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), index = 3)
	private Identifier hbk$cargoTexture(Identifier original) {
		return (Object) this instanceof CargoMinecartRenderer ? HbkMod.id("textures/entity/cargo_minecart.png") : original;
	}
}
