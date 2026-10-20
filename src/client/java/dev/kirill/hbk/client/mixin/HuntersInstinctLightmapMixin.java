package dev.kirill.hbk.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kirill.hbk.client.HuntersInstinctClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class HuntersInstinctLightmapMixin {
	@WrapOperation(method = "extract", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
	private boolean hbk$includeHuntersNightVision(LocalPlayer viewer, Holder<MobEffect> effect, Operation<Boolean> original) {
		return original.call(viewer, effect) || effect == MobEffects.NIGHT_VISION && HuntersInstinctClient.hasNightVision(viewer);
	}
}
