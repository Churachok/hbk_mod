package dev.kirill.hbk;

import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.mechanic.HuntersInstinct;
import dev.kirill.hbk.registry.ModEffects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.List;

/** Public drinking, real damage/death handling, vanilla effect timing and persistent effect data. */
public final class HuntersInstinctGameTests {
	@GameTest
	public void repeatedDrinkingAddsOnlyTimeAndRefreshesEightSecondsOfNausea(GameTestHelper test) {
		var player = player(test);
		try {
			double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			test.assertTrue(HuntersInstinctEffect.DURATION_TICKS == 3 * 60 * 20
					&& HuntersInstinctEffect.RESCUE_COST_TICKS == 2 * 60 * 20,
					"One sip grants three minutes and each rescue spends two minutes");
			for (int sip = 1; sip <= 3; sip++) {
				HuntersInstinct.drink(player);
				var instinct = player.getEffect(ModEffects.HUNTERS_INSTINCT);
				test.assertTrue(instinct != null && instinct.getDuration() == sip * HuntersInstinctEffect.DURATION_TICKS
						&& instinct.getAmplifier() == 0 && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + 6,
						"The first three sips must give three, six and nine minutes without accumulating levels or attack modifiers");
				test.assertTrue(player.getActiveEffects().size() == 2 && player.getEffect(MobEffects.NAUSEA).getDuration() == 160
						&& !player.hasEffect(MobEffects.STRENGTH) && !player.hasEffect(MobEffects.RESISTANCE)
						&& !player.hasEffect(MobEffects.REGENERATION) && !player.hasEffect(MobEffects.NIGHT_VISION),
						"Hunter bonuses must share one effect; only the requested short nausea is a separate vanilla effect");
			}
			for (int tick = 0; tick < 31; tick++) tickEffects(test.getLevel(), player);
			int remaining = duration(player);
			HuntersInstinct.drink(player);
			test.assertTrue(duration(player) == remaining + HuntersInstinctEffect.DURATION_TICKS
					&& player.getEffect(MobEffects.NAUSEA).getDuration() == 160
					&& player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + 6,
					"A later sip must preserve elapsed time, add exactly three minutes and restart the nausea window");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void resistanceMatchesVanillaTwoAndNeverMultipliesAnotherResistanceEffect(GameTestHelper test) {
		var level = test.getLevel();
		for (int vanillaAmplifier = -1; vanillaAmplifier <= 2; vanillaAmplifier++) {
			var player = player(test);
			var control = player(test);
			try {
				if (vanillaAmplifier >= 0) player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 8000, vanillaAmplifier));
				control.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 8000, Math.max(1, vanillaAmplifier)));
				HuntersInstinct.drink(player);
				test.assertTrue(player.hurtServer(level, level.damageSources().generic(), 10)
						&& control.hurtServer(level, level.damageSources().generic(), 10),
						"The comparison must apply genuine nonlethal unarmored damage");
				test.assertTrue(Math.abs(player.getHealth() - control.getHealth()) < 0.0001
						&& (vanillaAmplifier != -1 || Math.abs(player.getHealth() - 14) < 0.0001),
						"Hunter resistance must reduce damage by forty percent, using the stronger existing resistance instead of multiplying it");
			} finally {
				player.discard();
				control.discard();
			}
		}
		test.succeed();
	}

	@GameTest
	public void regenerationMatchesVanillaOneWithoutDuplicatingStrongerRegeneration(GameTestHelper test) {
		for (int vanillaAmplifier : new int[] {-1, 0, 1}) {
			var player = player(test);
			var control = player(test);
			try {
				player.setHealth(5);
				control.setHealth(5);
				if (vanillaAmplifier >= 0) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 3600, vanillaAmplifier));
				control.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 3600, Math.max(0, vanillaAmplifier)));
				HuntersInstinct.drink(player);
				for (int tick = 0; tick < 150; tick++) {
					tickEffects(test.getLevel(), player);
					tickEffects(test.getLevel(), control);
					test.assertTrue(player.getHealth() == control.getHealth(),
							"Hunter regeneration must match vanilla Regeneration I tick for tick, without adding a second heal to an existing regeneration effect");
				}
				test.assertTrue(player.getHealth() == (vanillaAmplifier == 1 ? 11 : 8),
						"Regeneration I must restore one health every fifty ticks, while existing Regeneration II keeps its faster rate");
			} finally {
				player.discard();
				control.discard();
			}
		}
		test.succeed();
	}

	@GameTest
	public void strongerVanillaEffectsSurviveDrinkingAndMilkOrExpiryRestoresOrdinaryCombat(GameTestHelper test) {
		var player = player(test);
		try {
			double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			for (int strength = 0; strength <= 2; strength++) {
				player.removeAllEffects();
				player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 8000, strength));
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 8000, 1));
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 8000, 2));
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 8000, 1));
				HuntersInstinct.drink(player);
				test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + Math.max(6, 3 * (strength + 1)),
						"Hunter Strength II must use the stronger attack bonus, never add six more damage to vanilla Strength");
				test.assertTrue(player.getEffect(MobEffects.STRENGTH).getAmplifier() == strength
						&& player.getEffect(MobEffects.STRENGTH).getDuration() == 8000
						&& player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1
						&& player.getEffect(MobEffects.REGENERATION).getDuration() == 8000
						&& player.getEffect(MobEffects.RESISTANCE).getAmplifier() == 2
						&& player.getEffect(MobEffects.RESISTANCE).getDuration() == 8000
						&& player.getEffect(MobEffects.SPEED).getDuration() == 8000,
						"Drinking must not downgrade, refresh, remove or conceal unrelated existing potion effects");
				player.removeEffect(ModEffects.HUNTERS_INSTINCT);
				test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + 3 * (strength + 1)
						&& player.hasEffect(MobEffects.STRENGTH) && player.hasEffect(MobEffects.RESISTANCE),
						"Removing the custom effect must leave pre-existing vanilla buffs intact with their normal strength");
			}
			new ItemStack(Items.MILK_BUCKET).finishUsingItem(test.getLevel(), player);
			test.assertTrue(player.getActiveEffects().isEmpty() && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack,
					"Milk must clear every active potion effect and restore base attack damage");
			HuntersInstinct.drink(player);
			new ItemStack(Items.MILK_BUCKET).finishUsingItem(test.getLevel(), player);
			for (int tick = 0; tick < 201; tick++) tickEffects(test.getLevel(), player);
			test.assertTrue(player.getActiveEffects().isEmpty() && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack,
					"Milk must stop hunter bonuses and nausea without hidden timers restoring them later");
			HuntersInstinct.drink(player);
			for (int tick = 0; tick < HuntersInstinctEffect.DURATION_TICKS; tick++) tickEffects(test.getLevel(), player);
			test.assertTrue(player.getActiveEffects().isEmpty() && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack,
					"Natural expiry must remove all hunter bonuses and the independent short nausea effect");
			player.setHealth(20);
			player.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 10);
			test.assertTrue(player.getHealth() == 10, "Expired resistance must no longer reduce incoming damage");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void everyLethalHitSpendsTwoMinutesAndRepeatedRescuesHaveNoExtraCooldown(GameTestHelper test) {
		var player = player(test);
		try {
			for (int sip = 0; sip < 3; sip++) HuntersInstinct.drink(player);
			for (int rescue = 0; rescue < 5; rescue++) {
				int before = duration(player);
				player.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 1000);
				test.assertTrue(player.isAlive() && player.getHealth() == 2
						&& duration(player) == Math.max(0, before - HuntersInstinctEffect.RESCUE_COST_TICKS),
						"Every otherwise-lethal ordinary hit must save exactly one heart and spend two minutes, keeping any positive remainder");
				for (int tick = 0; tick < 21; tick++) player.tick();
				test.assertTrue(player.invulnerableTime == 0,
						"The next rescue attempt must occur after Minecraft's genuine damage-immunity window");
			}
			test.assertFalse(player.hasEffect(ModEffects.HUNTERS_INSTINCT), "The fifth rescue must exhaust the remaining nine-minute batch");
			player.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 1000);
			test.assertFalse(player.isAlive(), "The next lethal hit must kill normally after the hunter timer is exhausted");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void rescueConsumesShortRemaindersButKeepsOneTickAboveTheCost(GameTestHelper test) {
		for (int ticks : new int[] {1, HuntersInstinctEffect.RESCUE_COST_TICKS, HuntersInstinctEffect.RESCUE_COST_TICKS + 1}) {
			var player = player(test);
			try {
				player.addEffect(new MobEffectInstance(ModEffects.HUNTERS_INSTINCT, ticks));
				player.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 1000);
				test.assertTrue(player.isAlive() && player.getHealth() == 2
						&& duration(player) == Math.max(0, ticks - HuntersInstinctEffect.RESCUE_COST_TICKS)
						&& player.hasEffect(ModEffects.HUNTERS_INSTINCT) == (ticks > HuntersInstinctEffect.RESCUE_COST_TICKS),
						"Even one remaining tick grants the final rescue; exactly two minutes are removed and a 2401-tick effect keeps one tick");
			} finally {
				player.discard();
			}
		}
		test.succeed();
	}

	@GameTest
	public void voidAndKillBypassHunterRescueWhileVanillaOffhandTotemTakesPriority(GameTestHelper test) {
		var level = test.getLevel();
		for (var source : List.of(level.damageSources().fellOutOfWorld(), level.damageSources().genericKill())) {
			var player = player(test);
			try {
				HuntersInstinct.drink(player);
				player.hurtServer(level, source, 1000);
				test.assertFalse(player.isAlive(), "Void and /kill damage must remain lethal despite an active hunter timer");
			} finally {
				player.discard();
			}
		}
		var player = player(test);
		try {
			HuntersInstinct.drink(player);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			player.hurtServer(level, level.damageSources().generic(), 1000);
			test.assertTrue(player.isAlive() && player.getHealth() == 1 && player.getOffhandItem().isEmpty()
					&& player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.ABSORPTION)
					&& !player.hasEffect(ModEffects.HUNTERS_INSTINCT),
					"A held vanilla totem must run first, consume itself, grant its normal effects and clear the hunter effect normally");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void accumulatedDurationAndOtherEffectsSurviveNativeEntitySaving(GameTestHelper test) {
		var level = test.getLevel();
		for (int strength = 0; strength <= 2; strength++) {
			var zombie = test.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 2, 2, 2);
			var restored = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.LOAD);
			try {
				double baseAttack = zombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
				zombie.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 8000, strength));
				HuntersInstinct.drink(zombie);
				HuntersInstinct.drink(zombie);
				zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, 8000, 1));
				for (int tick = 0; tick < 237; tick++) tickEffects(level, zombie);
				var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				zombie.saveWithoutId(output);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				// Check before the first tick: ticking would hide stale or missing saved attack modifiers.
				test.assertTrue(restored.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + Math.max(6, 3 * (strength + 1)),
						"Saving with Strength I, II or III must retain the stronger attack bonus immediately after loading, before any effect tick repairs it");
				test.assertTrue(duration(restored) == 2 * HuntersInstinctEffect.DURATION_TICKS - 237
						&& restored.getEffect(ModEffects.HUNTERS_INSTINCT).getAmplifier() == 0
						&& restored.getEffect(MobEffects.STRENGTH).getAmplifier() == strength
						&& restored.getEffect(MobEffects.STRENGTH).getDuration() == 8000 - 237
						&& restored.getEffect(MobEffects.SPEED).getDuration() == 8000 - 237
						&& restored.getEffect(MobEffects.SPEED).getAmplifier() == 1 && !restored.hasEffect(MobEffects.NAUSEA),
						"Entity save/load must retain the exact accumulated remaining time, level and unrelated effects, without reviving expired nausea");
				int before = duration(restored);
				HuntersInstinct.drink(restored);
				test.assertTrue(duration(restored) == before + HuntersInstinctEffect.DURATION_TICKS
						&& restored.getEffect(MobEffects.NAUSEA).getDuration() == 160,
						"A reloaded effect must accept another additive sip rather than reset to three minutes");
			} finally {
				zombie.discard();
				if (restored != null) restored.discard();
			}
		}
		test.succeed();
	}

	private static Player player(GameTestHelper test) {
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.setNoGravity(true);
		player.setOnGround(false);
		player.getFoodData().setFoodLevel(6);
		player.setHealth(20);
		return player;
	}

	private static int duration(LivingEntity entity) {
		var effect = entity.getEffect(ModEffects.HUNTERS_INSTINCT);
		return effect == null ? 0 : effect.getDuration();
	}

	private static void tickEffects(ServerLevel level, LivingEntity entity) {
		entity.tickCount++;
		for (var effect : List.copyOf(entity.getActiveEffects())) {
			if (!effect.tickServer(level, entity, () -> {})) entity.removeEffect(effect.getEffect());
		}
	}
}
