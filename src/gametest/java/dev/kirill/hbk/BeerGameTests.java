package dev.kirill.hbk;

import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.block.BeerCauldronBlock;
import dev.kirill.hbk.effect.BeerEffect;
import dev.kirill.hbk.item.BeerBottleItem;
import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModBlocks;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BeerGameTests {
	@GameTest
	public void threeSipsLeaveAnEmptyBeerBottleAndRefreshOneIntoxicationEffect(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.getFoodData().setFoodLevel(20);
			double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			ItemStack bottle = new ItemStack(ModItems.BEER_BOTTLE);
			test.assertTrue(BeerBottleItem.MAX_SIPS == 3 && BeerBottleItem.getSips(bottle) == 3
					&& bottle.getMaxStackSize() == 16, "Fresh full beer must retain legacy stacking and have three sips");
			for (int sip = 1; sip <= 3; sip++) {
				bottle = bottle.finishUsingItem(test.getLevel(), player);
				var active = player.getEffect(ModEffects.BEER);
				test.assertTrue(player.getActiveEffects().size() == 1 && active != null
						&& active.getDuration() == 2400 && active.getAmplifier() == 0,
						"Every sip must refresh only one intoxication effect to exactly two minutes");
				test.assertTrue(bottle.getCount() == 1 && (sip < 3
						? bottle.is(ModItems.BEER_BOTTLE) && BeerBottleItem.getSips(bottle) == 3 - sip
								&& bottle.getMaxStackSize() == 1
						: bottle.is(ModItems.EMPTY_BEER_BOTTLE)),
						"The bottle must remain through the first two sips and become empty only after the third");
				for (int tick = 0; tick < 100; tick++) tickEffectsAndFood(player);
			}
			test.assertTrue(player.getFoodData().getFoodLevel() == 20 && !player.hasEffect(MobEffects.NAUSEA),
					"Beer can be drunk at full hunger without adding food or immediate continuous nausea");
			test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack + 3,
					"Strength I must add exactly three melee damage");
			test.assertFalse(player.getInventory().contains(item -> item.is(Items.GLASS_BOTTLE)),
					"Beer must not create a vanilla glass bottle between sips or after its last sip");
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void remainingSipsSurviveSavingAndNetworkRoundTrips(GameTestHelper test) {
		var bottle = new ItemStack(ModItems.BEER_BOTTLE);
		bottle.set(ModDataComponents.BEER_SIPS, 1);
		bottle.set(DataComponents.MAX_STACK_SIZE, 1);
		var ops = test.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var encoded = ItemStack.CODEC.encodeStart(ops, bottle).getOrThrow();
		var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
		test.assertTrue(restored.is(ModItems.BEER_BOTTLE) && BeerBottleItem.getSips(restored) == 1
				&& restored.getMaxStackSize() == 1,
				"Saving a started bottle must preserve its remaining sip rather than refill it");
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
		try {
			ItemStack.STREAM_CODEC.encode(buffer, restored);
			var received = ItemStack.STREAM_CODEC.decode(buffer);
			test.assertTrue(received.is(ModItems.BEER_BOTTLE) && BeerBottleItem.getSips(received) == 1
					&& received.getMaxStackSize() == 1,
					"The client must receive the same remaining sip count as the server");
		} finally {
			buffer.release();
		}
		bottle.remove(ModDataComponents.BEER_SIPS);
		test.assertTrue(BeerBottleItem.getSips(bottle) == 3,
				"A legacy beer without a sip component must start with three sips");
		test.succeed();
	}

	@GameTest
	public void oldBeerStacksSeparateTheStartedBottleWithoutLosingBeer(GameTestHelper test) {
		var shulker = new ItemStack(Items.SHULKER_BOX);
		shulker.set(DataComponents.CONTAINER,
				ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.BEER_BOTTLE, 16))));
		var ops = test.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var savedShulker = ItemStack.CODEC.encodeStart(ops, shulker).getOrThrow();
		var restoredShulker = ItemStack.CODEC.parse(ops, savedShulker).getOrThrow();
		var preservedBeer = restoredShulker.get(DataComponents.CONTAINER).nonEmptyItemCopyStream()
				.findFirst().orElseThrow();
		test.assertTrue(preservedBeer.is(ModItems.BEER_BOTTLE) && preservedBeer.getCount() == 16
				&& preservedBeer.getMaxStackSize() == 16 && BeerBottleItem.getSips(preservedBeer) == 3,
				"A shulker containing a legacy full stack of sixteen beer bottles must decode without item loss");
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setGameMode(GameType.SURVIVAL);
			var legacy = new ItemStack(ModItems.BEER_BOTTLE, 2);
			var remaining = legacy.finishUsingItem(test.getLevel(), player);
			test.assertTrue(remaining.is(ModItems.BEER_BOTTLE) && remaining.getCount() == 1
					&& BeerBottleItem.getSips(remaining) == 3,
					"An old stack must keep its untouched full bottle in the hand");
			int started = 0;
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				var stack = player.getInventory().getItem(slot);
				if (stack.is(ModItems.BEER_BOTTLE) && BeerBottleItem.getSips(stack) == 2) started += stack.getCount();
			}
			test.assertTrue(started == 1,
					"A legacy stack must yield exactly one separate bottle with two remaining sips");
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void wheatMustBeAddedOneAtATimeToTheWaterBeforeAnEmptyBottleCanFill(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		BlockPos cauldron = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			var heldWheat = new ItemStack(Items.WHEAT, 4);
			player.setItemInHand(InteractionHand.MAIN_HAND, heldWheat);
			player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 64));
			test.getLevel().setBlockAndUpdate(cauldron,
					Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
			test.assertTrue(clickCauldron(player, InteractionHand.MAIN_HAND, cauldron).consumesAction()
					&& heldWheat.getCount() == 3 && player.getInventory().getItem(1).getCount() == 64,
					"A real cauldron right-click must use one held wheat, not grain from other inventory slots");
			var mixture = test.getLevel().getBlockState(cauldron);
			test.assertTrue(mixture.is(ModBlocks.BEER_CAULDRON) && mixture.getValue(BeerCauldronBlock.WHEAT) == 1
					&& mixture.getValue(LayeredCauldronBlock.LEVEL) == 3 && !BeerCauldronBlock.isReady(mixture),
					"One wheat in a full three-layer cauldron must remain unfinished wort");
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
			test.assertFalse(clickCauldron(player, InteractionHand.OFF_HAND, cauldron).consumesAction(),
					"A bottle must not collect beer until every water layer has its wheat");
			test.assertTrue(player.getOffhandItem().is(ModItems.EMPTY_BEER_BOTTLE)
					&& test.getLevel().getBlockState(cauldron).equals(mixture) && heldWheat.getCount() == 3,
					"An unfinished refill must not change either the bottle, water, or held wheat");
			clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
			clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
			test.assertTrue(heldWheat.getCount() == 1 && BeerCauldronBlock.isReady(test.getLevel().getBlockState(cauldron)),
					"Exactly three held wheat clicks must finish a full cauldron");
			test.assertFalse(clickCauldron(player, InteractionHand.MAIN_HAND, cauldron).consumesAction(),
					"Already-finished beer must reject unnecessary extra grain");
			test.assertTrue(heldWheat.getCount() == 1 && player.getInventory().getItem(1).getCount() == 64,
					"Rejected extra grain must not be consumed from the hand or inventory");
			test.assertTrue(clickCauldron(player, InteractionHand.OFF_HAND, cauldron).consumesAction()
					&& player.getOffhandItem().is(ModItems.BEER_BOTTLE)
					&& BeerBottleItem.getSips(player.getOffhandItem()) == 3
					&& player.getOffhandItem().getMaxStackSize() == 16
					&& test.getLevel().getBlockState(cauldron).is(Blocks.CAULDRON),
					"An empty offhand bottle must collect three ready layers and leave an ordinary empty cauldron");
		} finally {
			test.getLevel().removeBlock(cauldron, false);
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void inventoryWheatPureWaterWrongCauldronsAndBrokenBottlesCannotRefill(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		BlockPos cauldron = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
			player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 64));
			test.getLevel().setBlockAndUpdate(cauldron,
					Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
			player.getMainHandItem().useOn(cauldronContext(player, InteractionHand.MAIN_HAND, cauldron));
			test.assertTrue(player.getMainHandItem().is(ModItems.EMPTY_BEER_BOTTLE) && countWheat(player) == 64
					&& test.getLevel().getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL) == 3,
					"Pure water must not automatically pull wheat from the inventory or refill beer");
			for (var block : List.of(Blocks.CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON)) {
				test.getLevel().setBlockAndUpdate(cauldron, block.defaultBlockState());
				player.getMainHandItem().useOn(cauldronContext(player, InteractionHand.MAIN_HAND, cauldron));
				test.assertTrue(player.getMainHandItem().is(ModItems.EMPTY_BEER_BOTTLE) && countWheat(player) == 64
						&& test.getLevel().getBlockState(cauldron).is(block),
						"Only a water cauldron may refill beer; wrong cauldrons must remain unchanged");
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BROKEN_BEER_BOTTLE));
			test.getLevel().setBlockAndUpdate(cauldron, readyBeer(3));
			player.getMainHandItem().useOn(cauldronContext(player, InteractionHand.MAIN_HAND, cauldron));
			test.assertTrue(player.getMainHandItem().is(ModItems.BROKEN_BEER_BOTTLE) && countWheat(player) == 64
					&& test.getLevel().getBlockState(cauldron).equals(readyBeer(3)),
					"A bottle without a bottom must never refill or spend resources");
		} finally {
			test.getLevel().removeBlock(cauldron, false);
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void startedBottlesTakeOnlyMissingSipsAndLeaveTheOtherReadyLayers(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		BlockPos cauldron = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			var started = new ItemStack(ModItems.BEER_BOTTLE);
			started.set(ModDataComponents.BEER_SIPS, 1);
			started.set(DataComponents.MAX_STACK_SIZE, 1);
			player.setItemInHand(InteractionHand.MAIN_HAND, started);
			test.getLevel().setBlockAndUpdate(cauldron, readyBeer(3));
			test.assertTrue(player.getMainHandItem().useOn(cauldronContext(player, InteractionHand.MAIN_HAND, cauldron))
					.consumesAction() && BeerBottleItem.getSips(player.getMainHandItem()) == 3
					&& player.getMainHandItem().getMaxStackSize() == 16
					&& test.getLevel().getBlockState(cauldron).equals(readyBeer(1)),
					"Topping up a one-sip bottle must use exactly two layers and restore full-bottle stacking");
			var remaining = test.getLevel().getBlockState(cauldron);
			test.assertFalse(clickCauldron(player, InteractionHand.MAIN_HAND, cauldron).consumesAction(),
					"A full bottle cannot take or destroy any remaining beer");
			test.assertTrue(test.getLevel().getBlockState(cauldron).equals(remaining),
					"The layer left after a top-up must stay ready for a different empty bottle");
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
			test.assertTrue(player.getOffhandItem().useOn(cauldronContext(player, InteractionHand.OFF_HAND, cauldron))
					.consumesAction() && player.getOffhandItem().is(ModItems.BEER_BOTTLE)
					&& BeerBottleItem.getSips(player.getOffhandItem()) == 1 && player.getOffhandItem().getMaxStackSize() == 1
					&& test.getLevel().getBlockState(cauldron).is(Blocks.CAULDRON),
					"The last ready layer must create a one-sip nonstacking bottle, not magically fill all three sips");
			test.assertTrue(player.getActiveEffects().isEmpty(), "Brewing and refilling must not apply intoxication before drinking");
		} finally {
			test.getLevel().removeBlock(cauldron, false);
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void oneOrTwoWaterLayersNeedOnlyTheirOwnWheatAndYieldThatManySips(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		BlockPos cauldron = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			for (int layers = 1; layers <= 2; layers++) {
				test.getLevel().setBlockAndUpdate(cauldron,
						Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, layers));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, layers));
				for (int grain = 0; grain < layers; grain++) clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
				test.assertTrue(player.getMainHandItem().isEmpty()
						&& test.getLevel().getBlockState(cauldron).equals(readyBeer(layers)),
						"Each water layer must require exactly one wheat from the held stack");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
				clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
				test.assertTrue(BeerBottleItem.getSips(player.getMainHandItem()) == layers
						&& player.getMainHandItem().getMaxStackSize() == 1
						&& test.getLevel().getBlockState(cauldron).is(Blocks.CAULDRON),
						"A partly filled cauldron must yield its actual one or two sips, never three");
			}
		} finally {
			test.getLevel().removeBlock(cauldron, false);
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void waterBottlesAndBucketsDiluteWithoutDuplicatingTheExistingWheat(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		BlockPos cauldron = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			test.getLevel().setBlockAndUpdate(cauldron, readyBeer(1));
			player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.WATER));
			test.assertTrue(clickCauldron(player, InteractionHand.MAIN_HAND, cauldron).consumesAction()
					&& player.getMainHandItem().is(Items.GLASS_BOTTLE), "Adding plain bottled water must return its empty glass bottle");
			var diluted = test.getLevel().getBlockState(cauldron);
			test.assertTrue(diluted.getValue(LayeredCauldronBlock.LEVEL) == 2 && diluted.getValue(BeerCauldronBlock.WHEAT) == 1
					&& !BeerCauldronBlock.isReady(diluted), "A water bottle must dilute one ready layer into two unfinished layers");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
			clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
			test.assertTrue(player.getMainHandItem().is(ModItems.EMPTY_BEER_BOTTLE)
					&& test.getLevel().getBlockState(cauldron).equals(diluted), "Diluted wort must again reject beer bottles");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
			clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			test.assertTrue(clickCauldron(player, InteractionHand.MAIN_HAND, cauldron).consumesAction()
					&& player.getMainHandItem().is(Items.BUCKET), "Adding a water bucket must keep native empty-bucket behavior");
			diluted = test.getLevel().getBlockState(cauldron);
			test.assertTrue(diluted.is(ModBlocks.BEER_CAULDRON) && diluted.getValue(LayeredCauldronBlock.LEVEL) == 3
					&& diluted.getValue(BeerCauldronBlock.WHEAT) == 2 && !BeerCauldronBlock.isReady(diluted),
					"A bucket must preserve two wheat but dilute the mixture to three water layers");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
			clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
			test.assertTrue(clickCauldron(player, InteractionHand.MAIN_HAND, cauldron).consumesAction()
					&& player.getMainHandItem().is(Items.WATER_BUCKET)
					&& test.getLevel().getBlockState(cauldron).is(Blocks.CAULDRON),
					"Taking the liquid with a normal bucket must empty the cauldron without creating a wheat or beer item");
			test.assertTrue(countWheat(player) == 0
					&& !player.getInventory().contains(stack -> stack.is(ModItems.BEER_BOTTLE)),
					"Bucket extraction must not duplicate the dissolved ingredients or create a free beer bottle");
		} finally {
			test.getLevel().removeBlock(cauldron, false);
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void beerMixtureBlockStatesRoundTripAndWrongIngredientsDoNothing(GameTestHelper test) {
		var level = test.getLevel();
		var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
		for (int layers = 1; layers <= 3; layers++) {
			for (int wheat = 1; wheat <= 3; wheat++) {
				var state = ModBlocks.BEER_CAULDRON.defaultBlockState()
						.setValue(LayeredCauldronBlock.LEVEL, layers).setValue(BeerCauldronBlock.WHEAT, wheat);
				var saved = BlockState.CODEC.encodeStart(ops, state).getOrThrow();
				var restored = BlockState.CODEC.parse(ops, saved).getOrThrow();
				test.assertTrue(restored.equals(state) && Block.stateById(Block.getId(state)).equals(state),
						"Every wheat/water state must survive save/load and the native block-state network ID");
			}
		}
		var player = test.makeMockServerPlayerInLevel();
		BlockPos cauldron = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			for (var block : List.of(Blocks.CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON)) {
				level.setBlockAndUpdate(cauldron, block.defaultBlockState());
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 3));
				clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
				test.assertTrue(player.getMainHandItem().getCount() == 3 && level.getBlockState(cauldron).is(block),
						"Wheat may be added only to water or beer, never an empty, lava or snow cauldron");
			}
			level.setBlockAndUpdate(cauldron, Blocks.WATER_CAULDRON.defaultBlockState());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 3));
			clickCauldron(player, InteractionHand.MAIN_HAND, cauldron);
			test.assertTrue(player.getMainHandItem().getCount() == 3 && level.getBlockState(cauldron).is(Blocks.WATER_CAULDRON),
					"Seeds and unrelated items must not count as wheat or replace ordinary cauldron water");
		} finally {
			level.removeBlock(cauldron, false);
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void bottleWeaponsDealEightThenFifteenDamagePoisonAndBreakAtFivePercent(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		var target = test.spawnWithNoFreeWill(EntityTypes.COW, 3, 2, 3);
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.setNoGravity(true);
			player.snapTo(test.absoluteVec(new Vec3(2, 2, 2)));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
			target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
			target.setHealth(40);
			chargeAttack(player);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 8) < 0.00001
					&& Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 1.6) < 0.00001,
					"An empty bottle must have eight damage and the ordinary sword's 1.6 attack speed");
			seedFirstRoll(player, 0.05f, 0.055f);
			player.attack(target);
			test.assertTrue(Math.abs(target.getHealth() - 32) < 0.001
					&& player.getMainHandItem().is(ModItems.EMPTY_BEER_BOTTLE) && !target.hasEffect(MobEffects.POISON),
					"A roll just above five percent must deal eight damage without breaking or poisoning");
			chargeAttack(player);
			target.invulnerableTime = 0;
			target.setHealth(40);
			seedFirstRoll(player, 0.045f, 0.05f);
			player.attack(target);
			test.assertTrue(Math.abs(target.getHealth() - 32) < 0.001
					&& player.getMainHandItem().is(ModItems.BROKEN_BEER_BOTTLE) && !target.hasEffect(MobEffects.POISON)
					&& player.getOffhandItem().is(Items.SHIELD),
					"A roll below five percent must break the bottom only after the intact eight-damage hit");
			chargeAttack(player);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 15) < 0.00001
					&& Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 1.6) < 0.00001,
					"The broken bottle must have fifteen damage and retain sword attack speed");
			target.invulnerableTime = 0;
			target.setHealth(40);
			player.attack(target);
			var poison = target.getEffect(MobEffects.POISON);
			test.assertTrue(Math.abs(target.getHealth() - 25) < 0.001 && poison != null
					&& poison.getDuration() == 200 && poison.getAmplifier() == 0
					&& player.getMainHandItem().isEmpty() && player.getOffhandItem().is(Items.SHIELD),
					"One broken-bottle hit must deal fifteen damage, apply ten seconds of Poison I and destroy only that bottle");
			player.doTick();
			test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1
					&& player.getAttributeValue(Attributes.ATTACK_SPEED) == 4,
					"Destruction must leave no weapon damage or speed modifiers behind");
		} finally {
			target.discard();
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void rejectedAttacksDoNotBreakBottlesOrApplyPoison(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		var target = test.spawnWithNoFreeWill(EntityTypes.COW, 3, 2, 3);
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.setNoGravity(true);
			player.snapTo(test.absoluteVec(new Vec3(2, 2, 2)));
			target.setInvulnerable(true);
			for (var bottle : List.of(ModItems.EMPTY_BEER_BOTTLE, ModItems.BROKEN_BEER_BOTTLE)) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottle));
				chargeAttack(player);
				seedFirstRoll(player, 0.045f, 0.05f);
				player.attack(target);
				test.assertTrue(player.getMainHandItem().is(bottle) && target.getHealth() == target.getMaxHealth()
						&& !target.hasEffect(MobEffects.POISON),
						"An unsuccessful attack must not roll bottle breakage, spend its last hit or poison the target");
			}
		} finally {
			target.discard();
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
			test.assertTrue(BeerEffect.getNauseaBlend(player.getEffect(ModEffects.BEER).getDuration(), 0) == 0,
					"Nausea must begin smoothly after twenty seconds");
			tickEffectsAndFood(player);
			test.assertTrue(BeerEffect.isNauseaActive(player.getEffect(ModEffects.BEER).getDuration())
					&& player.getActiveEffects().size() == 1 && !player.hasEffect(MobEffects.NAUSEA),
					"A nausea pulse must be part of intoxication, not add a second effect");
			for (int tick = 0; tick < 160; tick++) tickEffectsAndFood(player);
			test.assertFalse(BeerEffect.isNauseaActive(player.getEffect(ModEffects.BEER).getDuration()),
					"Nausea must stop between pulses");
			for (int tick = 561; tick < 800; tick++) tickEffectsAndFood(player);
			test.assertTrue(BeerEffect.getNauseaBlend(player.getEffect(ModEffects.BEER).getDuration(), 0) == 0,
					"The next pulse must fade in rather than run continuously");
			tickEffectsAndFood(player);
			test.assertTrue(BeerEffect.isNauseaActive(player.getEffect(ModEffects.BEER).getDuration()),
					"The second pulse must follow twenty seconds after the first");
			for (int tick = 801; tick < 2400; tick++) tickEffectsAndFood(player);
			test.assertTrue(player.getActiveEffects().isEmpty() && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack,
					"All beer effects and the strength modifier must end after exactly two minutes");
			test.assertTrue(player.getFoodData().getSaturationLevel() < 5,
					"Internal hunger must actually consume saturation, not only show an icon");
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
			test.assertTrue(BeerEffect.isNauseaActive(player.getEffect(ModEffects.BEER).getDuration()),
					"Milk test must begin during a nausea pulse");
			new ItemStack(Items.MILK_BUCKET).finishUsingItem(test.getLevel(), player);
			for (int tick = 0; tick < 800; tick++) tickEffectsAndFood(player);
			test.assertTrue(player.getActiveEffects().isEmpty(), "Milk must clear the beer timer and stop future nausea");
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void intoxicationHealsLikeRegenerationOneAndDrainsLikeHungerTwo(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		var control = test.makeMockServerPlayerInLevel();
		try {
			for (var subject : List.of(player, control)) {
				subject.setGameMode(GameType.SURVIVAL);
				subject.getFoodData().setFoodLevel(6);
				subject.getFoodData().setSaturation(5);
				subject.setHealth(10);
			}
			new ItemStack(ModItems.BEER_BOTTLE).finishUsingItem(test.getLevel(), player);
			control.addEffect(new MobEffectInstance(MobEffects.HUNGER, 2400, 1));
			control.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2400, 0));
			for (int tick = 0; tick < 1000; tick++) {
				tickEffectsAndFood(player);
				tickEffectsAndFood(control);
				test.assertTrue(player.getHealth() == control.getHealth()
						&& player.getFoodData().getSaturationLevel() == control.getFoodData().getSaturationLevel()
						&& player.getFoodData().getFoodLevel() == control.getFoodData().getFoodLevel(),
						"Intoxication must match Regeneration I and Hunger II on every tick");
				test.assertTrue(player.getActiveEffects().size() == 1,
						"Even during nausea the beer must remain a single status effect");
			}
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
			test.getLevel().getServer().getPlayerList().remove(control);
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

	private static UseOnContext cauldronContext(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		return new UseOnContext(player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static InteractionResult clickCauldron(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return player.level().getBlockState(pos).useItemOn(player.getItemInHand(hand), player.level(), player, hand, hit);
	}

	private static BlockState readyBeer(int layers) {
		return ModBlocks.BEER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, layers)
				.setValue(BeerCauldronBlock.WHEAT, layers);
	}

	private static int countWheat(ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			if (stack.is(Items.WHEAT)) count += stack.getCount();
		}
		return count;
	}

	private static void chargeAttack(ServerPlayer player) {
		for (int tick = 0; tick < 20; tick++) player.doTick();
		player.setOnGround(true);
		player.setSprinting(false);
		player.setDeltaMovement(Vec3.ZERO);
	}

	private static void seedFirstRoll(ServerPlayer player, float minimum, float maximum) {
		for (long seed = 0; seed < 100_000; seed++) {
			player.getRandom().setSeed(seed);
			float roll = player.getRandom().nextFloat();
			if (roll >= minimum && roll < maximum) {
				player.getRandom().setSeed(seed);
				return;
			}
		}
		throw new IllegalStateException("No deterministic bottle-break roll found");
	}
}
