package dev.kirill.hbk;

import dev.kirill.hbk.entity.ImprovedBoatEntity;
import dev.kirill.hbk.menu.ImprovedBoatFuelMenu;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.util.List;

public final class ImprovedBoatGameTests {
	@GameTest
	public void improvedBoatsKeepWoodAndRecipes(GameTestHelper test) {
		var level = test.getLevel();
		var types = List.of(
				ModEntityTypes.IMPROVED_OAK_BOAT,
				ModEntityTypes.IMPROVED_SPRUCE_BOAT,
				ModEntityTypes.IMPROVED_BIRCH_BOAT,
				ModEntityTypes.IMPROVED_JUNGLE_BOAT,
				ModEntityTypes.IMPROVED_ACACIA_BOAT,
				ModEntityTypes.IMPROVED_CHERRY_BOAT,
				ModEntityTypes.IMPROVED_DARK_OAK_BOAT,
				ModEntityTypes.IMPROVED_PALE_OAK_BOAT,
				ModEntityTypes.IMPROVED_MANGROVE_BOAT,
				ModEntityTypes.IMPROVED_BAMBOO_RAFT
		);
		var sourceItems = List.of(
				Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT,
				Items.ACACIA_BOAT, Items.CHERRY_BOAT, Items.DARK_OAK_BOAT, Items.PALE_OAK_BOAT,
				Items.MANGROVE_BOAT, Items.BAMBOO_RAFT
		);
		List<Item> improvedItems = List.of(
				ModItems.IMPROVED_OAK_BOAT, ModItems.IMPROVED_SPRUCE_BOAT,
				ModItems.IMPROVED_BIRCH_BOAT, ModItems.IMPROVED_JUNGLE_BOAT,
				ModItems.IMPROVED_ACACIA_BOAT, ModItems.IMPROVED_CHERRY_BOAT,
				ModItems.IMPROVED_DARK_OAK_BOAT, ModItems.IMPROVED_PALE_OAK_BOAT,
				ModItems.IMPROVED_MANGROVE_BOAT, ModItems.IMPROVED_BAMBOO_RAFT
		);

		for (int index = 0; index < types.size(); index++) {
			ImprovedBoatEntity boat = types.get(index).create(level, EntitySpawnReason.COMMAND);
			test.assertTrue(boat != null, "Improved boat entity must be constructible");
			test.assertTrue(boat.getPickResult().is(improvedItems.get(index)),
					"Improved boat must keep its wood variant when picked or dropped");

			CraftingInput input = CraftingInput.of(2, 1, List.of(
					new ItemStack(sourceItems.get(index)),
					new ItemStack(Items.BLAST_FURNACE)
			));
			var recipe = level.getServer().getRecipeManager()
					.getRecipeFor(RecipeType.CRAFTING, input, level)
					.orElseThrow();
			test.assertTrue(recipe.value().assemble(input).is(improvedItems.get(index)),
					"Each wood variant must craft into its matching improved boat");
		}

		test.assertTrue(ImprovedBoatEntity.UNLIT_SPEED_MULTIPLIER == 0.5,
				"Unpowered furnace boats must move at half speed");
		test.assertTrue(ImprovedBoatEntity.LIT_SPEED_MULTIPLIER == 3.3,
				"Powered furnace boats must move at 3.3 times normal speed");
		test.succeed();
	}

