package dev.kirill.hbk.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.kirill.hbk.client.HuntersInstinctClient;
import dev.kirill.hbk.client.FurryLoveClientEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class HuntersInstinctOutlineMixin {
	@ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
	private boolean hbk$highlightNearbyLivingEntities(boolean original, Entity entity) {
		return original || HuntersInstinctClient.shouldHighlight(entity) || FurryLoveClientEffects.shouldHighlight(entity);
	}
}
