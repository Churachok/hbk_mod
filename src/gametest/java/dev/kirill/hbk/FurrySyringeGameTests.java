package dev.kirill.hbk;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.mechanic.FurryTransformation;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
	public void transformationLeavesEveryPlayerAttributeHitboxAndUnrelatedItemUnchanged(GameTestHelper test) {
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
					"The cosmetic-invariance fixture must actually use a syringe");
			test.assertTrue(beforeValues.equals(attributes.stream().map(player::getAttributeValue).toList())
					&& player.getHealth() == 11 && player.getFoodData().getFoodLevel() == 7 && player.getFoodData().getSaturationLevel() == 3
					&& player.experienceLevel == 4 && player.totalExperience == 100 && player.experienceProgress == 0.25f
					&& player.getBoundingBox().equals(box) && player.getEyeHeight() == eye
					&& player.getBbWidth() == width && player.getBbHeight() == height
					&& player.getActiveEffects().size() == 1 && player.getEffect(MobEffects.SPEED).getDuration() == 300,
					"Only the visual attachment may change: every player attribute, health, hunger, XP, hitbox, eye height and existing effect must stay intact");
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
