package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.HbkMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AdvancementToast.class)
public abstract class AdvancementToastMixin {
	@Shadow @Final private AdvancementHolder advancement;

	@Redirect(
			method = "extractRenderState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/advancements/AdvancementType;getDisplayName()Lnet/minecraft/network/chat/Component;"
			)
	)
	private Component hbk$hideUnknownAdvancementToastHeader(AdvancementType type) {
		return this.advancement.id().equals(HbkMod.id("unknown")) ? Component.empty() : type.getDisplayName();
	}

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 18))
	private int hbk$centerUnknownAdvancementToastTitle(int originalY) {
		return this.advancement.id().equals(HbkMod.id("unknown")) ? 11 : originalY;
	}
}
