package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class AnomalyServerLevelMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void hbk$disableAnomalyTransport(Entity entity, CallbackInfo ci) {
		if (AnomalyZones.transport(entity) && AnomalyZones.contains(entity)) {
			entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			ci.cancel();
		}
	}
}
