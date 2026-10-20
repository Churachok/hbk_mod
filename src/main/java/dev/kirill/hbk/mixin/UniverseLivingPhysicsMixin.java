package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class UniverseLivingPhysicsMixin {
	@Inject(method = "getJumpPower(F)F", at = @At("RETURN"), cancellable = true)
	private void hbk$jump(float scale, CallbackInfoReturnable<Float> cir) {
		var profile = UniverseProfile.of(((LivingEntity) (Object) this).level());
		if (profile != null) cir.setReturnValue((float) (cir.getReturnValue() * profile.jump()));
	}
	@Inject(method = "getSpeed()F", at = @At("RETURN"), cancellable = true)
	private void hbk$speed(CallbackInfoReturnable<Float> cir) {
		var profile = UniverseProfile.of(((LivingEntity) (Object) this).level());
		if (profile != null) cir.setReturnValue(cir.getReturnValue() * (.8f + profile.get(HiddenParameter.MOVEMENT) * .05f));
	}
	@Inject(method = "travelInAir", at = @At("TAIL"))
	private void hbk$air(net.minecraft.world.phys.Vec3 input, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		var profile = UniverseProfile.of(entity.level());
		if (profile == null || entity.isNoGravity() || entity.isFallFlying() || entity.onGround()) return;
		var motion = entity.getDeltaMovement();
		double drag = 1.008 - profile.get(HiddenParameter.AIR_DRAG) * .002;
		double terminal = new double[]{.4, .55, .7, .85, 1, 1.1, 1.2, 1.35, 1.5, 1.75}[profile.get(HiddenParameter.TERMINAL_SPEED)] * 3.92;
		entity.setDeltaMovement(motion.x * drag, Math.max(-terminal, motion.y), motion.z * drag);
	}
}
