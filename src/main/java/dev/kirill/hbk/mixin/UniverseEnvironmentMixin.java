package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.UniverseEnvironment;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeSystem.Builder.class)
public abstract class UniverseEnvironmentMixin {
	@Inject(method = "addDefaultLayers", at = @At("RETURN"))
	private void hbk$universeLayers(Level level, CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
		UniverseEnvironment.addLayers((EnvironmentAttributeSystem.Builder) (Object) this, level);
	}
}
