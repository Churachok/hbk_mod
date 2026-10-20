package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** A timed form on the existing player, with armour, regeneration and a four-legged sprint bonus. */
public final class FurryTransformation {
	public static final int NO_COLOR = -1;
	public static final int COLOR_COUNT = 16;
	public static final int DURATION_TICKS = 5 * 60 * 20;
	public static final float RAGE_CHANCE = 0.03f;
	public static final double ARMOR_BONUS = 4.0;
	public static final Identifier ARMOR_MODIFIER_ID = HbkMod.id("furry_armor");
	public static final Identifier SPRINT_SPEED_MODIFIER_ID = HbkMod.id("furry_sprint_speed");
	public static final int REGENERATION_INTERVAL_TICKS = 25;
	private static final double SPEED_II_MULTIPLIER = 1.4;

	private FurryTransformation() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damageTaken, blocked) ->
				onMeleeHit(victim, source, damageTaken > 0.0f && !blocked));
	}

	/** Reads the same synced attachment for local and remote player renderers without creating client-side state. */
	public static int getColorId(Player player) {
		if (player == null) return NO_COLOR;
		int color = player.getAttachedOrElse(ModAttachments.FURRY_COLOR, NO_COLOR);
		return color >= 0 && color < COLOR_COUNT ? color : NO_COLOR;
	}

	public static boolean isActive(Player player) {
		return getColorId(player) != NO_COLOR;
	}

	/** Exact server expiry also applies to hits that arrive before the end-of-tick cleanup. */
	public static boolean hasProtection(LivingEntity entity) {
		return entity instanceof Player player && player.isAlive() && !player.isSpectator() && isActive(player)
				&& (!(player.level() instanceof ServerLevel level)
						|| player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L)
								> level.getServer().overworld().getGameTime());
	}

	public static boolean isSprinting(Player player) {
		return hasProtection(player) && player.isSprinting() && !player.isPassenger()
				&& !player.isSwimming() && !player.isFallFlying();
	}

	/** Healing is a love-mode bonus, not a passive bonus of the furry appearance. */
	public static boolean hasLoveRegeneration(LivingEntity entity) {
		return entity instanceof Player && FurryBreeding.isInLove(entity);
	}

	/** No projectiles, rejected damage, ordinary humans or posthumous infections. */
	public static void onMeleeHit(LivingEntity victim, DamageSource source, boolean damageApplied) {
		if (!damageApplied || !victim.isAlive() || victim.level().isClientSide()
				|| !source.is(DamageTypes.PLAYER_ATTACK) || !(source.getEntity() instanceof Player attacker)
				|| source.getDirectEntity() != attacker || attacker == victim || !hasProtection(attacker)) return;
		if (attacker.getRandom().nextFloat() < RAGE_CHANCE) {
			victim.addEffect(new MobEffectInstance(ModEffects.GOSHAS_RAGE, GoshasRageEffect.DURATION_TICKS, 0), attacker);
		}
	}

	public static boolean start(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) return false;
		long now = player.level().getServer().overworld().getGameTime();
		player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now + DURATION_TICKS);
		player.setAttached(ModAttachments.FURRY_COLOR, player.getRandom().nextInt(COLOR_COUNT));
		syncFormBonuses(player);
		return true;
	}

	public static void tick(ServerPlayer player) {
		if ((player.hasAttached(ModAttachments.FURRY_COLOR) || player.hasAttached(ModAttachments.FURRY_EXPIRES_AT))
				&& !hasProtection(player)) {
			player.removeAttached(ModAttachments.FURRY_COLOR);
			player.removeAttached(ModAttachments.FURRY_EXPIRES_AT);
		}
		syncFormBonuses(player);
	}

	public static void syncFormBonuses(Player player) {
		var armor = player.getAttribute(Attributes.ARMOR);
		if (armor != null) {
			var current = armor.getModifier(ARMOR_MODIFIER_ID);
			if (!hasProtection(player)) {
				if (current != null) armor.removeModifier(ARMOR_MODIFIER_ID);
			} else if (current == null || current.amount() != ARMOR_BONUS) {
				armor.addOrUpdateTransientModifier(new AttributeModifier(ARMOR_MODIFIER_ID, ARMOR_BONUS,
						AttributeModifier.Operation.ADD_VALUE));
			}
		}
		syncSprintSpeed(player);
	}

	/** Runs before native effect ticks so an expiring potion cannot also receive the fallback heal. */
	public static void tickRegeneration(ServerPlayer player) {
		if (hasLoveRegeneration(player) && !player.hasEffect(MobEffects.REGENERATION)
				&& player.tickCount % REGENERATION_INTERVAL_TICKS == 0 && player.getHealth() < player.getMaxHealth()) {
			player.heal(1.0f);
		}
	}

	/** Supplement weaker vanilla Speed instead of stacking or removing someone else's potion. */
	public static void syncSprintSpeed(Player player) {
		var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movement == null) return;
		var potion = player.getEffect(MobEffects.SPEED);
		double potionMultiplier = potion == null ? 1.0 : 1.0 + 0.2 * (potion.getAmplifier() + 1);
		double amount = isSprinting(player) ? Math.max(0.0, SPEED_II_MULTIPLIER / potionMultiplier - 1.0) : 0.0;
		var current = movement.getModifier(SPRINT_SPEED_MODIFIER_ID);
		if (amount == 0.0) {
			if (current != null) movement.removeModifier(SPRINT_SPEED_MODIFIER_ID);
		} else if (current == null || Double.compare(current.amount(), amount) != 0) {
			movement.addOrUpdateTransientModifier(new AttributeModifier(SPRINT_SPEED_MODIFIER_ID, amount,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
