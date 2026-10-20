package dev.kirill.hbk;

import com.mojang.authlib.GameProfile;
import dev.kirill.hbk.entity.PinkFurryWolfEntity;
import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.mechanic.FurryTransformation;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FurryBreedingGameTests {
	@GameTest
	public void plugRecipeAndVanillaStyleBreedingDurationsLoadExactly(GameTestHelper test) {
		var recipe = test.getLevel().getServer().getRecipeManager()
				.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("furry_plug"))).orElseThrow().value();
		var nugget = new ItemStack(Items.IRON_NUGGET);
		var shard = new ItemStack(Items.AMETHYST_SHARD);
		var input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, nugget, ItemStack.EMPTY,
				nugget, nugget, nugget, ItemStack.EMPTY, shard, ItemStack.EMPTY));
		test.assertTrue(matches(recipe, input, test.getLevel()) && recipeResult(recipe, input).is(ModItems.FURRY_PLUG)
				&& recipeResult(recipe, input).getCount() == 1,
				"The exact iron-nugget rows ' N '/'NNN'/' A ' with an amethyst shard must craft one plug");
		var wrong = CraftingInput.of(3, 3, input.items().stream()
				.map(stack -> stack.is(Items.IRON_NUGGET) ? new ItemStack(Items.IRON_INGOT) : stack).toList());
		test.assertFalse(matches(recipe, wrong, test.getLevel()), "Iron ingots must not replace the agreed iron nuggets");
		test.assertTrue(FurryBreeding.LOVE_TICKS == 1200 && FurryBreeding.BREED_COOLDOWN_TICKS == 6000
				&& FurryBreeding.BABY_AGE_TICKS == -24000 && FurryBreeding.COURTSHIP_TICKS == 60,
				"Love lasts sixty seconds, parents wait five minutes, babies grow for twenty minutes and contact takes sixty ticks");
		test.succeed();
	}

	@GameTest
	public void realEntityClicksFeedAdultNpcsAndOnlyTransformedPlayers(GameTestHelper test) {
		floor(test);
		var giver = player(test, GameType.SURVIVAL);
		var target = player(test, GameType.SURVIVAL);
		var furry = wolf(test, 3, 4);
		try {
			giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_PLUG, 4));
			test.assertTrue(giver.interactOn(furry, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
					&& furry.isInLove() && furry.getLoveTicks() == 1200 && giver.getMainHandItem().getCount() == 3,
					"A genuine NPC click must consume one held plug and enter sixty seconds of love");
			test.assertTrue(!giver.interactOn(furry, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
					&& giver.getMainHandItem().getCount() == 3,
					"An already-ready adult must reject another plug without consuming it");
			test.assertFalse(giver.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
					"An ordinary player must not become a breeding partner");
			FurryTransformation.start(target);
			test.assertTrue(giver.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
					&& FurryBreeding.isInLove(target) && giver.getMainHandItem().getCount() == 2,
					"Clicking a transformed player must use the actual item interaction and consume one plug");
			target.removeAttached(ModAttachments.FURRY_LOVE_UNTIL);
			giver.setGameMode(GameType.CREATIVE);
			giver.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.FURRY_PLUG, 2));
			test.assertTrue(giver.interactOn(target, InteractionHand.OFF_HAND, Vec3.ZERO).consumesAction()
					&& FurryBreeding.isInLove(target) && giver.getOffhandItem().getCount() == 2,
					"A creative offhand click must still activate the target without spending the held stack");
		} finally {
			cleanup(test, giver, target);
		}
		test.succeed();
	}

	@GameTest
	public void onlySuccessfulGroundPickupAfterTransformationConsumesOneAndTriggersLove(GameTestHelper test) {
		var player = player(test, GameType.SURVIVAL);
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_PLUG, 2));
			var beforeForm = ground(test, player, 3);
			beforeForm.playerTouch(player);
			test.assertTrue(beforeForm.isRemoved() && plugs(player) == 5 && !FurryBreeding.isInLove(player),
					"Before transformation a real pickup must collect the entire stack normally without enabling breeding");
			FurryTransformation.start(player);
			FurryBreeding.tickPlayer(player);
			test.assertTrue(!ModItems.FURRY_PLUG.use(test.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction()
					&& !FurryBreeding.isInLove(player) && plugs(player) == 5,
					"Pre-existing inventory and self-use must not activate love after a transformation");
			var afterForm = ground(test, player, 3);
			afterForm.playerTouch(player);
			test.assertTrue(afterForm.isRemoved() && FurryBreeding.isInLove(player) && plugs(player) == 7,
					"A real eligible ground pickup must spend exactly one of three plugs and collect the other two");
			var alreadyReady = ground(test, player, 3);
			alreadyReady.playerTouch(player);
			test.assertTrue(alreadyReady.isRemoved() && plugs(player) == 10,
					"While already in love, native pickup must collect all three without another automatic charge");
			player.removeAttached(ModAttachments.FURRY_LOVE_UNTIL);
			var single = ground(test, player, 1);
			single.playerTouch(player);
			test.assertTrue(single.isRemoved() && FurryBreeding.isInLove(player) && plugs(player) == 10,
					"Picking up a single eligible plug must remove the ground entity, enable love and leave no extra inventory item");
			player.removeAttached(ModAttachments.FURRY_LOVE_UNTIL);
			player.getInventory().clearContent();
			for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			var blocked = ground(test, player, 3);
			blocked.playerTouch(player);
			test.assertTrue(!blocked.isRemoved() && blocked.getItem().getCount() == 3 && !FurryBreeding.isInLove(player),
					"A full inventory must leave the ground stack untouched and must not activate love before a permitted pickup");
		} finally {
			cleanup(test, player);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 180)
	public void twoNpcGoalsNavigateToContactAndProduceOnlyOnePersistentBaby(GameTestHelper test) {
		floor(test);
		var giver = player(test, GameType.SURVIVAL);
		var first = wolf(test, 1.5, 4);
		var second = wolf(test, 6, 4);
		feed(test, giver, first);
		feed(test, giver, second);
		double initialDistance = first.distanceToSqr(second);
		test.assertTrue(initialDistance > 9 && babies(test).isEmpty(), "The parents must start outside breeding contact");
		test.runAfterDelay(20, () -> {
			try {
				test.assertTrue(first.distanceToSqr(second) < initialDistance && babies(test).isEmpty(),
						"Native mating goals must find and approach the partner without producing offspring before sixty contact ticks");
			} catch (RuntimeException | Error error) {
				cleanup(test, giver);
				throw error;
			}
		});
		test.runAfterDelay(140, () -> {
			try {
				var children = babies(test);
				test.assertTrue(children.size() == 1 && wolves(test).size() == 3
						&& first.getAge() > 0 && first.getAge() <= 6000 && second.getAge() > 0 && second.getAge() <= 6000
						&& !first.isInLove() && !second.isInLove(),
						"Both simultaneously running parent goals must commit exactly one child and put both adults on cooldown");
				var child = children.getFirst();
				test.assertTrue(child.isPersistenceRequired() && child.getAge() < -23000
						&& child.getFurColor().getId() >= 0 && child.getFurColor().getId() < 16
						&& child.getBbWidth() < first.getBbWidth() && child.getBbHeight() < first.getBbHeight(),
						"The offspring must be a persistent native twenty-minute baby with a smaller hitbox and one of sixteen colours");
				test.succeed();
			} finally {
				cleanup(test, giver);
			}
		});
	}

	@GameTest(maxTicks = 150)
	public void anAdultNpcAndClickedFurryPlayerProduceABabyNpcAndSavePlayerCooldown(GameTestHelper test) {
		floor(test);
		var giver = player(test, GameType.SURVIVAL);
		var partner = player(test, GameType.SURVIVAL);
		partner.snapTo(test.absoluteVec(new Vec3(5, 2, 4)));
		FurryTransformation.start(partner);
		var furry = wolf(test, 2, 4);
		feed(test, giver, furry);
		giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_PLUG));
		test.assertTrue(giver.interactOn(partner, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
				"The furry player must actually receive the targeted plug interaction");
		partner.getRandom().setSeed(coinSeed(false));
		test.runAfterDelay(110, () -> {
			try {
				test.assertTrue(babies(test).size() == 1 && wolves(test).size() == 2
						&& furry.getAge() > 0 && !FurryBreeding.isInLove(partner),
						"The native NPC goal must mate with the ready player, yielding one baby NPC rather than a child or replacement player");
				var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id("torn_ace"));
				test.assertTrue(advancement != null && shperma(test).isEmpty()
						&& !partner.getAdvancements().getOrStartProgress(advancement).isDone()
						&& !giver.getAdvancements().getOrStartProgress(advancement).isDone(),
						"The seeded ordinary-baby outcome must not create burst items or award its exclusive advancement to either player");
				long deadline = partner.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L);
				test.assertTrue(deadline > now(test) && deadline <= now(test) + 6000 && !FurryBreeding.canEnterLove(partner),
						"The player parent must enter the same five-minute breeding cooldown as an NPC parent");
				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
				partner.saveWithoutId(saved);
				partner.removeAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL);
				partner.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), saved.buildResult()));
				test.assertTrue(partner.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L) == deadline
						&& !FurryBreeding.canEnterLove(partner), "Native player loading must not bypass or refresh the saved breeding cooldown");
				var child = babies(test).getFirst();
				test.assertTrue(furry.getUUID().equals(child.getFirstParentId())
						&& partner.getUUID().equals(child.getSecondParentId()) && partner.getUUID().equals(child.getPlayerParentId()),
						"A real NPC/player birth must remember both parents and designate that exact player as the family-follow target");
				partner.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
				FurryTransformation.tick(partner);
				var childSave = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
				child.saveWithoutId(childSave);
				var restored = ModEntityTypes.PINK_FURRY_WOLF.create(test.getLevel(), EntitySpawnReason.LOAD);
				try {
					restored.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), childSave.buildResult()));
					test.assertTrue(!FurryTransformation.isActive(partner) && child.getFollowParent() == partner
							&& restored.getFollowParent() == partner && partner.getUUID().equals(restored.getPlayerParentId())
							&& furry.getUUID().equals(restored.getFirstParentId()) && partner.getUUID().equals(restored.getSecondParentId()),
							"Native child loading and the end of the player's form must preserve loyalty to the actual player parent, not the NPC");
					partner.snapTo(test.absoluteVec(new Vec3(50, 2, 4)));
					test.assertTrue(child.getFollowParent() == null && restored.getFollowParent() == null,
							"With its player parent out of range, a mixed-family child must wait rather than follow its NPC parent instead");
				} finally {
					if (restored != null) restored.discard();
				}
				test.succeed();
			} finally {
				cleanup(test, giver, partner);
			}
		});
	}

	@GameTest
	public void babiesCannotEnterLoveUntilNativeGrowthAndAgeColourAndCooldownSurviveSaving(GameTestHelper test) {
		floor(test);
		var giver = player(test, GameType.SURVIVAL);
		var furry = wolf(test, 3, 4);
		var restored = ModEntityTypes.PINK_FURRY_WOLF.create(test.getLevel(), EntitySpawnReason.LOAD);
		try {
			furry.setAge(FurryBreeding.BABY_AGE_TICKS);
			giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_PLUG, 2));
			test.assertTrue(furry.isBaby() && !FurryBreeding.canEnterLove(furry)
					&& !giver.interactOn(furry, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
					&& giver.getMainHandItem().getCount() == 2 && !furry.isInLove(),
					"A native baby must reject breeding interaction without spending the plug");
			for (int age : new int[] {-12345, 6000, 0}) {
				furry.setAge(age);
				furry.setFurColor(DyeColor.CYAN);
				furry.setLoveTicks(age == 0 ? 777 : 0);
				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
				furry.saveWithoutId(saved);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), saved.buildResult()));
				test.assertTrue(restored.getAge() == age && restored.isBaby() == (age < 0) && restored.getFurColor() == DyeColor.CYAN
						&& restored.getLoveTicks() == (age == 0 ? 777 : 0),
						"Native entity saving must retain baby growth, adult breeding cooldown, fur colour and valid remaining love");
				if (age != 0) test.assertFalse(FurryBreeding.canEnterLove(restored), "Neither a saved baby nor a saved cooldown may breed early");
			}
			furry.resetLove();
			furry.setAge(-1);
			furry.aiStep();
			test.assertTrue(furry.getAge() == 0 && !furry.isBaby() && FurryBreeding.canEnterLove(furry),
					"The last native growth tick must turn the baby into an eligible adult");
			test.assertTrue(giver.interactOn(furry, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction() && furry.isInLove(),
					"The same breeding item must become usable once native growth completes");
		} finally {
			if (restored != null) restored.discard();
			cleanup(test, giver);
		}
		test.succeed();
	}

	@GameTest
	public void loveEndsAtItsDeadlineOnDeathAndWhenThePlayerTransformationEnds(GameTestHelper test) {
		floor(test);
		var player = player(test, GameType.SURVIVAL);
		var furry = wolf(test, 3, 4);
		try {
			FurryTransformation.start(player);
			FurryBreeding.enterLove(player);
			test.assertTrue(player.getAttachedOrElse(ModAttachments.FURRY_LOVE_UNTIL, 0L) == now(test) + 1200,
					"Player love must last exactly sixty seconds in shared overworld time");
			player.setAttached(ModAttachments.FURRY_LOVE_UNTIL, now(test) + 1);
			FurryBreeding.tickPlayer(player);
			test.assertTrue(FurryBreeding.isInLove(player), "The final tick before the love deadline must still be valid");
			player.setAttached(ModAttachments.FURRY_LOVE_UNTIL, now(test));
			FurryBreeding.tickPlayer(player);
			test.assertTrue(!FurryBreeding.isInLove(player) && !player.hasAttached(ModAttachments.FURRY_LOVE_UNTIL),
					"The exact love deadline must clear readiness without an extra tick");
			FurryBreeding.enterLove(player);
			player.setHealth(0);
			FurryBreeding.tickPlayer(player);
			test.assertFalse(player.hasAttached(ModAttachments.FURRY_LOVE_UNTIL), "Death must remove the player love attachment");
			player.setHealth(20);
			FurryTransformation.start(player);
			FurryBreeding.enterLove(player);
			player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
			FurryTransformation.tick(player);
			FurryBreeding.tickPlayer(player);
			test.assertTrue(!FurryTransformation.isActive(player) && !FurryBreeding.isInLove(player)
					&& !player.hasAttached(ModAttachments.FURRY_LOVE_UNTIL),
					"Ending the appearance must also end its breeding readiness");
			furry.setLoveTicks(1);
			furry.tick();
			test.assertTrue(!furry.isInLove() && furry.getLoveTicks() == 0, "An NPC's final love tick must expire normally");
			FurryBreeding.enterLove(furry);
			furry.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 1000);
			test.assertTrue(!furry.isAlive() && furry.getLoveTicks() == 0, "Ordinary NPC death must clear its remaining love");
		} finally {
			cleanup(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void contactAndSafeSpawnAreRequiredAndReverseParentCommitCannotDuplicateTheBaby(GameTestHelper test) {
		floor(test);
		var observer = player(test, GameType.SURVIVAL);
		var first = wolf(test, 1.5, 4);
		var second = wolf(test, 6, 4);
		try {
			FurryBreeding.enterLove(first);
			FurryBreeding.enterLove(second);
			test.assertTrue(!FurryBreeding.tryBreed(first, second) && first.isInLove() && second.isInLove() && babies(test).isEmpty(),
					"A discovered partner beyond three blocks must not produce offspring or spend either parent's readiness");
			first.snapTo(test.absoluteVec(new Vec3(2, 2, 4)));
			second.snapTo(test.absoluteVec(new Vec3(4.5, 2, 4)));
			test.setBlock(3, 2, 4, Blocks.STONE);
			test.assertTrue(!FurryBreeding.tryBreed(first, second) && first.isInLove() && second.isInLove()
					&& first.getAge() == 0 && second.getAge() == 0 && babies(test).isEmpty(),
					"A blocked birth position must be retryable without an orphan, lost love or prematurely applied cooldown");
			test.setBlock(3, 2, 4, Blocks.AIR);
			first.setFurColor(DyeColor.WHITE);
			second.setFurColor(DyeColor.WHITE);
			long seed = 46;
			int expected = RandomSource.create(seed).nextInt(16);
			first.getRandom().setSeed(seed);
			test.assertTrue(FurryBreeding.tryBreed(first, second), "Removing the obstruction must permit the same ready pair to breed");
			var child = babies(test).getFirst();
			test.assertTrue(babies(test).size() == 1 && child.getAge() == -24000 && child.isPersistenceRequired()
					&& child.getFurColor().getId() == expected && expected != DyeColor.WHITE.getId()
					&& first.getAge() == 6000 && second.getAge() == 6000 && !first.isInLove() && !second.isInLove(),
					"One native uniform nextInt(16), not parental inheritance, must determine the persistent newborn while both adults receive exact cooldowns");
			test.assertTrue(!FurryBreeding.tryBreed(second, first) && babies(test).size() == 1,
					"The other parent's goal must not be able to commit the same pair a second time in the same tick");
			var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id("torn_ace"));
			test.assertTrue(advancement != null && shperma(test).isEmpty()
					&& !observer.getAdvancements().getOrStartProgress(advancement).isDone(),
					"NPC/NPC breeding must still always produce a baby, never burst items or grant a nearby observer the player-only advancement");
		} finally {
			cleanup(test, observer);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 60)
	public void savedNpcFamiliesNavigateToTheirRealParentsInsteadOfCloserUnrelatedAdults(GameTestHelper test) {
		floor(test);
		var first = wolf(test, 2, 4);
		var second = wolf(test, 4, 4);
		FurryBreeding.enterLove(first);
		FurryBreeding.enterLove(second);
		test.assertTrue(FurryBreeding.tryBreed(first, second), "The family fixture must originate from a successful real birth");
		var original = babies(test).getFirst();
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
		original.saveWithoutId(output);
		original.discard();
		var child = ModEntityTypes.PINK_FURRY_WOLF.create(test.getLevel(), EntitySpawnReason.LOAD);
		child.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), output.buildResult()));
		child.snapTo(test.absoluteVec(new Vec3(1.5, 2, 4)));
		test.assertTrue(test.getLevel().addFreshEntity(child), "The saved child must return as a genuine ticking world entity");
		first.snapTo(test.absoluteVec(new Vec3(6, 2, 4)));
		second.snapTo(test.absoluteVec(new Vec3(6, 2, 6)));
		var stranger = wolf(test, 1.5, 2.5);
		for (var adult : List.of(first, second, stranger)) adult.setNoAi(true);
		double initialDistance = child.distanceToSqr(first);
		test.assertTrue(first.getUUID().equals(child.getFirstParentId()) && second.getUUID().equals(child.getSecondParentId())
				&& child.getPlayerParentId() == null && child.getFollowParent() == first
				&& child.distanceToSqr(stranger) < initialDistance,
				"Saving must retain both NPC parent UUIDs, and a closer unrelated adult must never be adopted");
		test.runAfterDelay(20, () -> {
			try {
				test.assertTrue(child.getFollowParent() == first && child.distanceToSqr(first) < initialDistance
						&& first.blockPosition().equals(child.getNavigation().getTargetPos()),
						"The native family goal must actually navigate toward the saved biological parent, not merely expose its UUID");
				first.discard();
				test.assertTrue(child.getFollowParent() == second, "An NPC-family baby must fall back to its other living real parent");
				second.discard();
				test.assertTrue(child.getFollowParent() == null, "An orphan must not follow an unrelated adult");
				child.setAge(0);
				test.assertTrue(child.getFollowParent() == null, "Adult children must stop using the baby family-follow behaviour");
				test.succeed();
			} finally {
				cleanup(test);
			}
		});
	}

	@GameTest
	public void lovePullUsesTheNearestAdultAtOneBlockPerSecondWithoutChangingVerticalMovement(GameTestHelper test) {
		floor(test);
		var player = player(test, GameType.SURVIVAL);
		var peer = player(test, GameType.SURVIVAL);
		var adult = wolf(test, 6, 2);
		var baby = wolf(test, 2.5, 2);
		baby.setAge(-24000);
		try {
			peer.snapTo(test.absoluteVec(new Vec3(2, 2, 5)));
			FurryTransformation.start(player);
			FurryTransformation.start(peer);
			test.assertTrue(FurryBreeding.enterLove(player) && !FurryBreeding.isInLove(peer)
					&& !FurryBreeding.isAttractionTarget(player, baby) && FurryBreeding.findAttractionTarget(player) == peer,
					"Love must choose the closest adult furry player even without reciprocal love and ignore the much closer baby");
			peer.snapTo(test.absoluteVec(new Vec3(6, 2, 6)));
			var pull = FurryBreeding.constrainLoveMovement(player, Vec3.ZERO);
			test.assertTrue(FurryBreeding.findAttractionTarget(player) == adult
					&& Math.abs(pull.x - 0.05) < 1.0E-10 && pull.y == 0 && Math.abs(pull.z) < 1.0E-10
					&& Math.abs(pull.length() * 20 - 1) < 1.0E-10,
					"Changing nearest adult must retarget the pull immediately, with exactly one horizontal block per twenty ticks at rest");
			var requested = new Vec3(-0.2, 0.7, 0.4);
			var attracted = FurryBreeding.constrainLoveMovement(player, requested);
			test.assertTrue(Math.abs(attracted.x - (-0.15)) < 1.0E-10 && attracted.y == requested.y
					&& Math.abs(attracted.z - requested.z) < 1.0E-10 && attracted.x < 0,
					"The pull must add only 0.05 horizontal movement while preserving all player input, including walking away and the exact jump/fall Y movement");
			player.snapTo(test.absoluteVec(new Vec3(4.5, 2, 2)));
			test.assertTrue(FurryBreeding.constrainLoveMovement(player, requested).equals(requested),
					"At the one-and-a-half-block contact distance the pull must stop without suppressing ordinary movement");
			player.snapTo(test.absoluteVec(new Vec3(2, 2, 2)));
			player.setAttached(ModAttachments.FURRY_LOVE_UNTIL, now(test));
			test.assertTrue(FurryBreeding.constrainLoveMovement(player, requested).equals(requested),
					"At the exact love deadline the pull must end even before attachment cleanup runs");
			FurryBreeding.tickPlayer(player);
			FurryBreeding.enterLove(player);
			player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now(test));
			test.assertTrue(FurryBreeding.constrainLoveMovement(player, requested).equals(requested),
					"The exact transformation deadline must also end the pull without an extra movement tick");
		} finally {
			cleanup(test, player, peer);
		}
		test.succeed();
	}

	@GameTest
	public void mixedPairsUseThePlayersNativeFairCoinWithoutChangingNpcOffspringColourOrAllowingASecondOutcome(GameTestHelper test) {
		floor(test);
		var player = player(test, GameType.SURVIVAL);
		var furry = wolf(test, 2, 4);
		player.snapTo(test.absoluteVec(new Vec3(4.5, 2, 4)));
		var seeds = RandomSource.create(0x4655525259434F49L);
		int bursts = 0, births = 0;
		try {
			var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id("torn_ace"));
			test.assertTrue(advancement != null && advancement.value().criteria().containsKey("burst"),
					"The burst advancement and its explicit burst criterion must actually load");
			for (int sample = 0; sample < 32; sample++) {
				babies(test).forEach(PinkFurryWolfEntity::discard);
				shperma(test).forEach(ItemEntity::discard);
				for (var criterion : advancement.value().criteria().keySet()) player.getAdvancements().revoke(advancement, criterion);
				furry.setAge(0);
				player.removeAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL);
				FurryTransformation.start(player);
				test.assertTrue(FurryBreeding.enterLove(furry) && FurryBreeding.enterLove(player),
						"Every deterministic sample must begin with a fresh eligible mixed pair");
				boolean burst = sample % 2 == 0;
				long seed;
				do seed = seeds.nextLong(); while (RandomSource.create(seed).nextBoolean() != burst);
				var expected = RandomSource.create(seed);
				test.assertTrue(expected.nextBoolean() == burst, "The balanced samples must use the real native nextBoolean decision");
				player.getRandom().setSeed(seed);
				long colourSeed = 46L + sample;
				int colour = RandomSource.create(colourSeed).nextInt(16);
				furry.getRandom().setSeed(colourSeed);
				test.assertTrue(FurryBreeding.tryBreed(furry, player), "Every unobstructed ready mixed pair must commit exactly one outcome");
				test.assertTrue(player.getRandom().nextBoolean() == expected.nextBoolean(),
						"The decision must consume exactly one native player coin flip, not a weighted roll or the NPC colour RNG");
				if (burst) {
					bursts++;
					test.assertTrue(babies(test).isEmpty() && shperma(test).size() == 12
							&& player.getAdvancements().getOrStartProgress(advancement).isDone(),
							"Native coin true must produce only twelve item drops and award the participating player the burst advancement");
				} else {
					births++;
					var child = babies(test).getFirst();
					test.assertTrue(babies(test).size() == 1 && shperma(test).isEmpty()
							&& child.getFurColor().getId() == colour && child.getAge() == -24000 && child.isPersistenceRequired()
							&& furry.getUUID().equals(child.getFirstParentId()) && player.getUUID().equals(child.getSecondParentId())
							&& player.getUUID().equals(child.getPlayerParentId()) && child.getFollowParent() == player
							&& !player.getAdvancements().getOrStartProgress(advancement).isDone(),
							"Native coin false must preserve ordinary baby age, NPC colour RNG and real parent-follow identity without the burst advancement");
				}
				test.assertTrue(furry.getAge() == 6000 && !furry.isInLove() && !FurryBreeding.isInLove(player)
						&& player.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L) == now(test) + 6000
						&& !FurryTransformation.hasLoveRegeneration(player) && !FurryBreeding.tryBreed(furry, player),
						"Both coin outcomes must end love and its regeneration, apply exact cooldowns and reject another commit");
			}
			test.assertTrue(bursts == 16 && births == 16,
					"The fixed balanced native-coin cases must exercise both sides of the fifty-fifty branch equally without a statistical tolerance");
		} finally {
			cleanup(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void burstRequiresASafeMixedPairAndDropsTwelveRadialItemsAwardingOnlyItsOwnPlayer(GameTestHelper test) {
		floor(test);
		var player = player(test, GameType.SURVIVAL);
		var observer = player(test, GameType.SURVIVAL);
		var furry = wolf(test, 2, 4);
		player.snapTo(test.absoluteVec(new Vec3(4.5, 2, 4)));
		FurryTransformation.start(player);
		FurryTransformation.start(observer);
		FurryBreeding.enterLove(furry);
		FurryBreeding.enterLove(player);
		FurryBreeding.enterLove(observer);
		try {
			var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id("torn_ace"));
			test.assertTrue(advancement != null && FurryBreeding.BURST_ITEM_COUNT == 12,
					"The loaded burst advancement must accompany the agreed twelve-item burst");
			long seed = coinSeed(true);
			player.getRandom().setSeed(seed);
			test.setBlock(3, 2, 4, Blocks.STONE);
			test.assertTrue(!FurryBreeding.tryBreed(furry, player) && furry.getAge() == 0 && furry.isInLove()
					&& FurryBreeding.isInLove(player) && babies(test).isEmpty() && shperma(test).isEmpty()
					&& !player.hasAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL)
					&& !player.getAdvancements().getOrStartProgress(advancement).isDone()
					&& player.getRandom().nextBoolean() == RandomSource.create(seed).nextBoolean(),
					"An obstructed mixed pair must fail before the outcome roll without lost readiness, cooldown, drops or an advancement");
			test.setBlock(3, 2, 4, Blocks.AIR);
			player.getRandom().setSeed(seed);
			int bullets = test.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET).size();
			test.assertTrue(FurryBreeding.tryBreed(furry, player), "The same pair must be able to retry safely after the obstruction is removed");
			var items = shperma(test);
			test.assertTrue(babies(test).isEmpty() && items.size() == 12
					&& items.stream().allMatch(item -> item.getItem().getCount() == 1
							&& item.getDeltaMovement().y > 0 && item.getDeltaMovement().horizontalDistanceSqr() > 0)
					&& test.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET).size() == bullets,
					"A burst must create twelve separate upward-moving collectible item entities, never a baby or a damaging bullet");
			var angles = items.stream().map(item -> Math.atan2(item.getDeltaMovement().z, item.getDeltaMovement().x)).sorted().toList();
			for (int direction = 0; direction < angles.size(); direction++) {
				double next = direction + 1 < angles.size() ? angles.get(direction + 1) : angles.getFirst() + 2 * Math.PI;
				test.assertTrue(Math.abs(next - angles.get(direction) - 2 * Math.PI / 12) < 0.000001,
						"The twelve item velocities must cover the complete horizontal circle with distinct evenly spaced directions");
			}
			test.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone()
					&& !observer.getAdvancements().getOrStartProgress(advancement).isDone()
					&& FurryBreeding.isInLove(observer) && FurryTransformation.hasLoveRegeneration(observer),
					"Only the mixed pair's participating player may receive the burst advancement; a nearby ready furry player must stay untouched");
			test.assertTrue(!furry.isInLove() && furry.getAge() == 6000 && !FurryBreeding.isInLove(player)
					&& !player.hasAttached(ModAttachments.FURRY_LOVE_UNTIL) && !FurryTransformation.hasLoveRegeneration(player)
					&& player.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L) == now(test) + 6000
					&& !FurryBreeding.enterLove(player) && !FurryBreeding.enterLove(furry)
					&& !FurryBreeding.tryBreed(furry, player) && shperma(test).size() == 12 && babies(test).isEmpty(),
					"A completed burst must clear only its parents' love/regeneration, charge both cooldowns and be impossible to duplicate immediately");
		} finally {
			cleanup(test, player, observer);
		}
		test.succeed();
	}

	@GameTest
	public void actualSummonEggUseAndNativeEggOffspringRemainPersistentAfterSavingAndDistanceDespawnChecks(GameTestHelper test) {
		floor(test);
		var player = player(test, GameType.SURVIVAL);
		var tracked = new ArrayList<PinkFurryWolfEntity>();
		try {
			var level = test.getLevel();
			var server = level.getServer();
			var source = server.createCommandSourceStack().withLevel(level)
					.withPosition(test.absoluteVec(new Vec3(3.5, 2, 3.5))).withSuppressedOutput();
			server.getCommands().performPrefixedCommand(source, "summon hbk:furry_wolf ~ ~ ~");
			test.assertTrue(wolves(test).size() == 1, "The real summon command must create one furry wolf in the fixture");
			var summoned = wolves(test).getFirst();
			tracked.add(summoned);
			var support = test.absolutePos(new BlockPos(5, 1, 4));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PINK_FURRY_WOLF_SPAWN_EGG));
			var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
			test.assertTrue(ModItems.PINK_FURRY_WOLF_SPAWN_EGG.useOn(context).consumesAction()
					&& player.getMainHandItem().isEmpty() && wolves(test).size() == 2,
					"A real survival egg use on the floor must consume its egg and create one additional adult");
			var fromEgg = wolves(test).stream().filter(furry -> furry != summoned).findFirst().orElseThrow();
			tracked.add(fromEgg);
			var childEgg = new ItemStack(ModItems.PINK_FURRY_WOLF_SPAWN_EGG);
			var child = (PinkFurryWolfEntity) SpawnEggItem.spawnOffspringFromSpawnEgg(player, summoned,
					ModEntityTypes.PINK_FURRY_WOLF, level, test.absoluteVec(new Vec3(5.5, 2, 5.5)), childEgg).orElseThrow();
			tracked.add(child);
			test.assertTrue(childEgg.isEmpty() && child.isBaby() && child.getAge() == -24000,
					"The native egg-on-adult path must create a genuine twenty-minute baby without relying on finalizeSpawn");
			var originals = List.of(summoned, fromEgg, child);
			for (int index = 0; index < originals.size(); index++) {
				var original = originals.get(index);
				test.assertTrue(original.isPersistenceRequired(),
						"Command adults, egg adults and native egg offspring must all receive persistence through production spawning, not the test helper");
				var color = List.of(DyeColor.CYAN, DyeColor.MAGENTA, DyeColor.BLUE).get(index);
				original.setFurColor(color);
				int age = original.getAge();
				var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				original.saveWithoutId(output);
				original.discard();
				var restored = ModEntityTypes.PINK_FURRY_WOLF.create(level, EntitySpawnReason.LOAD);
				tracked.add(restored);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				test.assertTrue(restored.isPersistenceRequired() && restored.getFurColor() == color && restored.getAge() == age,
						"Native saving and loading must retain manual-spawn persistence, fur colour and baby/adult age");
				// Stay in the same loaded X/Z chunk while isolating distance from parallel fixtures' players.
				restored.snapTo(restored.getX(), Math.min(restored.getY() + 256, level.getMaxY() - 4), restored.getZ(), 0, 0);
				restored.setNoGravity(true);
				test.assertTrue(level.addFreshEntity(restored), "The restored manual furry must return as a genuine world entity");
				var nearest = level.getNearestPlayer(restored, -1);
				test.assertTrue(nearest != null && restored.distanceToSqr(nearest) > 128 * 128,
						"The actual nearest player, including players in other test fixtures, must be beyond the immediate despawn radius");
				restored.checkDespawn();
				test.assertFalse(restored.isRemoved(), "A manually spawned furry must survive vanilla distance despawn even after save/load");
			}
		} finally {
			tracked.forEach(PinkFurryWolfEntity::discard);
			cleanup(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void naturalAndChunkGenerationFurriesKeepTheirOriginalDistanceDespawnBehaviourAfterSaving(GameTestHelper test) {
		floor(test);
		var player = player(test, GameType.SURVIVAL);
		var tracked = new ArrayList<PinkFurryWolfEntity>();
		try {
			var level = test.getLevel();
			for (var reason : List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION)) {
				var original = ModEntityTypes.PINK_FURRY_WOLF.spawn(level, test.absolutePos(new BlockPos(4, 2, 4)), reason);
				tracked.add(original);
				test.assertTrue(original != null && !original.isPersistenceRequired() && !original.isBaby(),
						"Real EntityType spawning for both natural reasons must still create nonpersistent adults");
				original.setFurColor(DyeColor.LIME);
				var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				original.saveWithoutId(output);
				original.discard();
				var restored = ModEntityTypes.PINK_FURRY_WOLF.create(level, EntitySpawnReason.LOAD);
				tracked.add(restored);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				test.assertTrue(!restored.isPersistenceRequired() && restored.getFurColor() == DyeColor.LIME && !restored.isBaby(),
						"Loading must not silently migrate naturally spawned furries into persistent manual-spawn pets");
				restored.snapTo(restored.getX(), Math.min(restored.getY() + 256, level.getMaxY() - 4), restored.getZ(), 0, 0);
				restored.setNoGravity(true);
				test.assertTrue(level.addFreshEntity(restored), "The natural save must be restored into the same already-loaded X/Z chunk");
				var nearest = level.getNearestPlayer(restored, -1);
				test.assertTrue(nearest != null && restored.distanceToSqr(nearest) > 128 * 128,
						"The natural control must use the same actual greater-than-128-block nearest-player condition as manual spawns");
				restored.checkDespawn();
				test.assertTrue(restored.isRemoved(), "Natural and chunk-generation furries must still be removed by native immediate distance despawn");
			}
		} finally {
			tracked.stream().filter(java.util.Objects::nonNull).forEach(PinkFurryWolfEntity::discard);
			cleanup(test, player);
		}
		test.succeed();
	}

	private static long coinSeed(boolean burst) {
		var seeds = RandomSource.create(0x4655525259425552L);
		for (int sample = 0; sample < 10000; sample++) {
			long seed = seeds.nextLong();
			if (RandomSource.create(seed).nextBoolean() == burst) return seed;
		}
		throw new AssertionError("The fixed native random sequence must contain both baby and burst coin outcomes");
	}

	private static List<ItemEntity> shperma(GameTestHelper test) {
		return test.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).stream()
				.filter(entity -> entity.getItem().is(ModItems.SHPERMA)).toList();
	}

	private static void floor(GameTestHelper test) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
			test.setBlock(x, 1, z, Blocks.STONE);
			if (x == 0 || x == 7 || z == 0 || z == 7) {
				for (int y = 2; y <= 4; y++) test.setBlock(x, y, z, Blocks.GLASS);
			}
		}
	}

	private static PinkFurryWolfEntity wolf(GameTestHelper test, double x, double z) {
		var furry = test.spawn(ModEntityTypes.PINK_FURRY_WOLF, new Vec3(x, 2, z));
		furry.setAge(0);
		furry.setPersistenceRequired();
		return furry;
	}

	private static void feed(GameTestHelper test, ServerPlayer giver, PinkFurryWolfEntity target) {
		giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_PLUG));
		test.assertTrue(giver.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction() && target.isInLove(),
				"The fixture must put an adult NPC in love through the real targeted item interaction");
	}

	private static ItemEntity ground(GameTestHelper test, ServerPlayer player, int count) {
		var item = new ItemEntity(test.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(ModItems.FURRY_PLUG, count));
		item.setNoPickUpDelay();
		test.getLevel().addFreshEntity(item);
		return item;
	}

	private static List<PinkFurryWolfEntity> wolves(GameTestHelper test) {
		return test.getEntities(ModEntityTypes.PINK_FURRY_WOLF);
	}

	private static List<PinkFurryWolfEntity> babies(GameTestHelper test) {
		return wolves(test).stream().filter(PinkFurryWolfEntity::isBaby).toList();
	}

	private static int plugs(ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			if (stack.is(ModItems.FURRY_PLUG)) count += stack.getCount();
		}
		return count;
	}

	private static ServerPlayer player(GameTestHelper test, GameType gameType) {
		var server = test.getLevel().getServer();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "furry-breed"), false);
		var player = new ServerPlayer(server, test.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(gameType);
		player.setNoGravity(true);
		player.setOnGround(false);
		player.snapTo(test.absoluteVec(new Vec3(2, 2, 2)));
		player.getInventory().clearContent();
		return player;
	}

	private static long now(GameTestHelper test) {
		return test.getLevel().getServer().overworld().getGameTime();
	}

	private static void cleanup(GameTestHelper test, ServerPlayer... players) {
		wolves(test).forEach(PinkFurryWolfEntity::discard);
		test.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).forEach(ItemEntity::discard);
		for (var player : players) test.getLevel().getServer().getPlayerList().remove(player);
	}

	@SuppressWarnings("unchecked")
	private static boolean matches(Recipe<?> recipe, CraftingInput input, ServerLevel level) {
		return ((Recipe<CraftingInput>) recipe).matches(input, level);
	}

	@SuppressWarnings("unchecked")
	private static ItemStack recipeResult(Recipe<?> recipe, CraftingInput input) {
		return ((Recipe<CraftingInput>) recipe).assemble(input);
	}
}
