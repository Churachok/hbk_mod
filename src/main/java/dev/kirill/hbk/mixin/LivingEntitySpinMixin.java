package dev.kirill.hbk.mixin;

import dev.kirill.hbk.entity.FunnySpinAccess;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpinMixin implements FunnySpinAccess {
	@Unique
	private static final EntityDataAccessor<Boolean> HBK_FUNNY_SPIN =
			SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void hbk$defineFunnySpin(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(HBK_FUNNY_SPIN, false);
	}

	@Inject(method = "tickEffects", at = @At("TAIL"))
	private void hbk$syncFunnySpin(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.level().isClientSide()) {
			entity.getEntityData().set(HBK_FUNNY_SPIN, entity.isAlive() && entity.hasEffect(ModEffects.FUNNY_SPIN));
		}
	}

	@Override
	public boolean hbk$isFunnySpinning() {
		return ((LivingEntity) (Object) this).getEntityData().get(HBK_FUNNY_SPIN);
	}
}
