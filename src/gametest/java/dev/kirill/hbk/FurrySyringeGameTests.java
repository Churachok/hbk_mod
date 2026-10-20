package dev.kirill.hbk;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.mechanic.FurryTransformation;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.UUID;

public final class FurrySyringeGameTests {
	@GameTest
	public void survivalUsesAreInstantAndConsumeExactlyOneSyringeFromEitherHand(GameTestHelper test) {
		var player = player(test, GameType.SURVIVAL);
		try {
			test.assertTrue(FurryTransformation.getColorId(player) == -1 && !FurryTransformation.isActive(player),
					"An ordinary new player must begin without a furry appearance");
			for (var hand : InteractionHand.values()) {
				var otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
				player.setItemInHand(otherHand, new ItemStack(Items.DIAMOND, 3));
				player.setItemInHand(hand, new ItemStack(ModItems.FURRY_SYRINGE, 2));
				test.assertTrue(ModItems.FURRY_SYRINGE.use(test.getLevel(), player, hand).consumesAction()
						&& FurryTransformation.isActive(player) && !player.isUsingItem()
						&& player.getItemInHand(hand).is(ModItems.FURRY_SYRINGE) && player.getItemInHand(hand).getCount() == 1
						&& player.getItemInHand(otherHand).is(Items.DIAMOND) && player.getItemInHand(otherHand).getCount() == 3,
						"Self-use must immediately transform the player and consume only one held syringe, without touching the other hand");
				test.assertTrue(player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L) == now(test) + 6000,
						"Every successful syringe must start an exact five-minute deadline");
				test.assertTrue(ModItems.FURRY_SYRINGE.use(test.getLevel(), player, hand).consumesAction()
						&& player.getItemInHand(hand).isEmpty(),
						"A repeated use must work immediately and consume the last syringe instead of imposing a cooldown");
			}
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void creativeUsesKeepBothHeldStacksAndRefreshTheDeadline(GameTestHelper test) {
		var player = player(test, GameType.CREATIVE);
		try {
			for (var hand : InteractionHand.values()) {
				player.setItemInHand(hand, new ItemStack(ModItems.FURRY_SYRINGE, 4));
				FurryTransformation.start(player);
				player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test) + 1);
				test.assertTrue(ModItems.FURRY_SYRINGE.use(test.getLevel(), player, hand).consumesAction()
						&& player.getItemInHand(hand).getCount() == 4 && FurryTransformation.isActive(player)
						&& player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L) == now(test) + FurryTransformation.DURATION_TICKS,
						"Creative use must reroll and refresh all five minutes without consuming a syringe in either hand");
			}
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void deterministicRerollsUseAllSixteenUniformNativeRandomChoicesIncludingRepeats(GameTestHelper test) {
		var player = player(test, GameType.CREATIVE);
		try {
			var seeds = RandomSource.create(0x46555252594CL);
			int seen = 0;
			int previous = -1;
			boolean repeated = false;
			for (int roll = 0; roll < 512; roll++) {
				long seed = seeds.nextLong();
				int expected = RandomSource.create(seed).nextInt(16);
				player.getRandom().setSeed(seed);
				test.assertTrue(FurryTransformation.start(player) && FurryTransformation.getColorId(player) == expected,
						"Each reroll must be exactly the native uniform nextInt(16), without forced colour changes or weighted colours");
				seen |= 1 << expected;
				repeated |= previous == expected;
				previous = expected;
			}
			test.assertTrue(seen == 0xFFFF && repeated,
					"The fixed deterministic samples must cover all sixteen colours and demonstrate that rerolling the same colour remains valid");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void deadSpectatorAndNonServerPlayersCannotActivateOrSpendASyringe(GameTestHelper test) {
		var player = player(test, GameType.SPECTATOR);
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_SYRINGE, 2));
			test.assertTrue(!ModItems.FURRY_SYRINGE.use(test.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction()
					&& !FurryTransformation.start(player) && !FurryTransformation.isActive(player)
					&& player.getMainHandItem().getCount() == 2,
					"Spectators must not receive a colour, deadline or consumed syringe");
			player.setGameMode(GameType.SURVIVAL);
			player.setHealth(0);
			test.assertTrue(!ModItems.FURRY_SYRINGE.use(test.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction()
					&& !FurryTransformation.start(player) && !FurryTransformation.isActive(player)
					&& player.getMainHandItem().getCount() == 2,
					"A dead player must not activate the appearance or consume an item");
		} finally {
			removePlayer(test, player);
		}
		var nonServerPlayer = test.makeMockPlayer(GameType.SURVIVAL);
		try {
			nonServerPlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.FURRY_SYRINGE, 2));
			test.assertTrue(!ModItems.FURRY_SYRINGE.use(test.getLevel(), nonServerPlayer, InteractionHand.OFF_HAND).consumesAction()
					&& FurryTransformation.getColorId(nonServerPlayer) == -1 && nonServerPlayer.getOffhandItem().getCount() == 2,
					"The common item must require an authoritative ServerPlayer before mutating appearance or inventory");
		} finally {
			nonServerPlayer.discard();
		}
		test.succeed();
	}

	@GameTest
	public void transformationAddsOnlyFourArmorAndPreservesOtherAttributesHitboxAndUnrelatedItems(GameTestHelper test) {
		var player = player(test, GameType.SURVIVAL);
		try {
			player.getInventory().setSelectedSlot(2);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_SYRINGE, 3));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
			player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 17));
			player.getInventory().setItem(8, new ItemStack(Items.COBBLESTONE, 61));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 1));
			player.setHealth(11);
			player.getFoodData().setFoodLevel(7);
			player.getFoodData().setSaturation(3);
			player.experienceLevel = 4;
			player.totalExperience = 100;
			player.experienceProgress = 0.25f;
			var attributes = BuiltInRegistries.ATTRIBUTE.stream().map(BuiltInRegistries.ATTRIBUTE::wrapAsHolder)
					.filter(player.getAttributes()::hasAttribute).toList();
			var beforeValues = attributes.stream().map(player::getAttributeValue).toList();
			var box = player.getBoundingBox();
			float eye = player.getEyeHeight();
			float width = player.getBbWidth(), height = player.getBbHeight();
			var expectedInventory = new ArrayList<ItemStack>();
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				expectedInventory.add(player.getInventory().getItem(slot).copy());
			}
			expectedInventory.get(2).shrink(1);
			int wolves = test.getEntities(ModEntityTypes.PINK_FURRY_WOLF).size();
			test.assertTrue(ModItems.FURRY_SYRINGE.use(test.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(),
					"The invariance fixture must actually use a syringe");
			for (int attribute = 0; attribute < attributes.size(); attribute++) {
				double expected = beforeValues.get(attribute) + (attributes.get(attribute).equals(Attributes.ARMOR) ? 4 : 0);
				test.assertTrue(player.getAttributeValue(attributes.get(attribute)) == expected,
						"Transformation must add exactly four armor while preserving every other non-sprinting player attribute");
			}
			test.assertTrue(player.getHealth() == 11 && player.getFoodData().getFoodLevel() == 7 && player.getFoodData().getSaturationLevel() == 3
					&& player.experienceLevel == 4 && player.totalExperience == 100 && player.experienceProgress == 0.25f
					&& player.getBoundingBox().equals(box) && player.getEyeHeight() == eye
					&& player.getBbWidth() == width && player.getBbHeight() == height
					&& player.getActiveEffects().size() == 1 && player.getEffect(MobEffects.SPEED).getDuration() == 300,
					"Apart from the specified armor bonus, health, hunger, XP, hitbox, eye height and existing effects must stay intact on injection");
			for (int slot = 0; slot < expectedInventory.size(); slot++) {
				test.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), expectedInventory.get(slot)),
						"Transformation must preserve the complete inventory except for the single syringe spent");
			}
			test.assertTrue(test.getEntities(ModEntityTypes.PINK_FURRY_WOLF).size() == wolves,
					"The syringe must change the existing player, not create a furry-wolf NPC");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void nativePlayerSavingRetainsTheSyncedColourButExpiryAndDeathRemoveTheAppearance(GameTestHelper test) {
		var player = player(test, GameType.SURVIVAL);
		try {
			test.assertTrue(FurryTransformation.DURATION_TICKS == 6000 && ModAttachments.FURRY_COLOR.isPersistent()
					&& ModAttachments.FURRY_COLOR.isSynced() && ModAttachments.FURRY_EXPIRES_AT.isPersistent()
					&& !ModAttachments.FURRY_EXPIRES_AT.isSynced(),
					"Colour must use Fabric's persistent synchronized attachment, while the exact five-minute deadline stays server-only");
			for (int colour = -1; colour < 16; colour++) {
				var encoded = ModAttachments.FURRY_COLOR.persistenceCodec().encodeStart(JsonOps.INSTANCE, colour).getOrThrow();
				test.assertTrue(ModAttachments.FURRY_COLOR.persistenceCodec().parse(JsonOps.INSTANCE, encoded).getOrThrow() == colour,
						"The persistent attachment codec must round-trip no-form and all sixteen sheep colours");
			}
			FurryTransformation.start(player);
			int colour = FurryTransformation.getColorId(player);
			long deadline = player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
			player.saveWithoutId(output);
			player.removeAttached(ModAttachments.FURRY_COLOR);
			player.removeAttached(ModAttachments.FURRY_EXPIRES_AT);
			player.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), output.buildResult()));
			test.assertTrue(FurryTransformation.getColorId(player) == colour
					&& player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L) == deadline,
					"Native player save/load must preserve the exact colour and original deadline without rerolling or refreshing it");
			player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test) + 1);
			FurryTransformation.tick(player);
			test.assertTrue(FurryTransformation.isActive(player) && FurryTransformation.getColorId(player) == colour,
					"The appearance must remain active until the last tick before its deadline");
			player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
			FurryTransformation.tick(player);
			test.assertTrue(FurryTransformation.getColorId(player) == -1 && !FurryTransformation.isActive(player)
					&& !player.hasAttached(ModAttachments.FURRY_COLOR) && !player.hasAttached(ModAttachments.FURRY_EXPIRES_AT),
					"At the exact deadline both appearance attachments must be removed");
			FurryTransformation.start(player);
			player.setHealth(0);
			FurryTransformation.tick(player);
			test.assertTrue(FurryTransformation.getColorId(player) == -1 && !FurryTransformation.isActive(player)
					&& !player.hasAttached(ModAttachments.FURRY_COLOR) && !player.hasAttached(ModAttachments.FURRY_EXPIRES_AT),
					"Death must end the appearance before its five-minute deadline and leave no colour or timer behind");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void formArmorAndSprintSpeedRespectEquipmentAndStrongerPotionsAndCleanUpOnlyTheirOwnBonuses(GameTestHelper test) {
		var player = player(test, GameType.SURVIVAL);
		try {
			// A real ServerPlayer rejects damage until its native client-loaded acknowledgement arrives.
			player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
			test.assertTrue(player.connection.hasClientLoaded(), "The damage fixture must finish the native client-loading immunity");
			double walking = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			double ordinaryArmor = player.getAttributeValue(Attributes.ARMOR);
			FurryTransformation.start(player);
			test.assertTrue(FurryTransformation.hasProtection(player) && !FurryTransformation.isSprinting(player)
					&& FurryTransformation.ARMOR_BONUS == 4 && player.getAttributeValue(Attributes.ARMOR) == ordinaryArmor + 4
					&& player.getAttributeValue(Attributes.MOVEMENT_SPEED) == walking && player.getActiveEffects().isEmpty(),
					"Walking form must add four armor without a speed boost or synthetic vanilla potion effects");
			player.setHealth(20);
			test.assertTrue(player.hurtServer(test.getLevel(), test.getLevel().damageSources().magic(), 10)
					&& player.getHealth() == 10,
					"Armor-bypassing magic must deal full damage: the form must no longer retain a hidden Resistance I bonus");
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			player.doTick();
			test.assertTrue(player.getAttributeValue(Attributes.ARMOR) == ordinaryArmor + 8 + 4,
					"The form's four armor must add to the real eight armor supplied by an equipped diamond chestplate");
			player.setSprinting(true);
			FurryTransformation.tick(player);
			test.assertTrue(FurryTransformation.isSprinting(player)
					&& Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - walking * 1.3 * 1.4) < 0.000001,
					"Four-legged sprinting must add precisely Speed II on top of vanilla's ordinary sprint modifier");
			player.setSprinting(false);
			FurryTransformation.tick(player);
			test.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == walking
					&& player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(FurryTransformation.SPRINT_SPEED_MODIFIER_ID) == null,
					"Stopping sprinting must immediately remove only the form-owned movement modifier");
			player.setSprinting(true);
			for (int amplifier = 0; amplifier <= 2; amplifier++) {
				player.removeEffect(MobEffects.SPEED);
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 8000, amplifier));
				FurryTransformation.tick(player);
				double expected = walking * 1.3 * Math.max(1.4, 1 + 0.2 * (amplifier + 1));
				test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - expected) < 0.000001
						&& player.getEffect(MobEffects.SPEED).getAmplifier() == amplifier
						&& player.getEffect(MobEffects.SPEED).getDuration() == 8000,
						"Sprint must supplement Speed I, match Speed II and leave stronger Speed III intact without stacking or refreshing it");
			}
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 8000, 2));
			player.invulnerableTime = 0;
			player.setHealth(20);
			test.assertTrue(player.hurtServer(test.getLevel(), test.getLevel().damageSources().magic(), 10)
					&& Math.abs(player.getHealth() - 16) < 0.0001,
					"Existing Resistance III must continue to reduce magic normally, without any additional resistance from the form");
			player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
			FurryTransformation.tick(player);
			test.assertTrue(!FurryTransformation.hasProtection(player)
					&& player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(FurryTransformation.SPRINT_SPEED_MODIFIER_ID) == null
					&& player.getAttribute(Attributes.ARMOR).getModifier(FurryTransformation.ARMOR_MODIFIER_ID) == null
					&& player.getAttributeValue(Attributes.ARMOR) == ordinaryArmor + 8
					&& player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)
					&& Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - walking * 1.3 * 1.6) < 0.000001
					&& player.getEffect(MobEffects.SPEED).getAmplifier() == 2 && player.getEffect(MobEffects.SPEED).getDuration() == 8000
					&& player.getEffect(MobEffects.RESISTANCE).getAmplifier() == 2 && player.getEffect(MobEffects.RESISTANCE).getDuration() == 8000,
					"Expiry must remove only form-owned armor and speed, preserving the equipped chestplate and stronger existing potions unchanged");
			player.removeAllEffects();
			player.invulnerableTime = 0;
			player.setHealth(20);
			test.assertTrue(player.hurtServer(test.getLevel(), test.getLevel().damageSources().magic(), 10) && player.getHealth() == 10,
					"After form and potion removal genuine incoming damage must return to the ordinary amount");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void onlyLoveModeRegeneratesEveryTwentyFiveTicksAndEndingItRestoresNativeHealingWithoutEndingTheForm(GameTestHelper test) {
		var player = player(test, GameType.SURVIVAL);
		var control = player(test, GameType.SURVIVAL);
		try {
			player.setHealth(8);
			FurryTransformation.start(player);
			test.assertTrue(player.getHealth() == 8 && !player.hasEffect(MobEffects.REGENERATION),
					"Injection must not heal immediately or introduce a short vanilla regeneration effect");
			for (int tick = 1; tick <= 50; tick++) {
				player.tickCount = tick;
				FurryTransformation.tickRegeneration(player);
				test.assertTrue(player.getHealth() == 8,
						"The ordinary furry form must never heal by itself when breeding mode is inactive");
			}
			test.assertTrue(FurryBreeding.enterLove(player) && FurryTransformation.hasLoveRegeneration(player),
					"The regeneration fixture must explicitly enter breeding mode before receiving Regeneration II");
			for (int tick = 1; tick <= 50; tick++) {
				player.tickCount = tick;
				FurryTransformation.tickRegeneration(player);
				test.assertTrue(player.getHealth() == 8 + tick / 25,
						"Breeding-mode Regeneration II must restore exactly one health at ticks 25 and 50, never between them");
			}
			player.addEffect(new MobEffectInstance(ModEffects.HUNTERS_INSTINCT, 3600, 0));
			player.setHealth(8);
			ModEffects.HUNTERS_INSTINCT.value().applyEffectTick(test.getLevel(), player, 0);
			test.assertTrue(player.getHealth() == 8,
					"Hunter's weaker built-in Regeneration I must not add a second heal while the stronger furry regeneration is active");
			player.tickCount = 75;
			FurryTransformation.tickRegeneration(player);
			test.assertTrue(player.getHealth() == 9, "The stronger form must still provide its one normal heal while Hunter's Instinct is present");
			player.removeEffect(ModEffects.HUNTERS_INSTINCT);
			for (int amplifier = 0; amplifier <= 2; amplifier++) {
				player.removeEffect(MobEffects.REGENERATION);
				control.removeEffect(MobEffects.REGENERATION);
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 8000, amplifier));
				control.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 8000, Math.max(1, amplifier)));
				player.setHealth(8);
				control.setHealth(8);
				for (int tick = 1; tick <= 50; tick++) {
					player.tickCount = control.tickCount = tick;
					player.getEffect(MobEffects.REGENERATION).tickServer(test.getLevel(), player, () -> {});
					control.getEffect(MobEffects.REGENERATION).tickServer(test.getLevel(), control, () -> {});
					FurryTransformation.tickRegeneration(player);
					test.assertTrue(player.getHealth() == control.getHealth(),
							"Existing Regeneration I must heal at the form's level-II rate, while II/III retain their native rate without double healing");
				}
				test.assertTrue(player.getEffect(MobEffects.REGENERATION).getAmplifier() == amplifier
						&& player.getEffect(MobEffects.REGENERATION).getDuration() == 7950,
						"The form must not replace or refresh the saved duration or amplifier of any native regeneration potion, including level I");
			}
			player.removeEffect(MobEffects.REGENERATION);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 7925, 0));
			player.setHealth(8);
			player.setAttached(ModAttachments.FURRY_LOVE_UNTIL, now(test));
			FurryBreeding.tickPlayer(player);
			FurryTransformation.tick(player);
			test.assertTrue(FurryTransformation.isActive(player) && !FurryTransformation.hasLoveRegeneration(player)
					&& player.getAttributeValue(Attributes.ARMOR) == 4
					&& player.getEffect(MobEffects.REGENERATION).getDuration() == 7925,
					"Ending love must preserve the furry form, its four armor and the unchanged native potion duration");
			for (int tick = 1; tick <= 50; tick++) {
				player.tickCount = 100 + tick;
				FurryTransformation.tickRegeneration(player);
				player.getEffect(MobEffects.REGENERATION).tickServer(test.getLevel(), player, () -> {});
				test.assertTrue(player.getHealth() == (tick <= 25 ? 8 : 9),
						"After love ends, native Regeneration I must return to its fifty-tick interval, with no fallback heal from the still-active form");
			}
			test.assertTrue(player.getEffect(MobEffects.REGENERATION).getAmplifier() == 0
					&& player.getEffect(MobEffects.REGENERATION).getDuration() == 7875,
					"Returning to the native healing interval must preserve the original potion level and continue, not reset, its countdown");
			player.removeEffect(MobEffects.REGENERATION);
			player.setHealth(8);
			player.tickCount = 175;
			FurryTransformation.tickRegeneration(player);
			test.assertTrue(player.getHealth() == 8, "Ending love must immediately stop fallback healing even while the form remains active");
			player.addEffect(new MobEffectInstance(ModEffects.HUNTERS_INSTINCT, 3600, 0));
			ModEffects.HUNTERS_INSTINCT.value().applyEffectTick(test.getLevel(), player, 0);
			test.assertTrue(player.getHealth() == 9 && FurryTransformation.isActive(player),
					"Outside breeding mode the ordinary furry form must allow Hunter's Instinct to provide its own normal Regeneration I");
			player.removeEffect(ModEffects.HUNTERS_INSTINCT);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 7950, 2));
			test.assertTrue(FurryBreeding.enterLove(player), "The expiry case must start with an active breeding-mode regeneration bonus");
			player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
			player.tickCount = 75;
			float health = player.getHealth();
			FurryTransformation.tickRegeneration(player);
			FurryTransformation.tick(player);
			FurryBreeding.tickPlayer(player);
			test.assertTrue(player.getHealth() == health && !FurryTransformation.isActive(player)
					&& player.getEffect(MobEffects.REGENERATION).getAmplifier() == 2
					&& player.getEffect(MobEffects.REGENERATION).getDuration() == 7950,
					"The exact expiry tick must stop built-in healing without removing the player's unrelated regeneration potion");
			player.removeEffect(MobEffects.REGENERATION);
			player.setHealth(8);
			for (int tick = 100; tick <= 200; tick += 25) {
				player.tickCount = tick;
				FurryTransformation.tickRegeneration(player);
			}
			test.assertTrue(player.getHealth() == 8, "After expiry no hidden regeneration timer may keep healing the ordinary player");
			FurryTransformation.start(player);
			test.assertTrue(FurryBreeding.enterLove(player), "The death case must begin with active breeding-mode regeneration");
			player.setHealth(0);
			player.tickCount = 225;
			FurryTransformation.tickRegeneration(player);
			test.assertTrue(player.getHealth() == 0, "A dead transformed player must never be healed back to life by the form");
		} finally {
			removePlayer(test, player);
			removePlayer(test, control);
		}
		test.succeed();
	}

	@GameTest
	public void onlySuccessfulOrdinaryMeleeFromAnActiveFurryCanWinTheThreePercentRageRoll(GameTestHelper test) {
		var attacker = player(test, GameType.SURVIVAL);
		var victim = test.spawnWithNoFreeWill(EntityTypes.COW, new Vec3(4, 2, 4));
		var arrow = new Arrow(EntityTypes.ARROW, test.getLevel());
		arrow.setOwner(attacker);
		try {
			FurryTransformation.start(attacker);
			long winningSeed = rageSeed(true), losingSeed = rageSeed(false);
			test.assertTrue(FurryTransformation.RAGE_CHANCE == 0.03f,
					"The melee infection probability must remain exactly three percent");
			attacker.getRandom().setSeed(winningSeed);
			test.assertTrue(victim.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(attacker), 1)
					&& victim.hasEffect(ModEffects.GOSHAS_RAGE) && victim.getEffect(ModEffects.GOSHAS_RAGE).getDuration() == 2400
					&& victim.getEffect(ModEffects.GOSHAS_RAGE).getAmplifier() == 0,
					"A real successful ordinary melee hit with the winning native random roll must infect its victim for exactly two minutes");
			victim.removeEffect(ModEffects.GOSHAS_RAGE);
			victim.setHealth(victim.getMaxHealth());
			victim.invulnerableTime = 0;
			attacker.getRandom().setSeed(losingSeed);
			test.assertTrue(victim.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(attacker), 1)
					&& !victim.hasEffect(ModEffects.GOSHAS_RAGE),
					"A successful melee hit outside the three-percent interval must deal damage without infecting the victim");
			victim.invulnerableTime = 0;
			attacker.getRandom().setSeed(winningSeed);
			test.assertTrue(victim.hurtServer(test.getLevel(), test.getLevel().damageSources().arrow(arrow, attacker), 1)
					&& !victim.hasEffect(ModEffects.GOSHAS_RAGE),
					"An arrow owned by the same transformed player must never infect even when the next roll would win");
			victim.setInvulnerable(true);
			victim.invulnerableTime = 0;
			attacker.getRandom().setSeed(winningSeed);
			test.assertTrue(!victim.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(attacker), 1)
					&& !victim.hasEffect(ModEffects.GOSHAS_RAGE),
					"Rejected native damage must not trigger the infection callback");
			victim.setInvulnerable(false);
			FurryTransformation.onMeleeHit(victim, test.getLevel().damageSources().playerAttack(attacker), false);
			test.assertTrue(!victim.hasEffect(ModEffects.GOSHAS_RAGE)
					&& attacker.getRandom().nextFloat() == RandomSource.create(winningSeed).nextFloat(),
					"Explicit failed-hit callbacks must not consume the attacker's random roll or infect anything");
			attacker.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
			attacker.getRandom().setSeed(winningSeed);
			victim.invulnerableTime = 0;
			test.assertTrue(victim.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(attacker), 1)
					&& !victim.hasEffect(ModEffects.GOSHAS_RAGE),
					"The exact form deadline must stop melee infection before the normal end-of-tick cleanup");
		} finally {
			arrow.discard();
			victim.discard();
			removePlayer(test, attacker);
		}
		test.succeed();
	}

	private static long rageSeed(boolean winning) {
		var seeds = RandomSource.create(0x4655525259524147L);
		for (int attempt = 0; attempt < 10000; attempt++) {
			long seed = seeds.nextLong();
			if ((RandomSource.create(seed).nextFloat() < 0.03f) == winning) return seed;
		}
		throw new AssertionError("The fixed native random sequence must contain both a winning and a losing rage roll");
	}

	private static ServerPlayer player(GameTestHelper test, GameType gameType) {
		// GameTestHelper's anonymous ServerPlayer hardcodes gameMode() to CREATIVE,
		// so setGameMode alone cannot exercise the genuine spectator rejection path.
		var server = test.getLevel().getServer();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "furry-test"), false);
		var player = new ServerPlayer(server, test.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(gameType);
		test.assertTrue(player.gameMode() == gameType && player.isSpectator() == (gameType == GameType.SPECTATOR),
				"The player fixture must expose the actual requested game mode, including spectator state");
		player.setNoGravity(true);
		player.setOnGround(false);
		player.snapTo(test.absoluteVec(new Vec3(2, 2, 2)));
		player.getInventory().clearContent();
		return player;
	}

	private static long now(GameTestHelper test) {
		return test.getLevel().getServer().overworld().getGameTime();
	}

	private static void removePlayer(GameTestHelper test, ServerPlayer player) {
		test.getLevel().getServer().getPlayerList().remove(player);
	}
}
