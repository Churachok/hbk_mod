package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(Entity.class)
public abstract class AnomalyEntityMixin {
	@ModifyReturnValue(method = "getGravity", at = @At("RETURN"))
	private double hbk$anomalyGravity(double original) {
		return original * AnomalyZones.gravity((Entity) (Object) this);
	}

	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void hbk$freezeAnomalyMovement(MoverType type, Vec3 movement, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (AnomalyZones.frozen(entity)) {
			entity.setDeltaMovement(Vec3.ZERO);
			entity.resetFallDistance();
			ci.cancel();
		}
	}
}
