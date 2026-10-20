package dev.kirill.hbk.universe;

import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import java.util.*;

/** Bounded actions, native mining/placement checks and per-stack persistent state. */
public final class AlloyActions {
	private static final ThreadLocal<Boolean> MINING = ThreadLocal.withInitial(() -> false);
	private static final Map<UUID, AirJump> JUMPS = new HashMap<>();
	private record AirJump(boolean pressed, boolean spent, int airborne) {}
	private AlloyActions() {}
	public static int number(ItemStack stack, String key, int fallback) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("hbk_" + key, fallback);
	}
	public static void setNumber(ItemStack stack, String key, int value) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putInt("hbk_" + key, value));
	}
	public static boolean equipped(LivingEntity entity, EquipmentSlot slot, String kind) { return UniverseAlloys.is(entity.getItemBySlot(slot), kind); }
	public static boolean ordinaryRock(BlockState state) {
		return !state.is(UniverseMining.ORES) && (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
				|| state.getBlock() instanceof UniverseRockBlock);
	}
	private static boolean allowed(ServerLevel level, Player player, BlockPos pos) {
		return player.mayBuild() && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
				&& level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
	}
	public static void mine(String kind, ItemStack tool, ServerPlayer player, BlockState original, BlockPos origin) {
		if (MINING.get() || player.isShiftKeyDown() || tool.isEmpty()) return;
		var level = player.level();
		MINING.set(true);
		try {
			if (kind.equals("drill_pickaxe") && ordinaryRock(original)) {
				var axis = Direction.getApproximateNearest(player.getLookAngle()).getAxis();
				for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
					BlockPos p = origin.offset(axis == Direction.Axis.X ? 0 : a, axis == Direction.Axis.Y ? 0 : axis == Direction.Axis.X ? a : b, axis == Direction.Axis.Z ? 0 : b);
					if (p.equals(origin) || tool.isEmpty()) continue;
					if (ordinaryRock(level.getBlockState(p)) && allowed(level, player, p) && level.getBlockEntity(p) == null) player.gameMode.destroyBlock(p);
				}
			} else if (kind.equals("forest_axe") && original.is(BlockTags.LOGS) && !AlloyWorldData.get(level).placedLog(origin)) {
				// Local trees have their own living soil. Built logs never enter this branch.
				int family = UniverseBotany.family(original.getBlock());
				if (family < 0 || !original.is(UniverseBotany.block(family, "log"))) return;
				Set<BlockPos> connected = new LinkedHashSet<>(); var queue = new ArrayDeque<BlockPos>(); queue.add(origin); connected.add(origin);
				boolean foliage = false, soil = false, placed = false;
				while (!queue.isEmpty() && connected.size() <= 64) {
					var p = queue.removeFirst();
					for (Direction dir : Direction.values()) {
						var next = p.relative(dir); var state = level.getBlockState(next);
						foliage |= state.is(UniverseBotany.block(family, "leaves")) && !state.getValue(LeavesBlock.PERSISTENT);
						soil |= state.is(UniverseBotany.block(family, "soil")) || state.is(UniverseMaterials.loam(family));
						if (state.is(original.getBlock()) && connected.add(next)) { queue.add(next); placed |= AlloyWorldData.get(level).placedLog(next); }
					}
				}
				if (!foliage || !soil || placed || connected.size() > 64) return;
				for (var p : connected) if (!p.equals(origin) && !tool.isEmpty() && allowed(level, player, p)) player.gameMode.destroyBlock(p);
			}
		} finally { MINING.set(false); }
	}
	public static InteractionResult use(String kind, Level world, Player player, InteractionHand hand) {
		if (!(world instanceof ServerLevel level)) return activeUse(kind) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
		switch (kind) {
			case "rift_sword" -> {
				Vec3 direction = player.getLookAngle(); Vec3 start = player.position(), end = start;
				for (double d = .25; d <= 4; d += .25) {
					Vec3 candidate = start.add(direction.scale(d));
					if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(candidate)) || !level.noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))) break;
					end = candidate;
				}
				if (end.distanceToSqr(start) < .1) return InteractionResult.FAIL;
				player.teleportTo(end.x, end.y, end.z); stack.hurtAndBreak(4, player, hand); cooldown(player, stack, 60);
			}
			case "grappling_hook" -> {
				Vec3 eye = player.getEyePosition(); var hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(16)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
				if (hit.getType() != HitResult.Type.BLOCK || !level.getBlockState(hit.getBlockPos()).isSolid()) return InteractionResult.FAIL;
				Vec3 pull = hit.getLocation().subtract(player.position());
				if (pull.lengthSqr() < 4) return InteractionResult.FAIL;
				player.setDeltaMovement(pull.normalize().scale(1.05)); sync(player); stack.hurtAndBreak(2, player, hand); cooldown(player, stack, 30);
			}
			case "building_glove" -> {
				BlockPos target = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(Math.min(4, player.blockInteractionRange() - .5))));
				if (!placeInventoryBlock(level, player, hand, target, Direction.UP)) return InteractionResult.FAIL;
				stack.hurtAndBreak(1, player, hand); cooldown(player, stack, 5);
			}
			case "tuning_fork" -> {
				for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(5), m -> m.isAlive() && !m.isAlliedTo(player))) {
					Vec3 delta = mob.position().subtract(player.position());
					if (delta.lengthSqr() > 25 || !player.hasLineOfSight(mob)) continue;
					mob.push(delta.normalize().scale(.9).add(0, .25, 0));
				}
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, player.getX(), player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
				stack.hurtAndBreak(3, player, hand); cooldown(player, stack, 100);
			}
			case "field_repair_kit" -> {
				var other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
				if (other.isEmpty() || !other.isDamaged() || other == stack) return InteractionResult.FAIL;
				ItemStack material = find(player, s -> other.isValidRepairItem(s));
				if (material.isEmpty()) return message(player, "repair_material");
				other.setDamageValue(Math.max(0, other.getDamageValue() - Math.max(1, other.getMaxDamage() / 4)));
				material.shrink(1); stack.hurtAndBreak(8, player, hand); cooldown(player, stack, 10);
			}
			case "miner_belt" -> { setNumber(stack, "enabled", number(stack, "enabled", 1) == 1 ? 0 : 1); message(player, number(stack, "enabled", 1) == 1 ? "enabled" : "disabled"); }
			case "sample_bag" -> openBag(player, stack);
			case "expedition_barometer" -> forecast(player);
			case "entry_compass", "ore_compass" -> { updateCompass(player, stack, kind.equals("ore_compass")); cooldown(player, stack, 40); }
			case "return_capsule" -> {
				if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
				BlockPos target = entrance(serverPlayer);
				if (target == null) return message(player, "no_entrance");
				level.getChunkAt(target);
				if (!(level.getBlockEntity(target) instanceof TimeFurnaceBlockEntity)) return message(player, "no_entrance");
				BlockPos safe = UniverseManager.safeArrival(level, target);
				if (safe == null || safe.distSqr(target) > 64) return message(player, "blocked");
				player.teleportTo(safe.getX() + .5, safe.getY(), safe.getZ() + .5); if (!player.hasInfiniteMaterials()) stack.shrink(1);
			}
			case "rope_ladder_spool" -> { return retractLadder(level, player, stack, hand); }
			default -> { return InteractionResult.PASS; }
		}
		return InteractionResult.SUCCESS_SERVER;
	}
	private static boolean activeUse(String kind) {
		return Set.of("rift_sword", "grappling_hook", "building_glove", "tuning_fork", "field_repair_kit", "miner_belt", "sample_bag", "expedition_barometer", "entry_compass", "ore_compass", "return_capsule", "rope_ladder_spool").contains(kind);
	}
	public static InteractionResult useOn(String kind, UseOnContext context) {
		Player player = context.getPlayer(); if (player == null) return InteractionResult.PASS;
		if (Set.of("field_hoe", "field_shovel").contains(kind)) return till(kind, context);
		if (kind.equals("forest_axe")) return Items.NETHERITE_AXE.useOn(context);
		if (!(context.getLevel() instanceof ServerLevel level)) return Set.of("row_builder", "harvest_sickle", "carrying_grips", "heat_cartridge", "seismic_hammer", "ore_compass", "rope_ladder_spool", "shaped_charge").contains(kind) || activeUse(kind) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		var stack = context.getItemInHand(); var pos = context.getClickedPos();
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (!allowed(level, player, pos)) return InteractionResult.FAIL;
		switch (kind) {
			case "row_builder" -> {
				int count = 0;
				for (int i = 1; i <= (player.isShiftKeyDown() ? 1 : 8) && !stack.isEmpty(); i++) {
					if (!placeInventoryBlock(level, player, context.getHand(), pos.relative(context.getClickedFace(), i), context.getClickedFace(), true)) break;
					stack.hurtAndBreak(1, player, context.getHand()); count++;
				}
				return count > 0 ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
			}
			case "harvest_sickle" -> { return harvest(level, player, context); }
			case "carrying_grips" -> { return carry(level, player, context); }
			case "heat_cartridge" -> {
				if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) return InteractionResult.PASS;
				var access = (dev.kirill.hbk.mixin.AlloyFurnaceAccess) furnace;
				int stored = number(stack, "heat", 0), lit = access.hbk$getHeat();
				if (stored == 0 && lit > 0) { setNumber(stack, "heat", lit); setNumber(stack, "heat_total", access.hbk$getHeatTotal()); access.hbk$setHeat(0); }
				else if (stored > 0 && lit == 0) { access.hbk$setHeat(stored); access.hbk$setHeatTotal(number(stack, "heat_total", stored)); setNumber(stack, "heat", 0); }
				else return InteractionResult.FAIL;
				level.setBlockAndUpdate(pos, furnace.getBlockState().setValue(AbstractFurnaceBlock.LIT, access.hbk$getHeat() > 0));
				furnace.setChanged(); return InteractionResult.SUCCESS_SERVER;
			}
			case "seismic_hammer" -> {
				if (!ordinaryRock(level.getBlockState(pos))) return InteractionResult.FAIL;
				BlockPos nearest = nearest(level, pos, 12, p -> level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
						&& !level.canSeeSky(p) && level.getBlockState(p.below()).isSolid());
				point(player, nearest); stack.hurtAndBreak(2, player, context.getHand()); cooldown(player, stack, 40); return InteractionResult.SUCCESS_SERVER;
			}
			case "ore_compass" -> {
				BlockState state = level.getBlockState(pos);
				if (!state.is(UniverseMining.ORES)) return message(player, "ore_sample");
				CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putString("hbk_ore", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
				updateCompass(player, stack, true); return InteractionResult.SUCCESS_SERVER;
			}
			case "rope_ladder_spool" -> {
				if (player.isShiftKeyDown()) return retractLadder(level, player, stack, context.getHand());
				if (number(stack, "ladder_length", 0) > 0 || context.getClickedFace().getAxis().isVertical()) return InteractionResult.FAIL;
				BlockPos top = pos.relative(context.getClickedFace()); int count = 0;
				for (int i = 0; i < 24 && stack.getDamageValue() + count + 1 < stack.getMaxDamage(); i++) {
					BlockPos p = top.below(i);
					var rope = UniverseAlloys.ROPE_LADDER.defaultBlockState().setValue(LadderBlock.FACING, context.getClickedFace());
					if (!allowed(level, player, p) || !level.getBlockState(p).isAir() || !rope.canSurvive(level, p)) break;
					level.setBlockAndUpdate(p, rope); count++;
				}
				if (count == 0) return InteractionResult.FAIL;
				CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> { nbt.putLong("hbk_ladder_top", top.asLong()); nbt.putString("hbk_ladder_world", level.dimension().identifier().toString()); });
				setNumber(stack, "ladder_length", count); stack.hurtAndBreak(count, player, context.getHand()); return InteractionResult.SUCCESS_SERVER;
			}
			case "shaped_charge" -> {
				int removed = 0;
				for (var p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
					if (ordinaryRock(level.getBlockState(p)) && level.getBlockEntity(p) == null && allowed(level, player, p)
							&& level.destroyBlock(p, true, player)) removed++;
				}
				if (removed == 0) return InteractionResult.FAIL;
				if (!player.hasInfiniteMaterials()) stack.shrink(1);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 3, .5, .5, .5, 0);
				return InteractionResult.SUCCESS_SERVER;
			}
			default -> { return use(kind, level, player, context.getHand()); }
		}
	}
	private static InteractionResult till(String kind, UseOnContext context) {
		Player player = context.getPlayer(); if (player == null) return InteractionResult.PASS;
		Item delegate = kind.equals("field_hoe") ? Items.NETHERITE_HOE : Items.NETHERITE_SHOVEL;
		if (!(context.getLevel() instanceof ServerLevel level)) return delegate.useOn(context);
		boolean success = false;
		int radius = player.isShiftKeyDown() ? 0 : 1;
		for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
			var p = context.getClickedPos().offset(x, 0, z);
			if (context.getItemInHand().isEmpty() || !allowed(level, player, p)) continue;
			var hit = new BlockHitResult(Vec3.atCenterOf(p), context.getClickedFace(), p, false);
			if (delegate.useOn(new UseOnContext(player, context.getHand(), hit)).consumesAction()) success = true;
		}
		return success ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
	}
	private static InteractionResult harvest(ServerLevel level, Player player, UseOnContext context) {
		boolean success = false; var tool = context.getItemInHand();
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
			var p = context.getClickedPos().offset(x, 0, z); var state = level.getBlockState(p);
			if (!allowed(level, player, p) || tool.isEmpty()) continue;
			if (state.getBlock() instanceof UniverseCropBlock && state.getValue(UniverseCropBlock.AGE) == 3) {
				state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
				tool.hurtAndBreak(1, player, context.getHand()); success = true; continue;
			}
			if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) continue;
			var drops = Block.getDrops(state, level, p, null, player, tool);
			Item seed = state.getCloneItemStack(level, p, false).getItem();
			var seeds = find(player, s -> s.is(seed));
			if (seeds.isEmpty()) continue;
			seeds.shrink(1); level.setBlockAndUpdate(p, crop.getStateForAge(0));
			for (var drop : drops) Block.popResource(level, p, drop);
			tool.hurtAndBreak(1, player, context.getHand()); success = true;
		}
		return success ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
	}
	public static boolean placeInventoryBlock(ServerLevel level, Player player, InteractionHand hand, BlockPos target, Direction face) {
		return placeInventoryBlock(level, player, hand, target, face, false);
	}
	private static boolean placeInventoryBlock(ServerLevel level, Player player, InteractionHand hand, BlockPos target, Direction face, boolean extend) {
		if (!allowed(level, player, target) || !level.getBlockState(target).isAir() || !extend && !player.isWithinBlockInteractionRange(target, 0)) return false;
		ItemStack blocks = find(player, s -> s.getItem() instanceof BlockItem block && !block.getBlock().defaultBlockState().getCollisionShape(level, target).isEmpty()
				&& block.getBlock().defaultBlockState().isCollisionShapeFullBlock(level, target) && !(block.getBlock() instanceof EntityBlock));
		if (!(blocks.getItem() instanceof BlockItem block)) return false;
		var context = new BlockPlaceContext(player, hand, blocks, new BlockHitResult(Vec3.atCenterOf(target), face, target, false));
		return block.place(context).consumesAction();
	}
	private static InteractionResult carry(ServerLevel level, Player player, UseOnContext context) {
		var tool = context.getItemInHand(); var pos = context.getClickedPos();
		var data = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (tool.has(DataComponents.BLOCK_ENTITY_DATA)) {
			BlockPos target = pos.relative(context.getClickedFace());
			if (!allowed(level, player, target) || !level.getBlockState(target).isAir() || !level.noCollision(player, new AABB(target))) return InteractionResult.FAIL;
			Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(data.getStringOr("hbk_carried_block", "minecraft:barrel")));
			var state = block.defaultBlockState();
			var savedData = tool.get(DataComponents.BLOCK_ENTITY_DATA);
			var savedTag = savedData.copyTagWithoutId(); savedTag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(savedData.type()).toString());
			var restored = BlockEntity.loadStatic(target, state, savedTag, level.registryAccess());
			if (restored == null || !level.setBlockAndUpdate(target, state)) return InteractionResult.FAIL;
			level.setBlockEntity(restored); restored.setChanged(); tool.remove(DataComponents.BLOCK_ENTITY_DATA); setNumber(tool, "carrying", 0);
			tool.hurtAndBreak(8, player, context.getHand()); return InteractionResult.SUCCESS_SERVER;
		}
		var be = level.getBlockEntity(pos); var state = level.getBlockState(pos);
		if (!(be instanceof BaseContainerBlockEntity container) || !(state.is(Blocks.CHEST) || state.is(Blocks.BARREL)) || container.isLocked()
				|| state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
				|| be instanceof RandomizableContainerBlockEntity random && random.getLootTable() != null) return InteractionResult.FAIL;
		var saved = be.saveWithFullMetadata(level.registryAccess());
		container.clearContent();
		if (!level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())) { level.setBlockEntity(BlockEntity.loadStatic(pos, state, saved, level.registryAccess())); return InteractionResult.FAIL; }
		tool.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(be.getType(), saved));
		CustomData.update(DataComponents.CUSTOM_DATA, tool, nbt -> nbt.putString("hbk_carried_block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
		setNumber(tool, "carrying", 1); return InteractionResult.SUCCESS_SERVER;
	}
	private static InteractionResult retractLadder(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
		var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); int length = number(stack, "ladder_length", 0);
		if (length == 0 || !level.dimension().identifier().toString().equals(data.getStringOr("hbk_ladder_world", ""))) return InteractionResult.PASS;
		var top = BlockPos.of(data.getLongOr("hbk_ladder_top", 0));
		if (!player.isWithinBlockInteractionRange(top, 1)) return InteractionResult.FAIL;
		for (int i = 0; i < length; i++) {
			var p = top.below(i);
			if (allowed(level, player, p) && level.getBlockState(p).is(UniverseAlloys.ROPE_LADDER)) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
		}
		setNumber(stack, "ladder_length", 0); return InteractionResult.SUCCESS_SERVER;
	}
	private static void openBag(Player player, ItemStack stack) {
		var bag = new SimpleContainer(18) {
			@Override public void setChanged() { stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems())); }
			@Override public boolean canPlaceItem(int slot, ItemStack value) { return value.is(UniverseAlloys.BAG_CONTENTS) && value.getItem().canFitInsideContainerItems(); }
			@Override public boolean stillValid(Player p) { for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i) == stack) return true; return false; }
		};
		var contents = NonNullList.withSize(18, ItemStack.EMPTY); stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
		for (int i = 0; i < 18; i++) bag.getItems().set(i, contents.get(i));
		player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inv, bag, 2) {
			{ for (int i = 0; i < slots.size(); i++) {
				Slot old = slots.get(i); int index = i;
				Slot replacement = new Slot(old.container, old.getContainerSlot(), old.x, old.y) {
					@Override public boolean mayPlace(ItemStack value) { return old.getItem() != stack && (index >= 18 || bag.canPlaceItem(index, value)); }
					@Override public boolean mayPickup(Player owner) { return old.getItem() != stack; }
				}; replacement.index = i; slots.set(i, replacement);
			} }
		}, stack.getHoverName()));
	}
	public static void inventoryTick(String kind, ItemStack stack, Player player, EquipmentSlot slot) {
		if (player.tickCount % 20 != 0) return;
		boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
		switch (kind) {
			case "mining_helmet" -> {
				if (slot != EquipmentSlot.HEAD) return;
				int charge = number(stack, "light", 0);
				if (charge <= 0) { var fuel = find(player, s -> s.is(Items.GLOWSTONE_DUST) || s.is(Items.GLOW_INK_SAC)); if (fuel.isEmpty()) return; fuel.shrink(1); charge = 6000; }
				setNumber(stack, "light", charge - 20); player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
			}
			case "air_tank" -> {
				if (!held) return;
				int air = number(stack, "air", 1200);
				if (!player.isUnderWater()) setNumber(stack, "air", Math.min(1200, air + 80));
				else if (air > 0) { player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 20)); setNumber(stack, "air", air - 20); }
			}
			case "carrying_grips" -> { if (stack.has(DataComponents.BLOCK_ENTITY_DATA)) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, true, false)); }
			case "entry_compass" -> { if (held) updateCompass(player, stack, false); }
			case "miner_belt" -> {
				if (number(stack, "enabled", 1) == 0 || number(stack, "last_tick", -1) == player.tickCount) return;
				setNumber(stack, "last_tick", player.tickCount);
				for (var drop : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5), e -> !e.hasPickUpDelay())) {
					// Private loot is approached only if this player can actually pick it up.
					var target = ((dev.kirill.hbk.mixin.AlloyItemEntityAccess) drop).hbk$pickupTarget();
					if (target != null && !target.equals(player.getUUID()) || drop.getOwner() != null && drop.getOwner() != player) continue;
					if (!player.hasLineOfSight(drop)) continue;
					drop.setDeltaMovement(player.position().add(0, .5, 0).subtract(drop.position()).normalize().scale(.3));
				}
			}
		}
	}
	public static void register() {
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator() || !(player.getItemInHand(hand).getItem() instanceof AlloyItem item)) return InteractionResult.PASS;
			if (Set.of("heat_cartridge", "carrying_grips", "harvest_sickle", "seismic_hammer", "ore_compass", "rope_ladder_spool", "row_builder", "shaped_charge").contains(item.kind()))
				return useOn(item.kind(), new UseOnContext(player, hand, hit));
			return InteractionResult.PASS;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> { JUMPS.clear(); AlloyDeviceBlockEntity.clearCaches(); });
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (level instanceof ServerLevel serverLevel && state.is(BlockTags.LOGS)) AlloyWorldData.get(serverLevel).forgetLog(pos);
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> JUMPS.remove(handler.player.getUUID()));
		ServerTickEvents.END_SERVER_TICK.register(server -> { AlloyDeviceBlockEntity.releaseDepartedEntities(); for (var player : server.getPlayerList().getPlayers()) mobility(player); });
	}
	public static void mobility(ServerPlayer player) {
		boolean pressed = player.getLastClientInput().jump();
		var previous = JUMPS.getOrDefault(player.getUUID(), new AirJump(false, false, 0));
		boolean ground = player.onGround() || player.isInWater() || player.onClimbable() || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
		boolean spent = !ground && previous.spent; int airborne = ground ? 0 : previous.airborne + 1;
		if (!ground && airborne > 2 && pressed && !previous.pressed && !spent && player.isAlive() && !player.isSpectator() && equipped(player, EquipmentSlot.FEET, "sail_boots")) {
			var motion = player.getDeltaMovement(); player.setDeltaMovement(motion.x, .48, motion.z); player.resetFallDistance();
			player.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(2, player, EquipmentSlot.FEET); sync(player); spent = true;
		}
		if (!ground && pressed && equipped(player, EquipmentSlot.CHEST, "gliding_chestplate") && player.getDeltaMovement().y < -.18) {
			var motion = player.getDeltaMovement(); player.setDeltaMovement(motion.x, -.18, motion.z); player.resetFallDistance(); sync(player);
			if (player.tickCount % 20 == 0) player.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(1, player, EquipmentSlot.CHEST);
		}
		if (player.isUnderWater() && equipped(player, EquipmentSlot.FEET, "diving_boots")) {
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 25, 0, true, false));
			if (player.isShiftKeyDown()) { var v = player.getDeltaMovement(); player.setDeltaMovement(v.x * .8, Math.min(-.12, v.y), v.z * .8); sync(player); }
		}
		JUMPS.put(player.getUUID(), new AirJump(pressed, spent, airborne));
	}
	private static BlockPos entrance(ServerPlayer player) {
		BlockPos target = AlloyWorldData.get(player.level()).entrance(player);
		if (target != null && (!player.level().hasChunkAt(target) || player.level().getBlockEntity(target) instanceof TimeFurnaceBlockEntity)) return target;
		return UniverseArrival.findReturnFurnace(player.level(), player.blockPosition());
	}
	private static void updateCompass(Player player, ItemStack stack, boolean ore) {
		if (!(player instanceof ServerPlayer serverPlayer)) return;
		BlockPos target;
		if (ore) {
			String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("hbk_ore", "");
			if (id.isEmpty()) { message(player, "ore_sample"); return; }
			var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
			target = nearest(serverPlayer.level(), player.blockPosition(), 16, p -> serverPlayer.level().getBlockState(p).is(block));
			point(player, target);
		} else target = entrance(serverPlayer);
		stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.ofNullable(target).map(p -> GlobalPos.of(player.level().dimension(), p)), false));
		if (target == null) message(player, ore ? "not_found" : "no_entrance");
	}
	private static void forecast(Player player) {
		var profile = UniverseProfile.of(player.level());
		if (profile == null) { message(player, "ordinary_weather"); return; }
		long now = player.level().getGameTime(), boundary = Math.floorDiv(now, 6000) * 6000 + 6000;
		boolean rain = UniverseWeather.rain(profile, now) > 0; long change = -1;
		for (int i = 0; i < 24; i++) if ((UniverseWeather.rain(profile, boundary + i * 6000) > 0) != rain) { change = (boundary + i * 6000 - now) / 20; break; }
		player.sendOverlayMessage(Component.translatable("message.hbk.alloy.forecast", Component.translatable(rain ? "message.hbk.alloy.rain" : "message.hbk.alloy.calm"), change < 0 ? "—" : change));
	}
	private static BlockPos nearest(ServerLevel level, BlockPos origin, int radius, java.util.function.Predicate<BlockPos> predicate) {
		BlockPos best = null; double distance = Double.MAX_VALUE;
		for (var p : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
			double d = p.distSqr(origin);
			if (d < distance && level.hasChunkAt(p) && predicate.test(p)) { best = p.immutable(); distance = d; }
		}
		return best;
	}
	private static void point(Player player, BlockPos target) {
		if (target == null) { message(player, "not_found"); return; }
		player.sendOverlayMessage(Component.translatable("message.hbk.alloy.coordinates", target.getX(), target.getY(), target.getZ()));
	}
	static ItemStack find(Player player, java.util.function.Predicate<ItemStack> predicate) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) { var stack = player.getInventory().getItem(i); if (!stack.isEmpty() && predicate.test(stack)) return stack; }
		return ItemStack.EMPTY;
	}
	static InteractionResult message(Player player, String key) { player.sendOverlayMessage(Component.translatable("message.hbk.alloy." + key)); return InteractionResult.SUCCESS_SERVER; }
	static void cooldown(Player player, ItemStack stack, int ticks) { player.getCooldowns().addCooldown(stack, ticks); }
	static void sync(Player player) { player.hurtMarked = true; if (player instanceof ServerPlayer serverPlayer) serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player)); }
}
