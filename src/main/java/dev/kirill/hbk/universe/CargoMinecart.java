package dev.kirill.hbk.universe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Native minecart physics/model; all 54 slots use native container persistence and drops. */
public final class CargoMinecart extends MinecartChest {
	private NonNullList<ItemStack> cargo = NonNullList.withSize(54, ItemStack.EMPTY);
	public CargoMinecart(EntityType<? extends MinecartChest> type, Level level) { super(type, level); }
	@Override public int getContainerSize() { return 54; }
	@Override public NonNullList<ItemStack> getItemStacks() { return cargo; }
	@Override public void clearItemStacks() { cargo = NonNullList.withSize(54, ItemStack.EMPTY); }
	@Override protected Item getDropItem() { return UniverseAlloys.item("cargo_minecart"); }
	@Override public ItemStack getPickResult() { return new ItemStack(getDropItem()); }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.sixRows(id, inventory, this); }
}
