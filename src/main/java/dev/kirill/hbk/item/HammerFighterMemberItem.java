package dev.kirill.hbk.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/** A sword that builds the same 5x3 wall pattern as the Sickle and Hammer. */
public final class HammerFighterMemberItem extends Item {
	private static final int WALL_HALF_WIDTH = 2;
	private static final int WALL_HEIGHT = 3;

	public HammerFighterMemberItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
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
				if (level.setBlockAndUpdate(pos, Blocks.STONE_BRICKS.defaultBlockState())) {
					placed++;
				}
			}
		}
		if (placed == 0) {
			return InteractionResult.FAIL;
		}

		context.getItemInHand().hurtAndBreak(1, player, context.getHand());
		level.playSound(null, center, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
		player.swing(context.getHand(), true);
		return InteractionResult.SUCCESS_SERVER;
	}
}
