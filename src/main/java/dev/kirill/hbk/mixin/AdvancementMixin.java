package dev.kirill.hbk.mixin;

import dev.kirill.hbk.HbkMod;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Advancement.class)
public abstract class AdvancementMixin {
	@Inject(method = "name", at = @At("RETURN"), cancellable = true)
	private static void hbk$useBlackUnknownAdvancementBrackets(
			AdvancementHolder advancement,
			CallbackInfoReturnable<Component> cir
	) {
		if (advancement.id().equals(HbkMod.id("unknown"))) {
			cir.setReturnValue(cir.getReturnValue().copy().withStyle(ChatFormatting.BLACK));
		}
	}
}
