package dev.kirill.hbk.entity;

import dev.kirill.hbk.menu.ImprovedBoatFuelMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** A single-seat boat with a persistent fuel tank and fuel-powered speed. */
public final class ImprovedBoatEntity extends Boat implements Container, ExtendedMenuProvider<Integer> {
	public static final double UNLIT_SPEED_MULTIPLIER = 0.5;
	public static final double LIT_SPEED_MULTIPLIER = 3.3;
	private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(
			ImprovedBoatEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int LEGACY_INPUT = 0;
	public static final int FUEL_SLOT = 1;
	private static final int LEGACY_RESULT = 2;

	private final SimpleContainer inventory = new SimpleContainer(3);
	private final ContainerData fuelData = new ContainerData() {
		@Override public int get(int slot) {
			return switch (slot) {
				case 0 -> burnTime;
				case 1 -> burnDuration;
				default -> 0;
			};
		}
		@Override public void set(int slot, int value) {
			switch (slot) {
				case 0 -> burnTime = value;
				case 1 -> burnDuration = value;
				default -> { }
			}
		}
		@Override public int getCount() { return 2; }
	};
	private int burnTime;
	private int burnDuration;

	public ImprovedBoatEntity(EntityType<? extends ImprovedBoatEntity> type, Level level, Supplier<Item> dropItem) {
		super(type, level, dropItem);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_LIT, false);
	}

	@Override
	public void tick() {
		double oldX = this.getX();
		double oldZ = this.getZ();
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel) {
			this.tickFuel(serverLevel);
			this.entityData.set(DATA_LIT, this.burnTime > 0);
		}
		if (!this.isRemoved() && this.hasControllingPassenger() && this.isLocalInstanceAuthoritative()) {
			double bonus = (this.entityData.get(DATA_LIT)
					? LIT_SPEED_MULTIPLIER : UNLIT_SPEED_MULTIPLIER) - 1.0;
			Vec3 extra = new Vec3((this.getX() - oldX) * bonus, 0,
					(this.getZ() - oldZ) * bonus);
			if (extra.horizontalDistanceSqr() > 1.0E-8) {
				this.move(MoverType.SELF, extra);
			}
		}
	}

	private void tickFuel(ServerLevel level) {
		// Saved boats from the old three-slot furnace keep their fuel; return the other items.
		for (int slot : new int[]{LEGACY_INPUT, LEGACY_RESULT}) {
			ItemStack legacy = this.inventory.removeItemNoUpdate(slot);
			if (!legacy.isEmpty()) this.spawnAtLocation(level, legacy);
		}
		if (!this.hasControllingPassenger()) return;
		if (this.burnTime > 0) this.burnTime--;
		if (this.burnTime > 0) return;
		ItemStack fuel = this.inventory.getItem(FUEL_SLOT);
		int duration = level.fuelValues().burnDuration(fuel);
		if (duration <= 0) return;
		this.burnTime = this.burnDuration = duration;
		ItemStack remainder = fuel.getCraftingRemainder() == null
				? ItemStack.EMPTY : fuel.getCraftingRemainder().create();
		fuel.shrink(1);
		if (fuel.isEmpty() && !remainder.isEmpty()) this.inventory.setItem(FUEL_SLOT, remainder);
		else if (!remainder.isEmpty()) this.spawnAtLocation(level, remainder);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
		if (player.isShiftKeyDown()) {
			if (!this.level().isClientSide()) {
				player.openMenu(this);
			}
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (player.level().fuelValues().isFuel(held)) {
			if (!this.level().isClientSide()) {
				if (!this.insertFuel(player, held)) player.openMenu(this);
			}
			return InteractionResult.SUCCESS;
		}
		return super.interact(player, hand, hitLocation);
	}

	private boolean insertFuel(Player player, ItemStack held) {
		ItemStack slot = this.inventory.getItem(FUEL_SLOT);
		if (!slot.isEmpty() && !ItemStack.isSameItemSameComponents(slot, held)) return false;
		int space = Math.min(held.getMaxStackSize(), this.inventory.getMaxStackSize()) - slot.getCount();
		if (space <= 0) return false;
		int moved = Math.min(space, held.getCount());
		if (slot.isEmpty()) {
			ItemStack inserted = held.copy();
			inserted.setCount(moved);
			this.inventory.setItem(FUEL_SLOT, inserted);
		} else {
			slot.grow(moved);
		}
		if (!player.isCreative()) held.shrink(moved);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.hbk.improved_boat.fuel_loaded"));
		}
		return true;
	}

	@Override public Component getDisplayName() {
		return Component.translatable("container.hbk.improved_boat");
	}
	@Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new ImprovedBoatFuelMenu(id, playerInventory, this, this.fuelData);
	}
	@Override public Integer getScreenOpeningData(ServerPlayer player) { return this.getId(); }

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ContainerHelper.saveAllItems(output, this.inventory.getItems());
		output.putInt("burn_time", this.burnTime);
		output.putInt("burn_duration", this.burnDuration);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ContainerHelper.loadAllItems(input, this.inventory.getItems());
		this.burnTime = input.getIntOr("burn_time", 0);
		this.burnDuration = input.getIntOr("burn_duration", 0);
		this.entityData.set(DATA_LIT, this.burnTime > 0);
	}

	@Override
	public void destroy(ServerLevel level, Item dropItem) {
		for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
			ItemStack stack = this.inventory.removeItemNoUpdate(slot);
			if (!stack.isEmpty() && level.getGameRules().get(GameRules.ENTITY_DROPS)) {
				this.spawnAtLocation(level, stack);
			}
		}
		super.destroy(level, dropItem);
	}

	@Override public int getContainerSize() { return this.inventory.getContainerSize(); }
	@Override public boolean isEmpty() { return this.inventory.isEmpty(); }
	@Override public ItemStack getItem(int slot) { return this.inventory.getItem(slot); }
	@Override public ItemStack removeItem(int slot, int count) { return this.inventory.removeItem(slot, count); }
	@Override public ItemStack removeItemNoUpdate(int slot) { return this.inventory.removeItemNoUpdate(slot); }
	@Override public void setItem(int slot, ItemStack stack) { this.inventory.setItem(slot, stack); }
	@Override public void setChanged() { this.inventory.setChanged(); }
	@Override public boolean stillValid(Player player) { return this.isAlive() && player.distanceToSqr(this) < 64.0; }
	@Override public void clearContent() { this.inventory.clearContent(); }
	@Override protected int getMaxPassengers() { return 1; }
	@Override protected float getSinglePassengerXOffset() { return 0.44f; }
	// Minecraft renders a seated player's hips about 0.75 blocks above their
	// entity origin and subtracts the 0.6 vehicle attachment from this height.
	// Place the hips on the model's seat top at 0.8125 above the boat origin.
	@Override protected double rideHeight(EntityDimensions dimensions) { return 0.66; }
}
