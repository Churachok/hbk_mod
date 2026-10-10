package dev.kirill.hbk;

import dev.kirill.hbk.item.OnigiriItem;
import dev.kirill.hbk.item.FunnyButtonItem;
import dev.kirill.hbk.entity.FunnySpinAccess;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class KitchenGameTests {
	@GameTest
	public void dryDoshirakRestoresTwoHungerButCostsHalfAHeart(GameTestHelper test) {
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		try {
			player.getFoodData().setFoodLevel(10);
			var noodles = new ItemStack(ModItems.DENIS_DOSHIRAK, 2);
			noodles.finishUsingItem(test.getLevel(), player);
			test.assertTrue(noodles.getCount() == 1 && player.getFoodData().getFoodLevel() == 12,
					"Dry noodles must consume one portion and restore two hunger");
			test.assertTrue(player.getHealth() == 19, "Dry noodles must cost half a heart");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void noodleRecipesAndEveryButtonWork(GameTestHelper test) {
		var level = test.getLevel();
		var manager = level.getServer().getRecipeManager();
		var noodles = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("denis_doshirak"))).orElseThrow().value();
		test.assertTrue(matches(noodles, CraftingInput.of(2, 1,
				List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.DRIED_KELP))), level), "Dry noodles must be obtainable");
		var kettle = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("doshirak_kettle"))).orElseThrow().value();
		test.assertTrue(matches(kettle, CraftingInput.of(2, 1,
				List.of(new ItemStack(ModItems.KIRILL_KETTLE), new ItemStack(ModItems.DENIS_DOSHIRAK))), level),
				"Noodles and the empty kettle must craft shapelessly");
		var button = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("funny_button"))).orElseThrow().value();
		var buttons = net.minecraft.tags.TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.withDefaultNamespace("buttons"));
		for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(buttons)) {
			test.assertTrue(matches(button, CraftingInput.of(2, 1,
					List.of(new ItemStack(holder), new ItemStack(Items.DIAMOND))), level), "Recipe must accept every button: " + holder);
		}
		test.assertFalse(matches(button, CraftingInput.of(2, 1,
				List.of(new ItemStack(Items.LEVER), new ItemStack(Items.DIAMOND))), level), "A lever is not a button");
		test.succeed();
	}

	@GameTest
	public void noodleKettleCooksReturnsEmptyAndBuffsExactDamageAndSpeed(GameTestHelper test) {
		var level = test.getLevel();
		var pos = test.absolutePos(new BlockPos(2, 2, 2));
		level.setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
		var furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
		furnace.setItem(0, new ItemStack(ModItems.DOSHIRAK_KETTLE));
		furnace.setItem(1, new ItemStack(Items.COAL));
		for (int tick = 0; tick < 199; tick++) AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		test.assertTrue(furnace.getItem(2).isEmpty(), "Noodle kettle must take 200 ticks to cook");
		AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		test.assertTrue(furnace.getItem(2).is(ModItems.HARD_DOSHIRAK_KETTLE), "Furnace must cook the noodle kettle");
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		try {
			player.getFoodData().setFoodLevel(10);
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			var empty = furnace.getItem(2).copy().finishUsingItem(level, player);
			test.assertTrue(empty.is(ModItems.KIRILL_KETTLE), "Drinking must return the ordinary empty kettle");
			test.assertTrue(player.getFoodData().getFoodLevel() == 13, "Drink must restore 3 hunger");
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.4) < 0.00001,
					"Original tea speed plus extra 20% must total 40%");
			test.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration() == 600
					&& player.getEffect(ModEffects.DOSHIRAK).getDuration() == 600, "Effects must last 30 seconds");
			var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 1, 2, 1);
			cow.hurtServer(level, level.damageSources().playerAttack(player), 4);
			test.assertTrue(Math.abs(cow.getHealth() - 4) < 0.001, "4 base damage must become 6 (+50%)");
			var arrow = new Arrow(EntityTypes.ARROW, level);
			arrow.setOwner(player);
			var ranged = test.spawnWithNoFreeWill(EntityTypes.COW, 3, 2, 1);
			ranged.hurtServer(level, level.damageSources().arrow(arrow, player), 4);
			test.assertTrue(Math.abs(ranged.getHealth() - 4) < 0.001, "Noodles must also buff projectile damage by 50%");
			new ItemStack(ModItems.HARD_DOSHIRAK_KETTLE).finishUsingItem(level, player);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.4) < 0.00001,
					"Repeated drinks must refresh, not stack");
		} finally {
			player.discard();
			level.removeBlock(pos, false);
		}
		test.succeed();
	}

	@GameTest
	public void feedingOnigiriInterceptsMobInteractionsAndBuffsMeleeAndProjectiles(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		try {
			var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 2);
			var zombie = test.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 3, 2, 2);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ONIGIRI, 3));
			double speed = zombie.getAttributeValue(Attributes.MOVEMENT_SPEED);
			test.assertTrue(UseEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, cow, null).consumesAction(),
					"Feeding must intercept the mob's vanilla interaction");
			UseEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, zombie, null);
			test.assertTrue(cow.hasEffect(ModEffects.ONIGIRI) && zombie.hasEffect(ModEffects.ONIGIRI),
					"Food must work on passive and hostile mobs");
			test.assertTrue(player.getMainHandItem().getCount() == 1 && !player.hasEffect(ModEffects.ONIGIRI),
					"Feeding two mobs must consume two portions without buffing the player");
			test.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 2.5) < 0.00001,
					"Fed mobs must gain exactly 150% speed");
			cow.hurtServer(level, level.damageSources().mobAttack(zombie), 4);
			test.assertTrue(Math.abs(cow.getHealth() - 3.6f) < 0.001, "A fed mob must deal 60% extra melee damage");
			var ranged = test.spawnWithNoFreeWill(EntityTypes.COW, 1, 2, 1);
			var arrow = new Arrow(EntityTypes.ARROW, level);
			arrow.setOwner(zombie);
			ranged.hurtServer(level, level.damageSources().arrow(arrow, zombie), 4);
			test.assertTrue(Math.abs(ranged.getHealth() - 3.6f) < 0.001, "A fed mob must deal 60% extra ranged damage");
			zombie.removeEffect(ModEffects.ONIGIRI);
			test.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 0.00001, "Speed must restore on expiration");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void everyGoshaDropsOneToFourDandruffEvenWithoutPlayerKill(GameTestHelper test) {
		for (int trial = 0; trial < 32; trial++) {
			var gosha = test.spawn(ModEntityTypes.GOSHA, 2, 2, 2);
			gosha.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 1000);
			int count = 0;
			for (var item : test.getEntities(EntityTypes.ITEM)) {
				if (item.getItem().is(ModItems.GOSHAS_DANDRUFF)) count += item.getItem().getCount();
				item.discard();
			}
			test.assertTrue(count >= 1 && count <= 4, "Every Gosha must drop 1-4 dandruff, got " + count);
			gosha.discard();
		}
		test.succeed();
	}

	@GameTest(maxTicks = 40)
	public void throwingDandruffConsumesOneAndDealsFiveDamage(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var origin = test.absolutePos(new BlockPos(1, 2, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOSHAS_DANDRUFF, 2));
		ModItems.GOSHAS_DANDRUFF.use(level, player, InteractionHand.MAIN_HAND);
		test.assertTrue(player.getMainHandItem().getCount() == 1, "Throwing must consume exactly one flake");
		var projectile = test.getEntities(ModEntityTypes.GOSHAS_DANDRUFF).getFirst();
		var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 4, 2, 2);
		cow.setNoGravity(true);
		var direction = cow.position().add(0, cow.getBbHeight() * 0.5, 0).subtract(projectile.position());
		projectile.shoot(direction.x, direction.y, direction.z, 1.5f, 0);
		test.runAfterDelay(6, () -> {
			test.assertTrue(Math.abs(cow.getHealth() - 5) < 0.001, "A thrown flake must hit for 5 damage");
			test.assertTrue(projectile.isRemoved(), "Projectile must disappear after impact");
			player.discard();
			test.succeed();
		});
	}

	@GameTest(maxTicks = 130)
	public void funnyButtonSpinsMobsWithinSphereThenExplodesOnlyHostiles(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var center = Vec3.atCenterOf(test.absolutePos(new BlockPos(2, 2, 2))).add(0, 200, 0);
		player.setPos(center);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FUNNY_BUTTON));
		var peaceful = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 2);
		var hostile = test.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 2, 2, 2);
		var far = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 2);
		peaceful.setPos(center.add(-6, 0, 0));
		hostile.setPos(center.add(3, 0, 0));
		far.setPos(center.add(40, 0, 40)); // Inside the query box, outside the radius-50 sphere.
		for (var mob : List.of(peaceful, hostile, far)) mob.setNoGravity(true);
		test.assertFalse(FunnyButtonItem.isHostile(test.spawnWithNoFreeWill(ModEntityTypes.ANTON, 1, 2, 1)),
				"Peaceful Anton must not count as hostile merely because of his spawn category");
		ModItems.FUNNY_BUTTON.use(level, player, InteractionHand.MAIN_HAND);
		test.assertTrue(peaceful.hasEffect(ModEffects.FUNNY_SPIN) && hostile.hasEffect(ModEffects.FUNNY_SPIN), "All nearby mobs must spin");
		test.assertFalse(far.hasEffect(ModEffects.FUNNY_SPIN), "Mobs outside the sphere must not spin");
		test.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "Button must have a cooldown");
		test.runAfterDelay(3, () -> {
			test.assertTrue(((FunnySpinAccess) peaceful).hbk$isFunnySpinning(), "Spin must be synchronized for client observers");
			test.assertTrue(hostile.isAlive(), "Explosion must wait until animation finishes");
		});
		test.runAfterDelay(110, () -> {
			test.assertFalse(hostile.isAlive(), "Hostile mob must explode after five seconds");
			test.assertTrue(peaceful.isAlive() && far.isAlive(), "Peaceful and out-of-range mobs must survive");
			test.assertFalse(((FunnySpinAccess) peaceful).hbk$isFunnySpinning(), "Peaceful mob must stop spinning afterward");
			for (var mob : List.of(peaceful, hostile, far)) mob.discard();
			player.discard();
			test.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void newCraftingAndSmeltingRecipesUnlockInVanillaBook(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		player.getInventory().add(new ItemStack(Items.WHEAT));
		player.getInventory().add(new ItemStack(Items.OAK_BUTTON));
		player.getInventory().add(new ItemStack(ModItems.KIRILL_KETTLE));
		player.getInventory().add(new ItemStack(ModItems.DOSHIRAK_KETTLE));
		player.getInventory().tick();
		test.runAfterDelay(2, () -> {
			for (String id : List.of("denis_doshirak", "doshirak_kettle", "hard_doshirak_kettle", "funny_button")) {
				test.assertTrue(player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, HbkMod.id(id))),
						"Collecting an ingredient must unlock new recipe: " + id);
			}
			player.getInventory().clearContent();
			test.succeed();
		});
	}

	@GameTest
	public void onigiriAcceptsEveryFishAndKettleCraftsFromFurnaceAndBottle(GameTestHelper test) {
		var level = test.getLevel();
		var manager = level.getServer().getRecipeManager();
		var rice = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("onigiri"))).orElseThrow().value();
		for (var fish : List.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH,
				Items.COOKED_COD, Items.COOKED_SALMON)) {
			var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BREAD), new ItemStack(fish)));
			test.assertTrue(matches(rice, input, level), "Onigiri recipe must accept fish: " + fish);
		}
		test.assertFalse(matches(rice, CraftingInput.of(2, 1,
				List.of(new ItemStack(Items.BREAD), new ItemStack(Items.BEEF))), level),
				"Meat must not replace fish in onigiri");
		var kettle = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id("kirill_kettle"))).orElseThrow().value();
		test.assertTrue(matches(kettle, CraftingInput.of(2, 1,
				List.of(new ItemStack(Items.FURNACE), new ItemStack(Items.GLASS_BOTTLE))), level),
				"Kettle must craft from a furnace and a glass bottle");
		test.succeed();
	}

	@SuppressWarnings("unchecked")
	private static boolean matches(net.minecraft.world.item.crafting.Recipe<?> recipe,
			CraftingInput input, net.minecraft.server.level.ServerLevel level) {
		return ((net.minecraft.world.item.crafting.Recipe<CraftingInput>) recipe).matches(input, level);
	}

	@GameTest
	public void onigiriBuffsMeleeAndProjectilesWithoutStacking(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		try {
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			var food = new ItemStack(ModItems.ONIGIRI, 2);
			food.finishUsingItem(level, player);
			test.assertTrue(food.getCount() == 1, "Eating onigiri must consume exactly one item");
			test.assertTrue(player.getEffect(ModEffects.ONIGIRI).getDuration() == OnigiriItem.DURATION_TICKS,
					"Onigiri must last 30 seconds");
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 2.5) < 0.00001,
					"Onigiri must add 150 percent movement speed");
			food.finishUsingItem(level, player);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 2.5) < 0.00001,
					"A second onigiri must refresh the effect without stacking speed");
			var melee = test.spawnWithNoFreeWill(EntityTypes.COW, 1, 2, 1);
			melee.hurtServer(level, level.damageSources().playerAttack(player), 4);
			test.assertTrue(Math.abs(melee.getHealth() - (melee.getMaxHealth() - 6.4f)) < 0.001,
					"Onigiri must add exactly 60 percent melee damage");
			var arrow = new Arrow(EntityTypes.ARROW, level);
			arrow.setOwner(player);
			var ranged = test.spawnWithNoFreeWill(EntityTypes.COW, 3, 2, 1);
			ranged.hurtServer(level, level.damageSources().arrow(arrow, player), 4);
			test.assertTrue(Math.abs(ranged.getHealth() - (ranged.getMaxHealth() - 6.4f)) < 0.001,
					"Onigiri must add exactly 60 percent projectile damage");
			player.removeEffect(ModEffects.ONIGIRI);
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 0.00001,
					"Removing Onigiri must restore movement speed");
		} finally {
			player.discard();
		}
		test.succeed();
	}

	@GameTest
	public void furnaceCooksKettleAndTeaRestoresHungerWithEmptyKettleReturned(GameTestHelper test) {
		var level = test.getLevel();
		var pos = test.absolutePos(new BlockPos(2, 2, 2));
		level.setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
		var furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
		furnace.setItem(0, new ItemStack(ModItems.KIRILL_KETTLE));
		furnace.setItem(1, new ItemStack(Items.COAL));
		for (int tick = 0; tick < 199; tick++) {
			AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		}
		test.assertTrue(furnace.getItem(2).isEmpty(), "Kettle must take the full 200 ticks to cook");
		AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		test.assertTrue(furnace.getItem(2).is(ModItems.HARD_KIRILL_KETTLE)
				&& furnace.getItem(0).isEmpty(), "Furnace must turn kettle into hardcore kettle");
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		try {
			player.getFoodData().setFoodLevel(10);
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			var empty = furnace.getItem(2).copy().finishUsingItem(level, player);
			test.assertTrue(empty.is(ModItems.KIRILL_KETTLE), "Drinking tea must return the empty kettle");
			test.assertTrue(player.getFoodData().getFoodLevel() == 13, "Tea must restore 3 hunger points");
			test.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.2) < 0.00001,
					"Tea must add 20 percent movement speed");
			test.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration() == 600
					&& player.getEffect(MobEffects.REGENERATION).getAmplifier() == 0,
					"Tea must give regeneration I for 30 seconds");
		} finally {
			player.discard();
			level.removeBlock(pos, false);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 80)
	public void placedHeadUsesHayCubeKicksAndCanBeRecovered(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var support = test.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LEBEDEV_HEAD, 2));
		var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
		test.assertTrue(ModItems.LEBEDEV_HEAD.useOn(context).consumesAction(), "Head must be placeable");
		test.assertTrue(player.getMainHandItem().getCount() == 1, "Placing head must consume one item");
		var heads = test.getEntities(ModEntityTypes.LEBEDEV_HEAD);
		test.assertTrue(heads.size() == 1, "Placing must create exactly one head");
		var head = heads.getFirst();
		player.setPos(head.getX() - 1, head.getY(), head.getZ());
		player.setYRot(-90);
		player.setXRot(40);
		test.assertTrue(Math.abs(head.getBbWidth() - 0.8f) < 0.0001
				&& Math.abs(head.getBbHeight() - 0.8f) < 0.0001, "Head must be 0.8 blocks wide and high");
		test.runAfterDelay(2, () -> {
			var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
					net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
			head.saveWithoutId(saved);
			double friction = head.getAttributeValue(Attributes.FRICTION_MODIFIER);
			head.discard();
			var restored = ModEntityTypes.LEBEDEV_HEAD.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
			restored.load(net.minecraft.world.level.storage.TagValueInput.create(
					net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
			level.addFreshEntity(restored);
			restored.tick();
			test.assertTrue(restored.getBodyArmorItem().is(Items.HAY_BLOCK)
					&& restored.isPersistenceRequired() && Math.abs(restored.getBbWidth() - 0.8f) < 0.0001,
					"Reloaded head must keep its hay physics, persistence and dimensions");
			test.assertTrue(Math.abs(restored.getAttributeValue(Attributes.FRICTION_MODIFIER) - friction) < 0.00001,
					"Reloaded head must preserve hay archetype friction");
			float health = restored.getHealth();
			restored.setDeltaMovement(Vec3.ZERO);
			restored.hurtServer(level, level.damageSources().playerAttack(player), 4);
			test.assertTrue(restored.getHealth() == health && restored.getDeltaMovement().lengthSqr() > 0.01,
					"Head must receive hay sulfur cube kick impulse without losing health");
			test.assertFalse(restored.readyForShearing() || restored.canBePickedUpWithBucket(new ItemStack(Items.WATER_BUCKET)),
					"Head must not expose sulfur cube bucket or shearing mechanics");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(false);
			test.assertTrue(restored.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
					&& restored.isRemoved(), "Ordinary right click must recover the head without sneaking");
			test.assertTrue(player.getInventory().contains(stack -> stack.is(ModItems.LEBEDEV_HEAD)),
					"Recovered head must enter the inventory");
			player.discard();
			test.succeed();
		});
	}
}
