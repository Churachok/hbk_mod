package dev.kirill.hbk.menu;

import dev.kirill.hbk.entity.ImprovedBoatEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One boat fuel slot, plus the player's regular inventory. */
public final class ImprovedBoatFuelMenu extends AbstractContainerMenu {
	private static final int FUEL_MENU_SLOT = 0;
	private static final int PLAYER_START = 1;
	private static final int PLAYER_END = 37;
	private final Container boatInventory;
	private final ContainerData burnData;

	public ImprovedBoatFuelMenu(int id, Inventory playerInventory, int boatId) {
		this(id, playerInventory, findBoat(playerInventory.player, boatId), new SimpleContainerData(2));
	}

	public ImprovedBoatFuelMenu(int id, Inventory playerInventory, Container boatInventory, ContainerData burnData) {
		super(ModMenuTypes.IMPROVED_BOAT_FUEL, id);
		checkContainerSize(boatInventory, 3);
		checkContainerDataCount(burnData, 2);
		this.boatInventory = boatInventory;
		this.burnData = burnData;
		this.addSlot(new Slot(boatInventory, ImprovedBoatEntity.FUEL_SLOT, 80, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return playerInventory.player.level().fuelValues().isFuel(stack);
			}
		});
		this.addStandardInventorySlots(playerInventory, 8, 84);
		this.addDataSlots(burnData);
	}

	private static Container findBoat(Player player, int boatId) {
		Entity entity = player.level().getEntity(boatId);
		return entity instanceof ImprovedBoatEntity boat ? boat : new SimpleContainer(3);
	}

	public int burnPercent() {
		int duration = this.burnData.get(1);
		return duration <= 0 ? 0 : Math.clamp(this.burnData.get(0) * 100 / duration, 0, 100);
	}

	public int remainingSeconds() {
		return (this.burnData.get(0) + 19) / 20;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.boatInventory.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (slotIndex == FUEL_MENU_SLOT) {
			if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
		} else if (this.slots.get(FUEL_MENU_SLOT).mayPlace(stack)) {
			if (!this.moveItemStackTo(stack, FUEL_MENU_SLOT, PLAYER_START, false)) return ItemStack.EMPTY;
		} else {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}
}