	@GameTest
	public void boatFloatsWithDriverSeatedAboveWater(GameTestHelper test) {
		var level = test.getLevel();
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				level.setBlockAndUpdate(test.absolutePos(new BlockPos(x, 1, z)), Blocks.WATER.defaultBlockState());
				level.setBlockAndUpdate(test.absolutePos(new BlockPos(x, 2, z)), Blocks.WATER.defaultBlockState());
			}
		}
		BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
		ImprovedBoatEntity boat = ModEntityTypes.IMPROVED_OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
		test.assertTrue(boat != null, "Improved boat must be constructible");
		boat.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		level.addFreshEntity(boat);
		for (int tick = 0; tick < 80; tick++) boat.tick();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.startRiding(boat);
		boat.positionRider(player);
		test.assertTrue(boat.getY() < pos.getY() && boat.getY() > pos.getY() - 0.8,
				"Boat must settle at the water surface");
		// The sitting player's hips are about 0.75 blocks above the entity origin.
		// The model seat top is 0.8125 blocks above the boat origin.
		double hipY = player.getY() + 0.75;
		double seatY = boat.getY() + 0.8125;
		test.assertTrue(Math.abs(hipY - seatY) < 0.04,
				"Driver's hips must meet the seat: hips=" + hipY + ", seat=" + seatY);
		test.assertTrue(hipY > pos.getY() + 0.15,
				"Driver must sit above the waterline: hips=" + hipY + ", surface=" + pos.getY());
		test.assertTrue(Math.abs(player.getZ() - boat.getZ() - 0.44) < 0.02,
				"Driver must sit at the center of the seat along the hull");
		player.stopRiding();
		boat.discard();
		test.succeed();
	}

	@GameTest
	public void uraniumBurnsLongAndSmeltsSlowlyInFurnace(GameTestHelper test) {
		var level = test.getLevel();
		test.assertTrue(level.fuelValues().burnDuration(new ItemStack(ModItems.URANIUM_235)) == 600 * 20,
				"Uranium must provide exactly 600 seconds of fuel");
		BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
		level.setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
		var furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
		test.assertTrue(furnace != null, "Furnace must have its block entity");
		furnace.setItem(0, new ItemStack(Items.SAND));
		furnace.setItem(1, new ItemStack(ModItems.URANIUM_235));
		for (int tick = 0; tick < 250; tick++) {
			AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		}
		test.assertTrue(furnace.getItem(2).isEmpty(), "Uranium must not finish the normal 200-tick recipe at 250 ticks");
		for (int tick = 250; tick < 805; tick++) {
			AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		}
		test.assertTrue(furnace.getItem(2).is(Items.GLASS), "Uranium must finish the recipe after 800 ticks");

		level.removeBlock(pos, false);
		test.succeed();
	}

	@GameTest
	public void boatAcceptsFuelWithoutSmeltingSlotsAndBurnsOnlyWhenDriven(GameTestHelper test) {
		var level = test.getLevel();
		var filler = test.makeMockPlayer(GameType.SURVIVAL);
		var player = test.makeMockServerPlayerInLevel();
		ImprovedBoatEntity boat = ModEntityTypes.IMPROVED_OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
		test.assertTrue(boat != null, "Improved boat must be constructible");
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		boat.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		level.addFreshEntity(boat);
		test.assertTrue(boat.getBbWidth() >= 2.6f, "Hull hitbox must cover the long boat model");
		filler.setPos(boat.getX() + 1.0, boat.getY(), boat.getZ());
		filler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.URANIUM_235, 3));
		test.assertTrue(boat.interact(filler, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
				"Right click with fuel must load the tank directly");
		test.assertTrue(boat.getItem(ImprovedBoatEntity.FUEL_SLOT).getCount() == 3
				&& filler.getMainHandItem().isEmpty(),
				"Direct fueling must consume the held stack in survival");
		player.setPos(boat.getX() + 1.0, boat.getY(), boat.getZ());
		var menu = (ImprovedBoatFuelMenu) boat.createMenu(1, player.getInventory(), player);
		test.assertTrue(menu.slots.size() == 37 && menu.slots.getFirst().getContainerSlot() == ImprovedBoatEntity.FUEL_SLOT,
				"Boat menu must expose one fuel slot and the player's inventory");
		test.assertTrue(menu.slots.getFirst().mayPlace(new ItemStack(Items.COAL))
				&& !menu.slots.getFirst().mayPlace(new ItemStack(Items.COBBLESTONE)),
				"Only fuel may enter the boat's single slot");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (int tick = 0; tick < 5; tick++) boat.tick();
		test.assertTrue(menu.remainingSeconds() == 0, "Fuel must wait while the boat has no driver");
		test.assertTrue(boat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
				&& player.getVehicle() == boat, "Empty-hand right click must seat the driver");
		boat.tick();
		boat.positionRider(player);
		test.assertTrue(Math.abs(player.getY() - boat.getY() - 0.06) < 0.02,
				"Driver's rendered sitting pose must line up with the seat: rider="
						+ player.getY() + ", boat=" + boat.getY());
		test.assertTrue(menu.remainingSeconds() == 600
				&& boat.getItem(ImprovedBoatEntity.FUEL_SLOT).getCount() == 2,
				"The driver must ignite one uranium item for 600 seconds");
		player.stopRiding();
		boat.tick();
		test.assertTrue(menu.remainingSeconds() == 600, "Fuel must pause while the boat is parked");
		player.setShiftKeyDown(true);
		boat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		test.assertTrue(player.containerMenu instanceof ImprovedBoatFuelMenu,
				"Sneak and right click must open the fuel-only menu");
		level.getServer().getPlayerList().remove(player);
		boat.discard();
		test.succeed();
	}
}
