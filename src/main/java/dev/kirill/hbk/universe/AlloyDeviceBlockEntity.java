package dev.kirill.hbk.universe;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class AlloyDeviceBlockEntity extends BaseContainerBlockEntity {
	private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
	private int fuel, signal, cursor, step;
	private boolean recording, playing;
	private int[] rhythm = new int[8];
	private BlockPos endpoint;
	private static final Map<AlloyDeviceBlockEntity, Boolean> FILTERS = new WeakHashMap<>();
	private static final Map<Player, AlloyDeviceBlockEntity> LINKING = new WeakHashMap<>();
	private static final Map<Entity, GlobalPos> TRANSIT = new WeakHashMap<>();
	public static void clearCaches() { FILTERS.clear(); LINKING.clear(); TRANSIT.clear(); }
	public static void releaseDepartedEntities() {
		TRANSIT.entrySet().removeIf(e -> e.getKey().isRemoved() || !e.getKey().level().dimension().equals(e.getValue().dimension()) || e.getValue().pos().distToCenterSqr(e.getKey().position()) > 4);
	}
	public AlloyDeviceBlockEntity(BlockPos pos, BlockState state) { super(UniverseAlloys.DEVICE_TYPE, pos, state); }
	public String kind() { return ((AlloyDeviceBlock) getBlockState().getBlock()).kind; }
	@Override public int getContainerSize() { return 27; }
	@Override protected NonNullList<ItemStack> getItems() { return items; }
	@Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
	@Override protected Component getDefaultName() { return Component.translatable("block.hbk." + kind()); }
	@Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.threeRows(id, inventory, this); }
	public int signal() { return signal; }
	public void use(Player player) {
		if (kind().equals("transition_plate") && player.isShiftKeyDown()) {
			var first = LINKING.remove(player);
			if (first == null || first.isRemoved() || first.level != level || first == this) {
				LINKING.put(player, this); AlloyActions.message(player, "link_first");
			} else if (first.worldPosition.distSqr(worldPosition) <= 128 * 128) {
				unlink(); first.unlink(); endpoint = first.worldPosition; first.endpoint = worldPosition; first.setChanged(); setChanged(); AlloyActions.message(player, "linked");
			} else AlloyActions.message(player, "link_far");
			return;
		}
		if (kind().equals("rhythm_controller")) {
			if (player.isShiftKeyDown()) { recording = true; playing = false; step = 0; output(0); AlloyActions.message(player, "recording"); }
			else { playing = !playing; recording = false; step = 0; if (!playing) output(0); AlloyActions.message(player, playing ? "enabled" : "disabled"); }
			setChanged(); return;
		}
		if (kind().equals("wind_vane")) {
			var profile = UniverseProfile.of(level);
			player.sendOverlayMessage(Component.translatable("message.hbk.alloy.wind", profile == null ? 0 : profile.get(HiddenParameter.WIND_STRENGTH),
					profile == null ? 0 : Math.floorMod((int) Math.toDegrees(UniverseWeather.windAngle(profile, level.getGameTime())), 360)));
			return;
		}
		if (Set.of("phase_glass", "lift_platform", "transition_plate", "anomaly_sensor").contains(kind())) return;
		player.openMenu(this);
		AlloyActions.message(player, kind() + "_help");
	}
	private void unlink() {
		if (endpoint != null && level != null && level.hasChunkAt(endpoint) && level.getBlockEntity(endpoint) instanceof AlloyDeviceBlockEntity other && worldPosition.equals(other.endpoint)) { other.endpoint = null; other.setChanged(); }
		endpoint = null; setChanged();
	}
	@Override protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out); ContainerHelper.saveAllItems(out, items); out.putInt("fuel", fuel); out.putInt("signal", signal); out.putInt("cursor", cursor);
		out.putInt("step", step); out.putBoolean("recording", recording); out.putBoolean("playing", playing); out.putIntArray("rhythm", rhythm);
		if (endpoint != null) out.store("endpoint", BlockPos.CODEC, endpoint);
	}
	@Override protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in); items = NonNullList.withSize(27, ItemStack.EMPTY); ContainerHelper.loadAllItems(in, items);
		fuel = Math.max(0, in.getIntOr("fuel", 0)); signal = Math.clamp(in.getIntOr("signal", 0), 0, 15); cursor = in.getIntOr("cursor", 0);
		step = Math.floorMod(in.getIntOr("step", 0), 8); recording = in.getBooleanOr("recording", false); playing = in.getBooleanOr("playing", false);
		rhythm = in.getIntArray("rhythm").filter(a -> a.length == 8).orElseGet(() -> new int[8]); endpoint = in.read("endpoint", BlockPos.CODEC).orElse(null);
	}
	@Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) { Containers.dropContents(level, pos, this); unlink(); } FILTERS.remove(this); super.preRemoveSideEffects(pos, state);
	}
	public static void tick(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, AlloyDeviceBlockEntity self) {
		if (!(world instanceof ServerLevel level)) return;
		long now = level.getGameTime(); String kind = self.kind();
		if ((kind.equals("transition_plate") || kind.equals("lift_platform")) && now % 5 == 0) self.transport(level, now);
		if (kind.equals("phase_glass") && now % 2 == 0) {
			boolean powered = level.hasNeighborSignal(pos);
			if (powered != state.getValue(AlloyDeviceBlock.POWERED)) level.setBlockAndUpdate(pos, state.setValue(AlloyDeviceBlock.POWERED, powered));
		}
		if (now % 20 != 0) return;
		switch (kind) {
			case "wind_vane" -> { var p = UniverseProfile.of(level); self.output(p == null ? 0 : Math.round(p.get(HiddenParameter.WIND_STRENGTH) * 15f / 9)); }
			case "anomaly_sensor" -> {
				var p = UniverseProfile.of(level); var zone = UniverseHazards.at(p, Vec3.atCenterOf(pos));
				self.output(zone != null && zone.strength(Vec3.atCenterOf(pos)) >= .35 && (zone.kind() != UniverseHazards.Kind.RIFT || Math.floorMod(now, 120) < 20) ? 15 : 0);
			}
			case "air_purifier" -> {
				if (self.fuel > 0) { self.fuel = Math.max(0, self.fuel - 20); self.setChanged(); }
				if (self.fuel <= 0 && self.consumePair(Items.PAPER, Items.CHARCOAL)) self.fuel = 1200;
				if (self.fuel > 0) FILTERS.put(self, true);
				else FILTERS.remove(self);
			}
			case "growth_lamp" -> self.grow(level);
			case "recycling_anvil" -> self.recycle();
			case "washing_trough" -> self.wash();
			case "drain_pump" -> { if (level.hasNeighborSignal(pos)) self.pump(level); }
			case "rhythm_controller" -> {
				if (self.recording) { self.rhythm[self.step++] = level.getBestNeighborSignal(pos); if (self.step == 8) { self.step = 0; self.recording = false; } self.setChanged(); }
				else if (self.playing) { self.output(Math.clamp(self.rhythm[self.step], 0, 15)); self.step = (self.step + 1) % 8; self.setChanged(); }
			}
		}
	}
	private void output(int value) { if (signal != value) { signal = value; setChanged(); if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock()); } }
	public static boolean filtered(Player player) {
		return FILTERS.keySet().stream().anyMatch(m -> !m.isRemoved() && m.level == player.level() && m.fuel > 0 && m.worldPosition.distToCenterSqr(player.position()) <= 36);
	}
	private boolean consumePair(Item a, Item b) {
		int first = -1, second = -1;
		for (int i = 0; i < items.size(); i++) { if (items.get(i).is(a)) first = i; if (items.get(i).is(b)) second = i; }
		if (first < 0 || second < 0) return false;
		items.get(first).shrink(1); items.get(second).shrink(1); setChanged(); return true;
	}
	private void grow(ServerLevel level) {
		if (fuel <= 0) { var meal = items.stream().filter(s -> s.is(Items.BONE_MEAL)).findFirst(); if (meal.isEmpty()) return; meal.get().shrink(1); fuel = 8; }
		for (int i = 0; i < 162; i++) {
			int index = Math.floorMod(cursor++, 162); BlockPos p = worldPosition.offset(index % 9 - 4, index / 81 - 1, index / 9 % 9 - 4); var state = level.getBlockState(p);
			if (level.hasChunkAt(p) && state.getBlock() instanceof BonemealableBlock plant && (state.getBlock() instanceof CropBlock || state.getBlock() instanceof UniverseCropBlock)
					&& plant.isValidBonemealTarget(level, p, state)) { plant.performBonemeal(level, level.getRandom(), p, state); fuel--; setChanged(); return; }
		}
	}
	private void recycle() {
		ItemStack gear = items.getFirst(); if (gear.isEmpty() || !gear.isDamageableItem()) return;
		int[] units = {2, 3, 3, 1, 2, 5, 8, 7, 4};
		for (int f = 0; f < 10; f++) for (int r = 0; r < UniverseEquipment.ROLES.size(); r++) if (gear.is(UniverseEquipment.item(f, UniverseEquipment.ROLES.get(r)))) {
			int count = (int) Math.floor(units[r] * .35 * (gear.getMaxDamage() - gear.getDamageValue()) / gear.getMaxDamage());
			if (count > 0 && insert(new ItemStack(UniverseMaterials.resource(f, 0), count))) { gear.shrink(1); setChanged(); }
			return;
		}
	}
	private void wash() {
		var input = items.getFirst();
		if (!input.is(net.minecraft.tags.TagKey.create(Registries.ITEM, dev.kirill.hbk.HbkMod.id("alloy_samples"))) || AlloyActions.number(input, "washed", 0) == 1) return;
		int waterSlot = -1; for (int i = 1; i < items.size(); i++) if (items.get(i).is(Items.WATER_BUCKET)) { waterSlot = i; break; }
		if (waterSlot < 0) return;
		ItemStack washed = input.copyWithCount(1); AlloyActions.setNumber(washed, "washed", 1);
		// Washing is optional now; the sample keeps its family and its normal blasting recipe.
		if (!canInsert(washed)) return;
		items.set(waterSlot, new ItemStack(Items.BUCKET)); input.shrink(1); insert(washed); setChanged();
	}
	private boolean canInsert(ItemStack stack) {
		return items.subList(1, items.size()).stream().anyMatch(s -> s.isEmpty() || ItemStack.isSameItemSameComponents(s, stack) && s.getCount() + stack.getCount() <= s.getMaxStackSize());
	}
	private boolean insert(ItemStack stack) {
		for (int i = 1; i < items.size(); i++) {
			var slot = items.get(i);
			if (slot.isEmpty()) { items.set(i, stack.copy()); return true; }
			if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() + stack.getCount() <= slot.getMaxStackSize()) { slot.grow(stack.getCount()); return true; }
		}
		return false;
	}
	private void pump(ServerLevel level) {
		for (Direction direction : Direction.values()) {
			var tank = level.getBlockEntity(worldPosition.relative(direction)); if (!(tank instanceof Container container)) continue;
			int bucketSlot = -1;
			for (int slot = 0; slot < container.getContainerSize(); slot++) if (container.getItem(slot).is(Items.BUCKET) && container.getItem(slot).getCount() == 1 && container.canPlaceItem(slot, new ItemStack(Items.WATER_BUCKET))) { bucketSlot = slot; break; }
			if (bucketSlot < 0) continue;
			for (int i = 0; i < 64; i++) {
				int index = Math.floorMod(cursor++, 405); var p = worldPosition.offset(index % 9 - 4, -(index / 81), index / 9 % 9 - 4);
				if (!level.hasChunkAt(p) || !level.getBlockState(p).is(Blocks.WATER) || !level.getFluidState(p).isSource()) continue;
				if (level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState())) { container.setItem(bucketSlot, new ItemStack(Items.WATER_BUCKET)); container.setChanged(); setChanged(); return; }
			}
		}
	}
	private void transport(ServerLevel level, long now) {
		for (var entity : level.getEntitiesOfClass(Entity.class, new AABB(worldPosition).inflate(0, .6, 0), e -> !e.isPassenger() && e.isAlive())) {
			if (TRANSIT.containsKey(entity)) continue;
			BlockPos target = endpoint;
			if (kind().equals("lift_platform")) {
				boolean down = entity instanceof Player player && player.isShiftKeyDown();
				if (entity instanceof Player p && !down && !(p instanceof net.minecraft.server.level.ServerPlayer serverPlayer ? serverPlayer.getLastClientInput().jump() : p.isJumping())) continue;
				target = null;
				for (int n = 2; n <= 64; n++) {
					BlockPos p = worldPosition.above(down ? -n : n);
					if (!level.hasChunkAt(p)) break;
					if (level.getBlockState(p).is(getBlockState().getBlock())) { target = p; break; }
					if (!level.getBlockState(p).isAir()) break;
				}
			} else if (target != null && (!level.hasChunkAt(target) || !(level.getBlockEntity(target) instanceof AlloyDeviceBlockEntity pair)
					|| !worldPosition.equals(pair.endpoint) || !pair.kind().equals("transition_plate"))) target = null;
			if (target == null || target.distSqr(worldPosition) > 128 * 128) continue;
			Vec3 arrival = new Vec3(target.getX() + .5, target.getY() + .15, target.getZ() + .5);
			if (!level.getWorldBorder().isWithinBounds(target) || !level.noCollision(entity, entity.getBoundingBox().move(arrival.subtract(entity.position())))) continue;
			TRANSIT.put(entity, GlobalPos.of(level.dimension(), target)); entity.teleportTo(arrival.x, arrival.y, arrival.z); entity.resetFallDistance();
		}
	}
}
