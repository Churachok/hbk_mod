package dev.kirill.hbk;

import dev.kirill.hbk.item.LiberalBloodBucketItem;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class LiberalBloodGameTests {
	@GameTest
	public void emptyBucketsCollectBloodWithoutPickingUpTheHead(GameTestHelper test) {
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		try {
			var head = test.spawnWithNoFreeWill(ModEntityTypes.LEBEDEV_HEAD, 2, 2, 2);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
			test.assertTrue(head.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(), "Filling must succeed");
			test.assertTrue(player.getMainHandItem().is(ModItems.LIBERAL_BLOOD_BUCKET)
					&& player.getMainHandItem().getCount() == 1, "One empty bucket becomes one blood bucket");
			test.assertFalse(head.isRemoved(), "Collecting blood must leave the head in the world");
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 3));
			head.interact(player, InteractionHand.OFF_HAND, Vec3.ZERO);
			test.assertTrue(player.getOffhandItem().is(Items.BUCKET) && player.getOffhandItem().getCount() == 2,
					"Offhand filling must consume just one empty bucket from a stack");
			test.assertTrue(player.getInventory().contains(s -> s.is(ModItems.LIBERAL_BLOOD_BUCKET)),
					"Filled bucket from a stack must enter the inventory");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
			head.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
			test.assertTrue(head.isRemoved() && player.getInventory().contains(s -> s.is(ModItems.LEBEDEV_HEAD)),
					"A different item must still pick up the head");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void drinkingClearsOldEffectsAndReducesBothMeleeAndProjectileDamage(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		try {
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH);
			player.getFoodData().setFoodLevel(20);
			player.addEffect(new MobEffectInstance(ModEffects.ONIGIRI, 300));
			player.addEffect(new MobEffectInstance(ModEffects.GOSHAS_RAGE, 300));
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 300));
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 3));
			var blood = new ItemStack(ModItems.LIBERAL_BLOOD_BUCKET);
			test.assertTrue(blood.get(net.minecraft.core.component.DataComponents.CONSUMABLE).canConsume(player, blood),
					"Blood must be drinkable even with full hunger");
			var empty = blood.finishUsingItem(level, player);
			test.assertTrue(empty.is(Items.BUCKET), "Drinking must return an empty bucket");
			test.assertTrue(player.getActiveEffects().size() == 3 && !player.hasEffect(ModEffects.ONIGIRI)
					&& !player.hasEffect(ModEffects.GOSHAS_RAGE) && !player.hasEffect(MobEffects.POISON),
					"Only Young Liberal, Regeneration I and Haste I may remain after drinking");
			for (var effect : java.util.List.of(ModEffects.YOUNG_LIBERAL, MobEffects.REGENERATION, MobEffects.HASTE)) {
				test.assertTrue(player.getEffect(effect).getDuration() == LiberalBloodBucketItem.DURATION_TICKS
						&& player.getEffect(effect).getAmplifier() == 0, "Each new effect must be level I for exactly one minute");
			}
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.6) < 0.00001
					&& Math.abs(player.getAttributeValue(Attributes.JUMP_STRENGTH) - jump * 1.6) < 0.00001,
					"Speed and jump strength must increase by exactly 60 percent");
			test.assertTrue(player.getFoodData().getFoodLevel() == 20 && player.getMaxHealth() == 20,
					"Blood must not change hunger; clearing rage must restore max health");
			var melee = test.spawnWithNoFreeWill(EntityTypes.COW, 1, 2, 1);
			melee.hurtServer(level, level.damageSources().playerAttack(player), 5);
			var arrow = new Arrow(EntityTypes.ARROW, level);
			arrow.setOwner(player);
			var ranged = test.spawnWithNoFreeWill(EntityTypes.COW, 3, 2, 1);
			ranged.hurtServer(level, level.damageSources().arrow(arrow, player), 5);
			test.assertTrue(Math.abs(melee.getHealth() - (melee.getMaxHealth() - 3)) < 0.0001
					&& Math.abs(ranged.getHealth() - (ranged.getMaxHealth() - 3)) < 0.0001,
					"Both melee and player-owned projectiles must lose 40 percent damage");
			new ItemStack(ModItems.LIBERAL_BLOOD_BUCKET).finishUsingItem(level, player);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.6) < 0.00001,
					"Drinking again must refresh the minute without stacking bonuses");
			// Exercise the real effect timer without waiting a wall-clock minute in every build.
			var effect = player.getEffect(ModEffects.YOUNG_LIBERAL);
			for (int i = 0; i < 1199; i++) {
				test.assertTrue(effect.tickServer(level, player, () -> {}), "Effect must last through tick 1199");
			}
			test.assertFalse(effect.tickServer(level, player, () -> {}), "Effect must end on tick 1200");
			player.removeEffect(ModEffects.YOUNG_LIBERAL);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 0.00001
					&& Math.abs(player.getAttributeValue(Attributes.JUMP_STRENGTH) - jump) < 0.00001,
					"Expiration must restore normal speed and jump strength");
		} finally {
			player.discard();
		}
		test.succeed();
	}
}
