package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class AnomalyMobMixin {
	@Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
	private void hbk$noSunburnInAnomaly(CallbackInfoReturnable<Boolean> cir) {
		if (AnomalyZones.contains((Mob) (Object) this)) cir.setReturnValue(false);
	}

	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void hbk$pauseAnomalyAi(CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (AnomalyZones.contains(mob)) {
			mob.getNavigation().stop();
			mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			ci.cancel();
		}
	}
}
