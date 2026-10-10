package dev.kirill.hbk;

import dev.kirill.hbk.effect.BeerEffect;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;

public final class BeerGameTests {
	@GameTest
	public void beerConsumesOneBottleReturnsGlassAndGivesLevelOneEffectsForTwoMinutes(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.getFoodData().setFoodLevel(20);
			double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			ItemStack empty = new ItemStack(ModItems.BEER_BOTTLE).finishUsingItem(test.getLevel(), player);
			test.assertTrue(empty.is(Items.GLASS_BOTTLE), "Drinking one beer must return an empty glass bottle");
			for (var effect : List.of(ModEffects.BEER, MobEffects.HUNGER, MobEffects.REGENERATION, MobEffects.STRENGTH)) {
				var active = player.getEffect(effect);
				test.assertTrue(active != null && active.getDuration() == 2400 && active.getAmplifier() == 0,
						"Every beer effect must start at level I and last exactly two minutes");
			}
			test.assertTrue(player.getFoodData().getFoodLevel() == 20 && !player.hasEffect(MobEffects.NAUSEA),
					"Beer can be drunk at full hunger without adding food or immediate continuous nausea");
			test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack + 3,
					"Strength I must add exactly three melee damage");
			var stack = new ItemStack(ModItems.BEER_BOTTLE, 2);
			stack.finishUsingItem(test.getLevel(), player);
			test.assertTrue(stack.getCount() == 1 && player.getInventory().contains(item -> item.is(Items.GLASS_BOTTLE)),
					"Drinking from a stack must consume exactly one beer and keep its empty bottle");
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void beerDrainsSaturationPulsesNauseaForEightSecondsAndExpires(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.getFoodData().setSaturation(5);
			double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			new ItemStack(ModItems.BEER_BOTTLE).finishUsingItem(test.getLevel(), player);
			for (int tick = 0; tick < 400; tick++) tickEffectsAndFood(player);
			test.assertFalse(player.hasEffect(MobEffects.NAUSEA), "The first twenty seconds must be nausea-free");
			tickEffectsAndFood(player);
			test.assertTrue(player.getEffect(MobEffects.NAUSEA).getDuration() == 160,
					"A nausea pulse must last exactly eight seconds");
			for (int tick = 0; tick < 160; tick++) tickEffectsAndFood(player);
			test.assertFalse(player.hasEffect(MobEffects.NAUSEA), "Nausea must stop between pulses");
			for (int tick = 561; tick < 800; tick++) tickEffectsAndFood(player);
			test.assertFalse(player.hasEffect(MobEffects.NAUSEA), "Nausea must not run continuously until the next pulse");
			tickEffectsAndFood(player);
			test.assertTrue(player.hasEffect(MobEffects.NAUSEA), "The second pulse must follow twenty seconds after the first");
			for (int tick = 801; tick < 2400; tick++) tickEffectsAndFood(player);
			test.assertTrue(player.getActiveEffects().isEmpty() && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack,
					"All beer effects and the strength modifier must end after exactly two minutes");
			test.assertTrue(player.getFoodData().getSaturationLevel() < 5,
					"Hunger I must actually consume saturation, not only show an icon");
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void anotherBeerRefreshesWithoutStackingAndMilkStopsFurtherNausea(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setGameMode(GameType.SURVIVAL);
			double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			new ItemStack(ModItems.BEER_BOTTLE).finishUsingItem(test.getLevel(), player);
			for (int tick = 0; tick < 600; tick++) tickEffectsAndFood(player);
			new ItemStack(ModItems.BEER_BOTTLE).finishUsingItem(test.getLevel(), player);
			test.assertTrue(player.getEffect(ModEffects.BEER).getDuration() == BeerEffect.DURATION_TICKS
					&& player.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack + 3,
					"Another bottle must refresh two minutes without stacking strength");
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 3000, 1));
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 3000, 1));
			new ItemStack(ModItems.BEER_BOTTLE).finishUsingItem(test.getLevel(), player);
			test.assertTrue(player.getEffect(MobEffects.STRENGTH).getAmplifier() == 1
					&& player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1,
					"Beer must not downgrade stronger existing strength or regeneration");
			for (int tick = 0; tick < 401; tick++) tickEffectsAndFood(player);
			test.assertTrue(player.hasEffect(MobEffects.NAUSEA), "Milk test must begin during a nausea pulse");
			new ItemStack(Items.MILK_BUCKET).finishUsingItem(test.getLevel(), player);
			for (int tick = 0; tick < 800; tick++) tickEffectsAndFood(player);
			test.assertTrue(player.getActiveEffects().isEmpty(), "Milk must clear the beer timer and stop future nausea");
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	private static void tickEffectsAndFood(ServerPlayer player) {
		player.tickCount++;
		for (var effect : List.copyOf(player.getActiveEffects())) {
			if (!effect.tickServer(player.level(), player, () -> {})) player.removeEffect(effect.getEffect());
		}
		player.getFoodData().tick(player);
	}
}
