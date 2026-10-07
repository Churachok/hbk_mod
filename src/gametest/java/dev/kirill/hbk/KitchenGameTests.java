package dev.kirill.hbk;

import dev.kirill.hbk.item.OnigiriItem;
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
