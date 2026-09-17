package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.CjEntity;
import dev.kirill.hbk.entity.NkvdEntity;
import dev.kirill.hbk.entity.StalinEntity;
import dev.kirill.hbk.entity.WorkerMobData;
import dev.kirill.hbk.entity.WorkerMobLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SickleAndHammerItem extends Item {
	private static final int AREA_RADIUS = 2;
	private static final int WALL_HALF_WIDTH = 2;
	private static final int WALL_HEIGHT = 3;

	public SickleAndHammerItem(Properties properties) {
		super(properties);
	}

	public static boolean mowArea(ServerLevel level, Player player, BlockPos center, ItemStack tool) {
		if (!hasBothHandsFree(player, tool)) {
			player.sendOverlayMessage(Component.translatable("message.hbk.sickle_and_hammer.two_handed"));
			return false;
		}

		int harvested = 0;
		for (int dx = -AREA_RADIUS; dx <= AREA_RADIUS; dx++) {
			for (int dz = -AREA_RADIUS; dz <= AREA_RADIUS; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos pos = center.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (!isHarvestable(state)) {
						continue;
					}
					Block block = state.getBlock();
					if (!level.destroyBlock(pos, true, player, 512)) {
						continue;
					}
					if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
						level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
					}
					harvested++;
					break;
				}
			}
		}

		if (harvested > 0) {
			tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			level.playSound(null, center, SoundEvents.CROP_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX() + 0.5, center.getY() + 0.8, center.getZ() + 0.5, Math.min(24, harvested), 1.5, 0.4, 1.5, 0.02);
		}
		return harvested > 0;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		ItemStack tool = context.getItemInHand();
		if (!hasBothHandsFree(player, tool)) {
			player.sendOverlayMessage(Component.translatable("message.hbk.sickle_and_hammer.two_handed"));
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}

		BlockPos center = context.getClickedPos().relative(context.getClickedFace());
		Direction widthDirection = context.getHorizontalDirection().getClockWise();
		int placed = 0;
		for (int horizontal = -WALL_HALF_WIDTH; horizontal <= WALL_HALF_WIDTH; horizontal++) {
			for (int vertical = 0; vertical < WALL_HEIGHT; vertical++) {
				BlockPos pos = center.relative(widthDirection, horizontal).above(vertical);
				if (!level.mayInteract(player, pos) || !level.getBlockState(pos).canBeReplaced()) {
					continue;
				}
				if (level.setBlockAndUpdate(pos, Blocks.BRICKS.defaultBlockState())) {
					placed++;
				}
			}
		}
		if (placed == 0) {
			return InteractionResult.FAIL;
		}

		tool.hurtAndBreak(1, player, context.getHand());
		level.playSound(null, center, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
		player.swing(context.getHand(), true);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!hasBothHandsFree(player, stack)) {
			player.sendOverlayMessage(Component.translatable("message.hbk.sickle_and_hammer.two_handed"));
			return InteractionResult.FAIL;
		}
		if (!(target instanceof Mob mob) || mob instanceof Enemy || mob instanceof StalinEntity || mob instanceof CjEntity || mob instanceof NkvdEntity) {
			player.sendOverlayMessage(Component.translatable("message.hbk.worker.not_neutral"));
			return InteractionResult.FAIL;
		}
		WorkerMobData data = (WorkerMobData) mob;
		if (data.hbk$getWorkerOwner() != null) {
			player.sendOverlayMessage(Component.translatable("message.hbk.worker.already_conscious"));
			return InteractionResult.FAIL;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		WorkerMobLogic.makeConscious(mob, (ServerPlayer) player);
		stack.hurtAndBreak(1, player, hand);
		Level level = player.level();
		level.playSound(null, mob.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
		((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 20, 0.45, 0.6, 0.45, 0.05);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static boolean hasBothHandsFree(Player player, ItemStack tool) {
		return player.getMainHandItem() == tool && player.getOffhandItem().isEmpty();
	}

	private static boolean isHarvestable(BlockState state) {
		if (state.is(BlockTags.CROPS)) {
			return !(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state);
		}
		return state.is(Blocks.SHORT_GRASS)
				|| state.is(Blocks.TALL_GRASS)
				|| state.is(Blocks.FERN)
				|| state.is(Blocks.LARGE_FERN)
				|| state.is(Blocks.SHORT_DRY_GRASS)
				|| state.is(Blocks.TALL_DRY_GRASS)
				|| state.is(Blocks.DEAD_BUSH);
	}
}
