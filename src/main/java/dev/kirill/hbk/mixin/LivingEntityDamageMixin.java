package dev.kirill.hbk.mixin;

import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.entity.NkvdEntity;
import dev.kirill.hbk.entity.ReferenceNpcEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void hbk$protectPipeGuardBystanders(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> result) {
		LivingEntity victim = (LivingEntity) (Object) this;
		if (source.getEntity() instanceof NkvdEntity guard && (
				victim instanceof ReferenceNpcEntity npc && npc.isNpc("grisha")
						|| guard.isPipeGuard() && !NkvdEntity.isPipeGuardEnemy(victim))) {
			result.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float hbk$modifyPlayerDamage(float amount, ServerLevel level, DamageSource source) {
		LivingEntity victim = (LivingEntity) (Object) this;
		if (source.getEntity() instanceof Player attacker && attacker != victim) {
			if (attacker.hasEffect(ModEffects.GOSHAS_RAGE)) {
				amount *= 3.0f;
			}
			if (attacker.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.KIRILL_GLASSES)) {
				amount *= 1.5f;
			}
		}
		return amount;
	}
}
