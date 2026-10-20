package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class AlloyCombatMixin {
	@ModifyVariable(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double hbk$anchored(double strength) {
		return AlloyActions.equipped((LivingEntity) (Object) this, EquipmentSlot.FEET, "anchor_boots") ? strength * .25 : strength;
	}
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float hbk$blastProtection(float amount, ServerLevel level, DamageSource source) {
		var entity = (LivingEntity) (Object) this; var shield = entity.getItemBlockingWith();
		if (source.is(DamageTypeTags.IS_EXPLOSION) && shield != null && UniverseAlloys.is(shield, "blast_shield")) {
			shield.hurtAndBreak(Math.max(1, (int) Math.ceil(amount * 2)), entity, entity.getUsedItemHand());
			return amount * .35f;
		}
		return amount;
	}
	@Inject(method = "blockUsingItem", at = @At("HEAD"))
	private void hbk$springShield(ServerLevel level, LivingEntity attacker, DamageSource source, float blocked, CallbackInfo ci) {
		var defender = (LivingEntity) (Object) this; var shield = defender.getItemBlockingWith();
		if (blocked > 0 && shield != null && UniverseAlloys.is(shield, "spring_shield")) {
			var delta = attacker.position().subtract(defender.position()).normalize(); attacker.push(delta.scale(1.1).add(0, .2, 0));
		}
	}
	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void hbk$impulseBolt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> result) {
		if (result.getReturnValue() && source.getDirectEntity() != null && source.getDirectEntity().entityTags().contains("hbk_impulse_bolt")) {
			var victim = (LivingEntity) (Object) this; var direction = source.getDirectEntity().getDeltaMovement().normalize(); victim.push(direction.scale(1.1).add(0, .15, 0));
			victim.hurtMarked = true;
		}
	}
}
