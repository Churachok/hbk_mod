package dev.kirill.hbk;

import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.block.JagermeisterCauldronBlock;
import dev.kirill.hbk.block.entity.JagermeisterCauldronBlockEntity;
import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.item.JagermeisterBottleItem;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.registry.ModBlockEntities;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class JagermeisterGameTests {
	@GameTest
	public void drinkOfTheGodsChallengeRequiresObtainingAFullSevenSipBottle(GameTestHelper test) {
		var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id("drink_of_the_gods"));
		test.assertTrue(advancement != null, "Drink of the Gods must load from its advancement resource");
		var display = advancement.value().display().orElseThrow();
		test.assertTrue(advancement.value().parent().orElseThrow().equals(HbkMod.id("hentai_beginnings"))
				&& display.getType() == AdvancementType.CHALLENGE && display.getIcon().create().is(ModItems.JAGERMEISTER_BOTTLE)
				&& display.shouldShowToast() && display.shouldAnnounceChat(),
				"The challenge must use the HBK parent, Jagermeister icon, toast and chat announcement");
		test.assertTrue(display.getTitle().getContents() instanceof TranslatableContents title
				&& title.getKey().equals("advancement.hbk.drink_of_the_gods.title")
				&& display.getDescription().getContents() instanceof TranslatableContents description
				&& description.getKey().equals("advancement.hbk.drink_of_the_gods.description"),
				"The challenge title and description must use their localized translation keys");
		test.assertTrue(!advancement.value().criteria().isEmpty() && advancement.value().criteria().values().stream()
				.allMatch(criterion -> criterion.trigger() == CriteriaTriggers.INVENTORY_CHANGED),
				"The loaded challenge must be awarded by obtaining an item, not by drinking or a manual-only trigger");
		var player = player(test);
		try {
			test.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(),
					"A player without a full bottle must not begin with the challenge unlocked");
			for (int sips = 0; sips < JagermeisterBottleItem.MAX_SIPS; sips++) {
				var bottle = new ItemStack(sips == 0 ? ModItems.EMPTY_JAGERMEISTER_BOTTLE : ModItems.JAGERMEISTER_BOTTLE);
				if (sips > 0) {
					bottle.set(ModDataComponents.JAGERMEISTER_SIPS, sips);
					bottle.set(DataComponents.MAX_STACK_SIZE, 1);
				}
				player.getInventory().setItem(0, bottle);
				CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), bottle);
				test.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(),
						"An empty bottle or any partial bottle with one through six sips must not grant the challenge");
			}
			var defaultFull = new ItemStack(ModItems.JAGERMEISTER_BOTTLE, 16);
			var explicitFull = new ItemStack(ModItems.JAGERMEISTER_BOTTLE);
			explicitFull.set(ModDataComponents.JAGERMEISTER_SIPS, JagermeisterBottleItem.MAX_SIPS);
			for (var bottle : List.of(defaultFull, explicitFull)) {
				for (var criterion : advancement.value().criteria().keySet()) player.getAdvancements().revoke(advancement, criterion);
				player.getInventory().clearContent();
				player.getInventory().setItem(0, bottle);
				CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), bottle);
				test.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
						"Both default full stacks and explicitly saved seven-sip bottles must grant the challenge through inventory changes");
			}
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void stagRemainsRenderOnlyAndBottleTooltipOmitsTheDurationLine(GameTestHelper test) {
		test.assertFalse(BuiltInRegistries.ITEM.containsKey(HbkMod.id("jagermeister_stag")),
				"The rescue stag must be a render-only model, not a separately registered item");
		var keys = new ItemStack(ModItems.JAGERMEISTER_BOTTLE)
				.getTooltipLines(Item.TooltipContext.of(test.getLevel()), null, TooltipFlag.NORMAL).stream()
				.map(line -> line.getContents()).filter(TranslatableContents.class::isInstance)
				.map(TranslatableContents.class::cast).map(TranslatableContents::getKey).toList();
		test.assertTrue(keys.contains("item.hbk.jagermeister_bottle.sips")
				&& keys.contains("item.hbk.jagermeister_bottle.effect")
				&& !keys.contains("item.hbk.jagermeister_bottle.duration"),
				"The real bottle tooltip must retain sips and buffs but omit the removed sip/rescue-duration line");
		test.succeed();
	}

	@GameTest
	public void fixedRecipeExcludesRemovedFlowersButKeepsGoldenDandelionAndTheFiveAdditionalFlowers(GameTestHelper test) {
		var flowers = JagermeisterIngredients.items();
		var smallFlowers = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("small_flowers"));
		test.assertTrue(JagermeisterIngredients.TOTAL_INGREDIENTS == 20
				&& flowers.size() == JagermeisterIngredients.TOTAL_INGREDIENTS
				&& flowers.stream().distinct().count() == JagermeisterIngredients.TOTAL_INGREDIENTS
				&& JagermeisterIngredients.count(JagermeisterIngredients.ALL_MASK) == JagermeisterIngredients.TOTAL_INGREDIENTS,
				"The recipe must contain distinct flowers with one persistent bit per active ingredient");
		for (int index = 0; index < flowers.size(); index++) {
			test.assertTrue(JagermeisterIngredients.indexOf(new ItemStack(flowers.get(index))) == index,
					"Each recipe flower must map back to its logical recipe index");
			if (index < flowers.size() - 5) test.assertTrue(new ItemStack(flowers.get(index)).is(smallFlowers),
					"All ingredients before the five additional flowers must be Minecraft's small flowers");
		}
		test.assertTrue(flowers.containsAll(List.of(Items.GOLDEN_DANDELION, Items.OPEN_EYEBLOSSOM,
				Items.CACTUS_FLOWER, Items.SPORE_BLOSSOM,
				Items.CHORUS_FLOWER, Items.PINK_PETALS, Items.WILDFLOWERS)),
				"The golden dandelion, open eyeblossom and all five additional flowers are required");
		test.assertFalse(flowers.contains(Items.DANDELION) || flowers.contains(Items.CLOSED_EYEBLOSSOM),
				"Ordinary dandelion and closed eyeblossom must not appear in the recipe");
		test.assertTrue(JagermeisterIngredients.indexOf(ItemStack.EMPTY) == -1
				&& JagermeisterIngredients.indexOf(new ItemStack(Items.DANDELION)) == -1
				&& JagermeisterIngredients.indexOf(new ItemStack(Items.CLOSED_EYEBLOSSOM)) == -1
				&& JagermeisterIngredients.indexOf(new ItemStack(Items.SUNFLOWER)) == -1
				&& JagermeisterIngredients.indexOf(new ItemStack(Items.WHEAT)) == -1,
				"Removed flowers, tall flowers, wheat and an empty hand are not substitutes for the agreed recipe");
		int removedBits = 1 | (1 << 16);
		test.assertTrue((JagermeisterIngredients.ALL_MASK & removedBits) == 0
				&& JagermeisterIngredients.count(removedBits) == 0 && !JagermeisterIngredients.isComplete(removedBits),
				"The old dandelion and closed-eyeblossom bits must stay reserved and never count towards readiness");
		test.succeed();
	}

	@GameTest
	public void allUniqueFlowersProduceExactlyThreeFullBottlesFromThreeLayers(GameTestHelper test) {
		var player = player(test);
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			water(test, pos, 3);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE, 3));
			int mask = 0;
			for (int index = 0; index < JagermeisterIngredients.TOTAL_INGREDIENTS; index++) {
				var flower = new ItemStack(JagermeisterIngredients.items().get(index), 2);
				player.setItemInHand(InteractionHand.MAIN_HAND, flower);
				test.assertTrue(click(player, InteractionHand.MAIN_HAND, pos).consumesAction() && flower.getCount() == 1,
						"Adding a new flower must consume exactly one from the held stack");
				mask |= JagermeisterIngredients.ingredientBit(index);
				test.assertTrue(brew(test, pos).getIngredientsMask() == mask
						&& test.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 3,
						"Each accepted flower must update only its own bit without consuming water");
				test.assertFalse(click(player, InteractionHand.MAIN_HAND, pos).consumesAction(),
						"The same flower must not count twice");
				test.assertTrue(flower.getCount() == 1 && brew(test, pos).getIngredientsMask() == mask,
						"A duplicate must neither consume a flower nor modify the saved recipe");
				if (index < JagermeisterIngredients.TOTAL_INGREDIENTS - 1) {
					test.assertTrue(!test.getLevel().getBlockState(pos).getValue(JagermeisterCauldronBlock.READY)
							&& !click(player, InteractionHand.OFF_HAND, pos).consumesAction()
							&& player.getOffhandItem().getCount() == 3,
							"Missing even one flower must keep the brew unfinished and reject an empty bottle");
				}
			}
			test.assertTrue(test.getLevel().getBlockState(pos).getValue(JagermeisterCauldronBlock.READY),
					"Only the full active-flower mask may finish the batch");
			for (int bottle = 0; bottle < 3; bottle++) {
				test.assertTrue(click(player, InteractionHand.OFF_HAND, pos).consumesAction(),
						"Each empty bottle must take one finished water layer");
				if (bottle < 2) test.assertTrue(brew(test, pos).getIngredientsMask() == JagermeisterIngredients.ALL_MASK
						&& test.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 2 - bottle,
						"The remaining batch must keep its flowers and lose exactly one layer");
			}
			test.assertTrue(count(player, ModItems.JAGERMEISTER_BOTTLE) == 3
					&& count(player, ModItems.EMPTY_JAGERMEISTER_BOTTLE) == 0
					&& test.getLevel().getBlockState(pos).is(Blocks.CAULDRON) && test.getLevel().getBlockEntity(pos) == null,
					"Three layers must yield exactly three bottles and finally discard the spent flower batch");
			assertFullBottles(test, player);
		} finally {
			cleanup(test, player, pos);
		}
		test.succeed();
	}

	@GameTest
	public void flowerMasksSurviveSavingUpdateTagsAndBlockEntityPackets(GameTestHelper test) {
		var player = player(test);
		var level = test.getLevel();
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			water(test, pos, 1);
			int last = JagermeisterIngredients.TOTAL_INGREDIENTS - 1;
			for (int index : new int[] {0, 5, last}) add(player, pos, index);
			int expected = JagermeisterIngredients.ingredientBit(0)
					| JagermeisterIngredients.ingredientBit(5) | JagermeisterIngredients.ingredientBit(last);
			test.assertTrue(expected == ((1 << 1) | (1 << 6) | (1 << 21)),
					"The first, middle and final active ingredients must keep their original persistent bit positions");
			var original = brew(test, pos);
			var restored = BlockEntity.loadStatic(pos, level.getBlockState(pos),
					original.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
			test.assertTrue(restored instanceof JagermeisterCauldronBlockEntity loaded && loaded.getIngredientsMask() == expected,
					"Block-entity save/load must preserve the exact partial recipe, not just its flower count");
			var received = new JagermeisterCauldronBlockEntity(pos, level.getBlockState(pos));
			received.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
					original.getUpdateTag(level.registryAccess())));
			test.assertTrue(received.getIngredientsMask() == expected, "The initial chunk update tag must send the partial flower checklist");
			var packet = original.getUpdatePacket();
			received.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), packet.getTag()));
			test.assertTrue(packet.getPos().equals(pos) && packet.getType() == ModBlockEntities.JAGERMEISTER_CAULDRON
					&& received.getIngredientsMask() == expected,
					"Incremental block-entity packets must identify the correct cauldron and retain every ingredient bit");
			level.setBlockEntity(restored);
			for (int index = 0; index < JagermeisterIngredients.TOTAL_INGREDIENTS; index++) {
				if ((expected & JagermeisterIngredients.ingredientBit(index)) == 0) add(player, pos, index);
			}
			test.assertTrue(brew(test, pos).getIngredientsMask() == JagermeisterIngredients.ALL_MASK
					&& level.getBlockState(pos).getValue(JagermeisterCauldronBlock.READY),
					"A restored unfinished cauldron must finish normally after adding its missing flowers");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE));
			click(player, InteractionHand.MAIN_HAND, pos);
			test.assertTrue(JagermeisterBottleItem.getSips(player.getMainHandItem()) == 7 && level.getBlockState(pos).is(Blocks.CAULDRON),
					"The complete flower recipe must make a full seven-sip bottle even in just one water layer");
		} finally {
			cleanup(test, player, pos);
		}
		test.succeed();
	}

	@GameTest
	public void legacyMasksDiscardRemovedFlowersWithoutShiftingGoldenDandelionOrLaterFlowers(GameTestHelper test) {
		var player = player(test);
		var level = test.getLevel();
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			water(test, pos, 3);
			add(player, pos, 0);
			var saved = brew(test, pos).saveWithFullMetadata(level.registryAccess());
			// These are persisted positions from the original recipe, not new logical ingredient indices.
			int removedBits = 1 | (1 << 16);
			int legacyMask = removedBits | (1 << 1) | (1 << 17) | (1 << 18) | (1 << 20);
			saved.putInt("ingredients_mask", legacyMask);
			var restored = (JagermeisterCauldronBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos),
					saved, level.registryAccess());
			int goldenDandelionBit = JagermeisterIngredients.ingredientBit(JagermeisterIngredients.indexOf(new ItemStack(Items.GOLDEN_DANDELION)));
			int expected = goldenDandelionBit
					| JagermeisterIngredients.ingredientBit(JagermeisterIngredients.indexOf(new ItemStack(Items.CACTUS_FLOWER)))
					| JagermeisterIngredients.ingredientBit(JagermeisterIngredients.indexOf(new ItemStack(Items.SPORE_BLOSSOM)))
					| JagermeisterIngredients.ingredientBit(JagermeisterIngredients.indexOf(new ItemStack(Items.PINK_PETALS)));
			test.assertTrue(expected == ((1 << 1) | (1 << 17) | (1 << 18) | (1 << 20))
					&& restored.getIngredientsMask() == expected && JagermeisterIngredients.count(restored.getIngredientsMask()) == 4,
					"Legacy bits 1, 17, 18 and 20 must retain their flowers while removed bits 0 and 16 are ignored");
			level.setBlockEntity(restored);
			for (var duplicate : List.of(Items.GOLDEN_DANDELION, Items.CACTUS_FLOWER, Items.SPORE_BLOSSOM,
					Items.PINK_PETALS, Items.DANDELION, Items.CLOSED_EYEBLOSSOM)) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(duplicate, 2));
				test.assertTrue(!click(player, InteractionHand.MAIN_HAND, pos).consumesAction()
						&& player.getMainHandItem().getCount() == 2 && brew(test, pos).getIngredientsMask() == expected,
						"Loaded flowers must remain duplicates, and removed flowers must not consume or replace any active bit");
			}
			var unfinished = level.getBlockState(pos);
			// Both older recipe versions and a batch without either removed flower are now complete.
			for (int completeMask : new int[] {(1 << 22) - 1, JagermeisterIngredients.ALL_MASK | 1, JagermeisterIngredients.ALL_MASK}) {
				level.setBlockAndUpdate(pos, unfinished);
				saved.putInt("ingredients_mask", completeMask);
				var completed = (JagermeisterCauldronBlockEntity) BlockEntity.loadStatic(pos, unfinished,
						saved, level.registryAccess());
				level.setBlockEntity(completed);
				test.assertFalse(level.getBlockState(pos).getValue(JagermeisterCauldronBlock.READY),
						"The fixture must model an old unfinished block state with all twenty required flowers already saved");
				completed.reconcileReadyState();
				test.assertTrue(level.getBlockState(pos).getValue(JagermeisterCauldronBlock.READY)
						&& completed.getIngredientsMask() == JagermeisterIngredients.ALL_MASK,
						"Loading must finish every saved batch containing all twenty active flowers, regardless of removed bits");
			}
			level.setBlockAndUpdate(pos, unfinished);
			saved.putInt("ingredients_mask", (JagermeisterIngredients.ALL_MASK & ~goldenDandelionBit) | removedBits);
			var missingGolden = (JagermeisterCauldronBlockEntity) BlockEntity.loadStatic(pos, unfinished, saved, level.registryAccess());
			level.setBlockEntity(missingGolden);
			missingGolden.reconcileReadyState();
			test.assertTrue(!level.getBlockState(pos).getValue(JagermeisterCauldronBlock.READY)
					&& JagermeisterIngredients.count(missingGolden.getIngredientsMask()) == 19,
					"Old ordinary-dandelion and closed-eyeblossom bits must not substitute for the still-required golden dandelion");
			add(player, pos, JagermeisterIngredients.indexOf(new ItemStack(Items.GOLDEN_DANDELION)));
			test.assertTrue(level.getBlockState(pos).getValue(JagermeisterCauldronBlock.READY)
					&& brew(test, pos).getIngredientsMask() == JagermeisterIngredients.ALL_MASK,
					"Adding the golden dandelion must finish an otherwise complete nineteen-flower legacy batch");
		} finally {
			cleanup(test, player, pos);
		}
		test.succeed();
	}

	@GameTest
	public void unfinishedBrewAcceptsWaterButReadyBatchesAreSealedAgainstInfiniteRefills(GameTestHelper test) throws ReflectiveOperationException {
		var player = player(test);
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			water(test, pos, 1);
			add(player, pos, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.WATER));
			test.assertTrue(click(player, InteractionHand.MAIN_HAND, pos).consumesAction()
					&& test.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 2
					&& brew(test, pos).getIngredientsMask() == JagermeisterIngredients.ingredientBit(0), "Bottled water must preserve the unfinished recipe");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			test.assertTrue(click(player, InteractionHand.MAIN_HAND, pos).consumesAction()
					&& player.getMainHandItem().is(Items.BUCKET)
					&& test.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 3
					&& brew(test, pos).getIngredientsMask() == JagermeisterIngredients.ingredientBit(0), "A bucket must refill unfinished water without erasing its flowers");
			for (int index = 1; index < JagermeisterIngredients.TOTAL_INGREDIENTS; index++) add(player, pos, index);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE));
			click(player, InteractionHand.MAIN_HAND, pos);
			var sealed = test.getLevel().getBlockState(pos);
			for (var water : List.of(new ItemStack(Items.WATER_BUCKET), PotionContents.createItemStack(Items.POTION, Potions.WATER))) {
				player.setItemInHand(InteractionHand.MAIN_HAND, water);
				test.assertTrue(!click(player, InteractionHand.MAIN_HAND, pos).consumesAction()
						&& water.getCount() == 1 && test.getLevel().getBlockState(pos).equals(sealed),
						"A ready two-layer remainder must reject both bucket and bottled-water dilution");
			}
			var block = (JagermeisterCauldronBlock) ModBlocks.JAGERMEISTER_CAULDRON;
			for (int rain = 0; rain < 1000; rain++) block.handlePrecipitation(sealed, test.getLevel(), pos, Biome.Precipitation.RAIN);
			// Exercise the native protected drip hook without waiting for random pointed-dripstone ticks.
			var drip = JagermeisterCauldronBlock.class.getDeclaredMethod("receiveStalactiteDrip",
					BlockState.class, Level.class, BlockPos.class, Fluid.class);
			drip.setAccessible(true);
			drip.invoke(block, sealed, test.getLevel(), pos, Fluids.WATER);
			test.assertTrue(test.getLevel().getBlockState(pos).equals(sealed)
					&& brew(test, pos).getIngredientsMask() == JagermeisterIngredients.ALL_MASK,
					"Rain and stalactite drips must never replenish an already-finished batch");
		} finally {
			cleanup(test, player, pos);
		}
		test.succeed();
	}

	@GameTest
	public void wrongIngredientsLiquidsAndBottleTypesNeverCreateJagermeister(GameTestHelper test) {
		var player = player(test);
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			for (var state : List.of(Blocks.CAULDRON.defaultBlockState(), Blocks.LAVA_CAULDRON.defaultBlockState(),
					Blocks.POWDER_SNOW_CAULDRON.defaultBlockState(), ModBlocks.BEER_CAULDRON.defaultBlockState())) {
				test.getLevel().setBlockAndUpdate(pos, state);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_DANDELION, 2));
				click(player, InteractionHand.MAIN_HAND, pos);
				test.assertTrue(player.getMainHandItem().getCount() == 2 && test.getLevel().getBlockState(pos).equals(state),
						"Flowers must not convert an empty, lava, snow or beer cauldron into herbal brew");
			}
			water(test, pos, 3);
			for (var wrong : List.of(Items.WHEAT, Items.SUNFLOWER, Items.PEONY, Items.LILAC, Items.ROSE_BUSH,
					Items.DANDELION, Items.CLOSED_EYEBLOSSOM)) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wrong));
				if (wrong == Items.WHEAT) continue; // Wheat deliberately belongs to the independent beer recipe.
				click(player, InteractionHand.MAIN_HAND, pos);
				test.assertTrue(test.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON)
						&& player.getMainHandItem().getCount() == 1, "Unlisted flowers, including both removed ingredients, must not enter the active recipe");
			}
			brewAll(test, player, pos, 3);
			var ready = test.getLevel().getBlockState(pos);
			for (var bottle : List.of(ModItems.BEER_BOTTLE, ModItems.EMPTY_BEER_BOTTLE, ModItems.BROKEN_BEER_BOTTLE, ModItems.JAGERMEISTER_BOTTLE)) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottle));
				click(player, InteractionHand.MAIN_HAND, pos);
				test.assertTrue(test.getLevel().getBlockState(pos).equals(ready) && player.getMainHandItem().is(bottle),
						"Only the matching empty herbal bottle may collect Jagermeister; beer and full bottles must stay unchanged");
			}
		} finally {
			cleanup(test, player, pos);
		}
		test.succeed();
	}

	@GameTest
	public void sevenSipsExtendHuntersInstinctAndReturnAnOrdinaryStackableEmptyBottle(GameTestHelper test) {
		var player = player(test);
		try {
			player.setHealth(10);
			player.getFoodData().setFoodLevel(5);
			ItemStack bottle = new ItemStack(ModItems.JAGERMEISTER_BOTTLE);
			for (int left = 6; left >= 0; left--) {
				bottle = bottle.finishUsingItem(test.getLevel(), player);
				test.assertTrue(left == 0 ? bottle.is(ModItems.EMPTY_JAGERMEISTER_BOTTLE) && bottle.getMaxStackSize() == 16
						: bottle.is(ModItems.JAGERMEISTER_BOTTLE) && JagermeisterBottleItem.getSips(bottle) == left && bottle.getMaxStackSize() == 1,
						"Every drink use must spend one of seven sips, returning a normal stackable empty bottle only after the last");
				var instinct = player.getEffect(ModEffects.HUNTERS_INSTINCT);
				var nausea = player.getEffect(MobEffects.NAUSEA);
				test.assertTrue(player.getActiveEffects().size() == 2 && instinct != null && instinct.getAmplifier() == 0
						&& instinct.getDuration() == (7 - left) * HuntersInstinctEffect.DURATION_TICKS
						&& nausea != null && nausea.getDuration() == 8 * 20
						&& player.getHealth() == 10 && player.getFoodData().getFoodLevel() == 5,
						"Each sip must add three minutes of one level-I hunter effect and refresh eight seconds of nausea, not instant health or food");
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
			player.removeAllEffects();
			for (int tick = 0; tick < 20; tick++) player.doTick();
			test.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1
					&& player.getAttributeValue(Attributes.ATTACK_SPEED) == 4 && bottle.get(DataComponents.WEAPON) == null,
					"An empty herbal bottle must not inherit the beer bottle's weapon component or attributes");
			var fullStack = new ItemStack(ModItems.JAGERMEISTER_BOTTLE, 2);
			var rest = fullStack.finishUsingItem(test.getLevel(), player);
			test.assertTrue(rest.getCount() == 1 && JagermeisterBottleItem.getSips(rest) == 7
					&& player.getInventory().contains(stack -> stack.is(ModItems.JAGERMEISTER_BOTTLE) && JagermeisterBottleItem.getSips(stack) == 6),
					"Drinking from a stack must keep untouched full bottles and separate the one started bottle");
			var partial = new ItemStack(ModItems.JAGERMEISTER_BOTTLE);
			partial.set(ModDataComponents.JAGERMEISTER_SIPS, 2);
			partial.set(DataComponents.MAX_STACK_SIZE, 1);
			var ops = test.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
			var saved = ItemStack.CODEC.encodeStart(ops, partial).getOrThrow();
			var loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
			var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
			try {
				ItemStack.STREAM_CODEC.encode(buffer, loaded);
				var received = ItemStack.STREAM_CODEC.decode(buffer);
				test.assertTrue(JagermeisterBottleItem.getSips(received) == 2 && received.getMaxStackSize() == 1,
						"The remaining sip count and nonstacking state must survive saving and synchronization");
			} finally {
				buffer.release();
			}
		} finally {
			test.getLevel().getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void stackedAndCreativeEmptyBottlesUseVanillaSingleContainerExchange(GameTestHelper test) {
		var player = player(test);
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		try {
			brewAll(test, player, pos, 3);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE, 16));
			click(player, InteractionHand.OFF_HAND, pos);
			test.assertTrue(player.getOffhandItem().getCount() == 15 && player.getMainHandItem().is(Items.STICK)
					&& count(player, ModItems.JAGERMEISTER_BOTTLE) == 1,
					"A stacked offhand refill must consume only one empty bottle and leave the other hand untouched");
			player.getInventory().clearContent();
			for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE, 2));
			click(player, InteractionHand.OFF_HAND, pos);
			var dropped = test.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
					item -> item.getItem().is(ModItems.JAGERMEISTER_BOTTLE));
			test.assertTrue(player.getOffhandItem().getCount() == 1 && dropped.size() == 1
					&& dropped.getFirst().getItem().getCount() == 1 && JagermeisterBottleItem.getSips(dropped.getFirst().getItem()) == 7,
					"Full inventory must drop exactly one full bottle without losing the remaining empty stack");
			dropped.forEach(ItemEntity::discard);
			player.getInventory().clearContent();
			player.setGameMode(GameType.CREATIVE);
			brewAll(test, player, pos, 3);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE, 16));
			for (int bottle = 0; bottle < 3; bottle++) click(player, InteractionHand.OFF_HAND, pos);
			test.assertTrue(player.getOffhandItem().getCount() == 16 && count(player, ModItems.JAGERMEISTER_BOTTLE) == 1
					&& test.getLevel().getBlockState(pos).is(Blocks.CAULDRON),
					"Creative refill must retain the empty stack and use vanilla's duplicate-result protection");
			var full = new ItemStack(ModItems.JAGERMEISTER_BOTTLE, 2);
			test.assertTrue(full.finishUsingItem(test.getLevel(), player).getCount() == 2 && JagermeisterBottleItem.getSips(full) == 7
					&& player.getEffect(ModEffects.HUNTERS_INSTINCT).getDuration() == HuntersInstinctEffect.DURATION_TICKS
					&& player.getEffect(ModEffects.HUNTERS_INSTINCT).getAmplifier() == 0
					&& player.getEffect(MobEffects.NAUSEA).getDuration() == 8 * 20,
					"Creative drinking must apply the same effects without spending sips, splitting the full stack or creating an empty bottle");
		} finally {
			cleanup(test, player, pos);
		}
		test.succeed();
	}

	@GameTest
	public void emptyBottleCraftsFromTheSevenGreenGlassOutline(GameTestHelper test) {
		var manager = test.getLevel().getServer().getRecipeManager();
		var recipe = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("empty_jagermeister_bottle"))).orElseThrow().value();
		var glass = Items.STAINED_GLASS.green();
		var input = CraftingInput.of(3, 3, List.of(new ItemStack(glass), ItemStack.EMPTY, new ItemStack(glass),
				new ItemStack(glass), ItemStack.EMPTY, new ItemStack(glass), new ItemStack(glass), new ItemStack(glass), new ItemStack(glass)));
		test.assertTrue(matches(recipe, input, test.getLevel()), "The agreed seven-block green-glass outline must craft the empty bottle");
		var result = manager.getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow().value().assemble(input);
		test.assertTrue(result.is(ModItems.EMPTY_JAGERMEISTER_BOTTLE) && result.getCount() == 1,
				"The shaped recipe must produce exactly one matching empty herbal bottle");
		var plain = CraftingInput.of(3, 3, input.items().stream().map(stack -> stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(Items.GLASS)).toList());
		test.assertFalse(matches(recipe, plain, test.getLevel()), "Plain or differently colored glass must not replace the requested green glass");
		test.succeed();
	}

	@SuppressWarnings("unchecked")
	private static boolean matches(Recipe<?> recipe, CraftingInput input, net.minecraft.server.level.ServerLevel level) {
		return ((Recipe<CraftingInput>) recipe).matches(input, level);
	}

	private static ServerPlayer player(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(test.absoluteVec(new Vec3(2, 2, 3)));
		return player;
	}

	private static void water(GameTestHelper test, BlockPos pos, int layers) {
		test.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, layers));
	}

	private static InteractionResult click(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		return player.level().getBlockState(pos).useItemOn(player.getItemInHand(hand), player.level(), player, hand,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static JagermeisterCauldronBlockEntity brew(GameTestHelper test, BlockPos pos) {
		return (JagermeisterCauldronBlockEntity) test.getLevel().getBlockEntity(pos);
	}

	private static void add(ServerPlayer player, BlockPos pos, int index) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JagermeisterIngredients.items().get(index)));
		click(player, InteractionHand.MAIN_HAND, pos);
	}

	private static void brewAll(GameTestHelper test, ServerPlayer player, BlockPos pos, int layers) {
		water(test, pos, layers);
		for (int index = 0; index < JagermeisterIngredients.TOTAL_INGREDIENTS; index++) add(player, pos, index);
		test.assertTrue(JagermeisterIngredients.isComplete(brew(test, pos).getIngredientsMask())
				&& test.getLevel().getBlockState(pos).getValue(JagermeisterCauldronBlock.READY), "Fixture must create a fully finished flower batch");
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			if (stack.is(item)) count += stack.getCount();
		}
		return count;
	}

	private static void assertFullBottles(GameTestHelper test, ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			if (stack.is(ModItems.JAGERMEISTER_BOTTLE)) test.assertTrue(JagermeisterBottleItem.getSips(stack) == 7 && stack.getMaxStackSize() == 16,
					"Every collected bottle must be full, seven-sip and normally stackable");
		}
	}

	private static void cleanup(GameTestHelper test, ServerPlayer player, BlockPos pos) {
		test.getLevel().removeBlock(pos, false);
		test.getLevel().getServer().getPlayerList().remove(player);
	}
}
