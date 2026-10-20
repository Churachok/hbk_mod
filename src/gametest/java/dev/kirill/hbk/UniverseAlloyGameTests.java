package dev.kirill.hbk;

import dev.kirill.hbk.mixin.AlloyFurnaceAccess;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class UniverseAlloyGameTests {
	@GameTest public void allFortyFiveAlloysFollowTheFullProductionChain(GameTestHelper test) {
		var level = test.getLevel(); var recipes = level.getServer().getRecipeManager();
		test.assertTrue(UniverseAlloys.CATALOG.size() == 45 && UniverseAlloys.recipes().size() == 226, "Every pair and upgrade is registered");
		for (var alloy : UniverseAlloys.CATALOG) {
			Item sample = UniverseAlloys.item(alloy.id() + "_sample"), ingot = UniverseAlloys.item(alloy.id() + "_ingot");
			for (boolean reverse : List.of(false, true)) for (boolean ores : List.of(false, true)) {
				Item first = ores ? UniverseMaterials.block(alloy.first(), 4).asItem() : UniverseMaterials.resource(alloy.first(), 0);
				Item second = ores ? UniverseMaterials.block(alloy.second(), 4).asItem() : UniverseMaterials.resource(alloy.second(), 0);
				var input = CraftingInput.of(2, 1, List.of(new ItemStack(reverse ? second : first), new ItemStack(reverse ? first : second)));
				var crafted = recipes.getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value().assemble(input);
				test.assertTrue(crafted.is(sample) && crafted.getCount() == 1, "Symmetric recipe accepts ores or their minerals: " + alloy.id());
			}
			var input = new SingleRecipeInput(new ItemStack(sample));
			var blast = recipes.getRecipeFor(RecipeType.BLASTING, input, level).orElseThrow().value();
			test.assertTrue(blast.assemble(input).is(ingot) && blast.cookingTime() == 200, "Blast furnace turns sample into its ingot");
			test.assertTrue(recipes.getRecipeFor(RecipeType.SMELTING, input, level).isEmpty() && recipes.getRecipeFor(RecipeType.SMOKING, input, level).isEmpty(), "Ordinary furnaces and smokers cannot replace the blast furnace");
			for (var reward : alloy.rewards()) {
				var base = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(reward.base())));
				base.set(DataComponents.CUSTOM_NAME, Component.literal("Старый знакомый"));
				if (base.isDamageableItem()) base.setDamageValue(41);
				base.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
				var smith = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(ingot));
				var result = recipes.getRecipeFor(RecipeType.SMITHING, smith, level).orElseThrow().value().assemble(smith);
				test.assertTrue(result.is(UniverseAlloys.item(reward.id())), "Smithing produces the accepted reward: " + reward.id());
				test.assertTrue(result.getHoverName().equals(base.getHoverName()) && result.getEnchantments().equals(base.getEnchantments()), "Name and enchantments survive smithing");
				if (base.isDamageableItem()) test.assertTrue(result.getDamageValue() == 41 && result.getMaxDamage() == base.getMaxDamage(), "Netherite durability and damage survive the upgrade");
				var noTemplate = new SmithingRecipeInput(ItemStack.EMPTY, base, new ItemStack(ingot));
				test.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, noTemplate, level).isEmpty(), "Upgrade template is required");
			}
			var storageInput = CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(ingot)));
			var storage = recipes.getRecipeFor(RecipeType.CRAFTING, storageInput, level).orElseThrow().value().assemble(storageInput);
			var unpackInput = CraftingInput.of(1, 1, List.of(storage));
			var unpack = recipes.getRecipeFor(RecipeType.CRAFTING, unpackInput, level).orElseThrow().value().assemble(unpackInput);
			test.assertTrue(unpack.is(ingot) && unpack.getCount() == 9, "Storage does not create or lose ingots");
		}
		test.succeed();
	}
	@GameTest public void realBlastFurnaceAndSmithingMenuConsumeTheirInputs(GameTestHelper test) {
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(pos, Blocks.BLAST_FURNACE.defaultBlockState());
		var furnace = (BlastFurnaceBlockEntity) level.getBlockEntity(pos);
		furnace.setItem(0, new ItemStack(UniverseAlloys.item("drilling_steel_sample"))); furnace.setItem(1, new ItemStack(Items.COAL));
		for (int i = 0; i < 205; i++) AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		test.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty() && furnace.getItem(2).is(UniverseAlloys.item("drilling_steel_ingot")), "Real furnace consumes sample and coal");
		var player = test.makeMockServerPlayerInLevel(); level.setBlockAndUpdate(pos.above(), Blocks.SMITHING_TABLE.defaultBlockState());
		var menu = new SmithingMenu(1, player.getInventory(), ContainerLevelAccess.create(level, pos.above()));
		menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
		menu.getSlot(1).set(new ItemStack(Items.NETHERITE_PICKAXE)); menu.getSlot(2).set(furnace.getItem(2).copy());
		test.assertTrue(menu.getSlot(3).getItem().is(UniverseAlloys.item("drill_pickaxe")), "Native smithing menu displays the drill");
		menu.quickMoveStack(player, 3);
		test.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty(), "Native smithing consumes template, netherite item and ingot");
		test.succeed();
	}
	@GameTest public void smeltingLootPreservesFortuneSilkTouchAndSpatialShardGate(GameTestHelper test) {
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(1, 2, 1));
		ItemStack heat = new ItemStack(UniverseAlloys.item("smelting_pickaxe"));
		var state = Blocks.IRON_ORE.defaultBlockState();
		var drops = Block.getDrops(state, level, pos, null, null, heat);
		test.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.IRON_INGOT), "Iron ore emerges smelted");
		var raw = List.of(new ItemStack(Items.RAW_IRON, 7));
		var converted = AlloyLoot.smelt(level, state, heat, raw);
		test.assertTrue(converted.size() == 1 && converted.getFirst().is(Items.IRON_INGOT) && converted.getFirst().getCount() == 7, "Fortune's already calculated count is preserved exactly once");
		heat.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		drops = Block.getDrops(state, level, pos, null, null, heat);
		test.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.IRON_ORE), "Silk Touch retains the ore block");
		for (String name : List.of("drill_pickaxe", "smelting_pickaxe", "seismic_hammer")) {
			var pickaxe = new ItemStack(UniverseAlloys.item(name));
			test.assertFalse(UniverseMining.permits(level, dev.kirill.hbk.registry.ModBlocks.FRACTURED_BLOCK.defaultBlockState(), pickaxe), "Alloy tools cannot mine spatial shards");
			for (int f = 0; f < 10; f++) test.assertFalse(Block.getDrops(UniverseMaterials.state(f, 4, 0), level, pos, null, null, pickaxe).isEmpty(), "Netherite alloy tools harvest parallel ores");
		}
		test.succeed();
	}
	@GameTest public void heatCartridgeConservesBurnTimeAndWashedSamplesCookFaster(GameTestHelper test) {
		var level = test.getLevel(); var p = test.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(p, Blocks.FURNACE.defaultBlockState()); level.setBlockAndUpdate(p.east(), Blocks.BLAST_FURNACE.defaultBlockState());
		var first = (AbstractFurnaceBlockEntity) level.getBlockEntity(p); var second = (AbstractFurnaceBlockEntity) level.getBlockEntity(p.east());
		((AlloyFurnaceAccess) first).hbk$setHeat(700); ((AlloyFurnaceAccess) first).hbk$setHeatTotal(1600);
		var player = test.makeMockPlayer(GameType.SURVIVAL); var cartridge = new ItemStack(UniverseAlloys.item("heat_cartridge")); player.setItemInHand(InteractionHand.MAIN_HAND, cartridge);
		AlloyActions.useOn("heat_cartridge", new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false)));
		test.assertTrue(((AlloyFurnaceAccess) first).hbk$getHeat() == 0 && AlloyActions.number(cartridge, "heat", 0) == 700, "Capturing heat removes it from the source");
		AlloyActions.useOn("heat_cartridge", new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(p.east()), Direction.UP, p.east(), false)));
		test.assertTrue(((AlloyFurnaceAccess) second).hbk$getHeat() == 700 && AlloyActions.number(cartridge, "heat", 0) == 0, "Transfer cannot copy stored heat");
		var sample = new ItemStack(UniverseAlloys.item("light_bronze_sample")); AlloyActions.setNumber(sample, "washed", 1); second.setItem(0, sample);
		for (int i = 0; i < 105; i++) AbstractFurnaceBlockEntity.serverTick(level, p.east(), level.getBlockState(p.east()), second);
		test.assertTrue(second.getItem(2).is(UniverseAlloys.item("light_bronze_ingot")), "Washed samples finish after 100 ticks");
		test.succeed();
	}
	@GameTest public void gasProtectionDevicesConsumeFiltersAndPersistTheirInventory(GameTestHelper test) {
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(1, 2, 1));
		var block = UniverseAlloys.device("air_purifier"); level.setBlockAndUpdate(pos, block.defaultBlockState());
		var machine = (AlloyDeviceBlockEntity) level.getBlockEntity(pos); machine.setItem(0, new ItemStack(Items.PAPER)); machine.setItem(1, new ItemStack(Items.CHARCOAL));
		var player = test.makeMockPlayer(GameType.SURVIVAL); player.setPos(pos.getX() + 2, pos.getY(), pos.getZ());
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(UniverseAlloys.item("hermetic_helmet")));
		for (var kind : List.of(UniverseHazards.Kind.SPORE_GAS, UniverseHazards.Kind.FROST_GAS, UniverseHazards.Kind.CINDER_GAS)) test.assertTrue(UniverseHazards.protectedFromGas(player, kind), "Helmet protects from every local gas");
		player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		test.assertFalse(UniverseHazards.protectedFromGas(player, UniverseHazards.Kind.SPORE_GAS), "Helmet protection stops when unequipped");
		long previous = level.getGameTime();
		// Tick at the next machine boundary without changing world time.
		test.runAfterDelay((int) (20 - previous % 20), () -> {
			AlloyDeviceBlockEntity.tick(level, pos, level.getBlockState(pos), machine);
			test.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty(), "The purifier actually consumes paper and charcoal");
			test.assertTrue(UniverseHazards.protectedFromGas(player, UniverseHazards.Kind.SPORE_GAS), "Working purifier protects nearby players");
			machine.setItem(4, new ItemStack(Items.PAPER, 17));
			var saved = machine.saveWithFullMetadata(level.registryAccess());
			var restored = (AlloyDeviceBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
			test.assertTrue(restored != null && restored.getItem(4).getCount() == 17, "Native container inventory persists");
			test.succeed();
		});
	}
	@GameTest public void carryingGripsMoveOneContainerWithoutDuplicatingItsContents(GameTestHelper test) {
		var level = test.getLevel(); var origin = test.absolutePos(new BlockPos(1, 2, 1)); var target = origin.east(2);
		level.setBlockAndUpdate(origin, Blocks.BARREL.defaultBlockState());
		var barrel = (BarrelBlockEntity) level.getBlockEntity(origin); var namedBarrel = new ItemStack(Items.BARREL); namedBarrel.set(DataComponents.CUSTOM_NAME, Component.literal("Груз")); barrel.applyComponentsFromItemStack(namedBarrel); barrel.setItem(26, new ItemStack(Items.DIAMOND, 11));
		var player = test.makeMockPlayer(GameType.SURVIVAL); player.setPos(origin.getX(), origin.getY() + 1, origin.getZ() + 3);
		var grips = new ItemStack(UniverseAlloys.item("carrying_grips")); player.setItemInHand(InteractionHand.MAIN_HAND, grips);
		AlloyActions.useOn("carrying_grips", new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false)));
		test.assertTrue(level.getBlockState(origin).isAir() && grips.has(DataComponents.BLOCK_ENTITY_DATA), "The original container is removed into the grips");
		AlloyActions.useOn("carrying_grips", new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(target.below()), Direction.UP, target.below(), false)));
		var restored = (BarrelBlockEntity) level.getBlockEntity(target);
		test.assertTrue(restored != null && restored.getItem(26).getCount() == 11 && restored.getName().equals(Component.literal("Груз")), "Contents and name survive placement");
		test.assertFalse(grips.has(DataComponents.BLOCK_ENTITY_DATA), "Placement clears the stored container exactly once");
		test.succeed();
	}
	@GameTest public void cargoMinecartStoresAllFiftyFourSlots(GameTestHelper test) {
		var cart = UniverseAlloys.CARGO_MINECART.create(test.getLevel(), EntitySpawnReason.COMMAND);
		test.assertTrue(cart != null && cart.getContainerSize() == 54, "Large cart has 54 real slots");
		cart.setItem(53, new ItemStack(Items.DIAMOND, 23));
		test.assertTrue(cart.getItem(53).getCount() == 23 && cart.getItemStacks().size() == 54, "The last slot is usable");
		var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, test.getLevel().registryAccess());
		cart.saveWithoutId(saved);
		var restored = UniverseAlloys.CARGO_MINECART.create(test.getLevel(), EntitySpawnReason.LOAD);
		restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, test.getLevel().registryAccess(), saved.buildResult()));
		test.assertTrue(restored.getItem(53).getCount() == 23, "Slot 54 survives native entity serialization");
		cart.clearItemStacks(); test.assertTrue(cart.getItem(53).isEmpty(), "Native inventory clearing covers all 54 slots"); test.succeed();
	}
	@GameTest public void sampleBagHasEighteenFilteredPersistentSlots(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel(); var bag = new ItemStack(UniverseAlloys.item("sample_bag"));
		player.setItemInHand(InteractionHand.MAIN_HAND, bag); AlloyActions.use("sample_bag", player.level(), player, InteractionHand.MAIN_HAND);
		var menu = player.containerMenu;
		test.assertTrue(menu instanceof ChestMenu chest && chest.getRowCount() == 2 && menu.slots.size() == 54, "Native two-row menu provides exactly 18 storage slots");
		var mineral = new ItemStack(UniverseMaterials.resource(0, 0), 7);
		test.assertTrue(menu.getSlot(0).mayPlace(mineral), "Minerals fit the sample bag");
		test.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.BARREL)) || menu.getSlot(0).mayPlace(bag), "Containers and bags cannot be nested into the sample bag");
		test.assertFalse(menu.getSlot(45).mayPickup(player), "The open bag is locked in the selected hotbar slot");
		menu.getSlot(0).setByPlayer(mineral); player.closeContainer();
		var saved = NonNullList.withSize(18, ItemStack.EMPTY); bag.get(DataComponents.CONTAINER).copyInto(saved);
		test.assertTrue(saved.getFirst().getCount() == 7, "Bag contents are stored in the native item component"); test.succeed();
	}
	@GameTest public void sailBootsAllowOnlyOneExtraJumpUntilLanding(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel(); player.setItemSlot(EquipmentSlot.FEET, new ItemStack(UniverseAlloys.item("sail_boots")));
		player.setOnGround(false); player.getAbilities().flying = false;
		player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
		for (int i = 0; i < 4; i++) AlloyActions.mobility(player);
		var jump = new net.minecraft.world.entity.player.Input(false, false, false, false, true, false, false);
		player.setLastClientInput(jump); AlloyActions.mobility(player);
		test.assertTrue(Math.abs(player.getDeltaMovement().y - .48) < .001, "A fresh press gives one extra jump");
		player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY); AlloyActions.mobility(player); player.setDeltaMovement(Vec3.ZERO);
		player.setLastClientInput(jump); AlloyActions.mobility(player);
		test.assertTrue(player.getDeltaMovement().y == 0, "Repeated presses cannot fly indefinitely");
		player.setOnGround(true); player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY); AlloyActions.mobility(player); player.setOnGround(false);
		for (int i = 0; i < 4; i++) AlloyActions.mobility(player);
		player.setLastClientInput(jump); AlloyActions.mobility(player);
		test.assertTrue(player.getDeltaMovement().y > .4, "Landing replenishes exactly one extra jump"); test.succeed();
	}
	@GameTest public void gravityChestplateDampensTheActualLocalGravity(GameTestHelper test) {
		var level = UniverseManager.load(test.getLevel().getServer(), new UniverseCode("23456"));
		var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, 0, 180, 0);
		double factor = UniverseEnvironment.gravity(UniverseProfile.of(level), stand.position()), before = stand.getGravity();
		stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(UniverseAlloys.item("gravity_chestplate")));
		test.assertTrue(Math.abs(stand.getGravity() - before / factor * (1 + (factor - 1) * .2)) < .00001, "The chestplate dampens the actual gravity hook by 80%"); test.succeed();
	}
	@GameTest public void sickleHarvestsNativeAndParallelCropsWithoutFreeReplanting(GameTestHelper test) {
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(1, 2, 1));
		var player = test.makeMockPlayer(GameType.SURVIVAL); var sickle = new ItemStack(UniverseAlloys.item("harvest_sickle")); player.setItemInHand(InteractionHand.MAIN_HAND, sickle);
		level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState()); level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
		var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		AlloyActions.useOn("harvest_sickle", context);
		test.assertTrue(level.getBlockState(pos).getValue(CropBlock.AGE) == 7, "Annual crops require inventory seeds");
		player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS)); AlloyActions.useOn("harvest_sickle", context);
		test.assertTrue(level.getBlockState(pos).getValue(CropBlock.AGE) == 0 && player.getInventory().getItem(1).isEmpty(), "One seed is consumed for replanting");
		var local = pos.east(); level.setBlockAndUpdate(local.below(), UniverseBotany.block(0, "soil").defaultBlockState());
		level.setBlockAndUpdate(local, UniverseBotany.block(0, "plant").defaultBlockState().setValue(UniverseCropBlock.AGE, 3));
		AlloyActions.useOn("harvest_sickle", context);
		test.assertTrue(level.getBlockState(local).getValue(UniverseCropBlock.AGE) == 1, "Perennial parallel plants keep their native renewable stem"); test.succeed();
	}
	@GameTest(maxTicks = 100) public void washerAndRecyclingAnvilConserveTheirInputs(GameTestHelper test) {
		var level = test.getLevel(); var p = test.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(p, UniverseAlloys.device("washing_trough").defaultBlockState()); var washer = (AlloyDeviceBlockEntity) level.getBlockEntity(p);
		washer.setItem(0, new ItemStack(UniverseAlloys.item("heat_alloy_sample"), 2)); washer.setItem(1, new ItemStack(Items.WATER_BUCKET));
		var r = p.east(2); level.setBlockAndUpdate(r, UniverseAlloys.device("recycling_anvil").defaultBlockState()); var recycler = (AlloyDeviceBlockEntity) level.getBlockEntity(r);
		recycler.setItem(0, new ItemStack(UniverseEquipment.item(0, "chestplate")));
		test.runAfterDelay(25, () -> {
			test.assertTrue(washer.getItem(0).getCount() == 1 && washer.getItem(1).is(Items.BUCKET), "Washing consumes exactly one sample and one water bucket");
			test.assertTrue(AlloyActions.number(washer.getItem(2), "washed", 0) == 1 && washer.getItem(2).getCount() == 1, "One washed sample retains its alloy identity");
			test.assertTrue(recycler.getItem(0).isEmpty() && recycler.getItem(1).is(UniverseMaterials.resource(0, 0)) && recycler.getItem(1).getCount() == 2, "Recycling yields only 35% rounded down, consuming the equipment");
			test.succeed();
		});
	}
	@GameTest(maxTicks = 100) public void poweredPumpTransfersOneSourceIntoOneAdjacentBucket(GameTestHelper test) {
		var level = test.getLevel(); var p = test.absolutePos(new BlockPos(1, 3, 1));
		level.setBlockAndUpdate(p, UniverseAlloys.device("drain_pump").defaultBlockState()); level.setBlockAndUpdate(p.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		level.setBlockAndUpdate(p.north(), Blocks.BARREL.defaultBlockState()); var tank = (BarrelBlockEntity) level.getBlockEntity(p.north()); tank.setItem(0, new ItemStack(Items.BUCKET));
		level.setBlockAndUpdate(p.east(2).below(), Blocks.STONE.defaultBlockState()); level.setBlockAndUpdate(p.east(2), Blocks.WATER.defaultBlockState());
		test.runAfterDelay(25, () -> {
			test.assertTrue(tank.getItem(0).is(Items.WATER_BUCKET), "Pump fills a real adjacent bucket");
			test.assertFalse(level.getBlockState(p.east(2)).is(Blocks.WATER) && level.getFluidState(p.east(2)).isSource(), "The transferred source is removed from the world"); test.succeed();
		});
	}
	@GameTest public void phaseGlassActuallyChangesCollisionWithPower(GameTestHelper test) {
		var level = test.getLevel(); var p = test.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(p, UniverseAlloys.device("phase_glass").defaultBlockState()); level.setBlockAndUpdate(p.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		test.runAfterDelay(5, () -> {
			test.assertTrue(level.getBlockState(p).getCollisionShape(level, p).isEmpty(), "Powered glass is genuinely passable");
			level.setBlockAndUpdate(p.west(), Blocks.AIR.defaultBlockState());
			test.runAfterDelay(5, () -> { test.assertFalse(level.getBlockState(p).getCollisionShape(level, p).isEmpty(), "Unpowered glass becomes solid again"); test.succeed(); });
		});
	}
	@GameTest(maxTicks = 100) public void transitionPlatesAndLiftUseSafeRealEndpoints(GameTestHelper test) {
		var level = test.getLevel(); var p = test.absolutePos(new BlockPos(1, 2, 1)); var target = p.east(7);
		for (var pos : List.of(p, target)) {
			for (int y = 1; y <= 3; y++) level.setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(pos, UniverseAlloys.device("transition_plate").defaultBlockState());
		}
		var player = test.makeMockPlayer(GameType.SURVIVAL); player.setShiftKeyDown(true);
		var first = (AlloyDeviceBlockEntity) level.getBlockEntity(p); var second = (AlloyDeviceBlockEntity) level.getBlockEntity(target); first.use(player); second.use(player);
		var traveller = new net.minecraft.world.entity.decoration.ArmorStand(level, p.getX() + .5, p.getY() + .15, p.getZ() + .5); traveller.setNoGravity(true); level.addFreshEntity(traveller);
		var lift = p.south(4); var upper = lift.above(6);
		for (int y = 1; y <= 9; y++) level.setBlockAndUpdate(lift.above(y), Blocks.AIR.defaultBlockState());
		for (var pos : List.of(lift, upper)) level.setBlockAndUpdate(pos, UniverseAlloys.device("lift_platform").defaultBlockState());
		var cargo = new net.minecraft.world.entity.decoration.ArmorStand(level, lift.getX() + .5, lift.getY() + .15, lift.getZ() + .5); cargo.setNoGravity(true); level.addFreshEntity(cargo);
		test.runAfterDelay(10, () -> {
			test.assertTrue(traveller.distanceToSqr(Vec3.atCenterOf(target)) < 2, "Linked plates move the entity to their actual paired endpoint");
			test.assertTrue(cargo.getY() > upper.getY(), "The lift passes through the clear vertical shaft to the upper platform");
			test.runAfterDelay(65, () -> { test.assertTrue(traveller.distanceToSqr(Vec3.atCenterOf(target)) < 2, "Standing on a plate does not cause repeated return teleports"); test.succeed(); });
		});
	}
	@GameTest public void ropeLadderHangsBelowItsAnchorAndRetractsWithoutFreeDrops(GameTestHelper test) {
		var level = test.getLevel(); var anchor = test.absolutePos(new BlockPos(1, 29, 1)); var top = anchor.east();
		for (int y = 0; y < 24; y++) { level.setBlockAndUpdate(top.below(y), Blocks.AIR.defaultBlockState()); if (y > 0) level.setBlockAndUpdate(anchor.below(y), Blocks.AIR.defaultBlockState()); }
		level.setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
		var player = test.makeMockPlayer(GameType.SURVIVAL); player.setPos(anchor.getX() + 3, anchor.getY(), anchor.getZ());
		var spool = new ItemStack(UniverseAlloys.item("rope_ladder_spool")); player.setItemInHand(InteractionHand.MAIN_HAND, spool);
		AlloyActions.useOn("rope_ladder_spool", new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(anchor), Direction.EAST, anchor, false)));
		test.assertTrue(AlloyActions.number(spool, "ladder_length", 0) == 24 && level.getBlockState(top.below(23)).is(UniverseAlloys.ROPE_LADDER), "Twenty-four sections hang freely from the top anchor");
		test.assertTrue(Block.getDrops(level.getBlockState(top), level, top, null, player, spool).isEmpty(), "Breaking a deployed section cannot manufacture free ladder items");
		player.setShiftKeyDown(true); AlloyActions.use("rope_ladder_spool", level, player, InteractionHand.MAIN_HAND);
		test.assertTrue(level.getBlockState(top).isAir() && level.getBlockState(top.below(23)).isAir() && AlloyActions.number(spool, "ladder_length", -1) == 0, "Retraction removes the complete hanging ladder"); test.succeed();
	}
	@GameTest(maxTicks = 240) public void rhythmControllerRecordsAndReplaysRealRedstone(GameTestHelper test) {
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(pos, UniverseAlloys.device("rhythm_controller").defaultBlockState()); level.setBlockAndUpdate(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		var controller = (AlloyDeviceBlockEntity) level.getBlockEntity(pos); var player = test.makeMockPlayer(GameType.SURVIVAL); player.setShiftKeyDown(true); controller.use(player);
		test.runAfterDelay(165, () -> {
			level.setBlockAndUpdate(pos.west(), Blocks.AIR.defaultBlockState()); player.setShiftKeyDown(false); controller.use(player);
			test.runAfterDelay(25, () -> {
				test.assertTrue(controller.signal() == 15 && level.getBlockState(pos).getSignal(level, pos, Direction.EAST) == 15, "The controller replays recorded input after the live source is removed");
				var restored = (AlloyDeviceBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos), controller.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
				test.assertTrue(restored != null && restored.signal() == 15, "Recorded state and output persist with the block"); test.succeed();
			});
		});
	}
}
