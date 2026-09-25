package dev.kirill.hbk.mixin;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float hbk$tripleGoshasRageDamage(float amount, ServerLevel level, DamageSource source) {
		LivingEntity victim = (LivingEntity) (Object) this;
		if (source.getEntity() instanceof Player attacker && attacker != victim
				&& attacker.hasEffect(ModEffects.GOSHAS_RAGE)) {
			return amount * 3.0f;
		}
		return amount;
	}
}
