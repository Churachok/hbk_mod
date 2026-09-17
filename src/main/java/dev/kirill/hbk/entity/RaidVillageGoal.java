package dev.kirill.hbk.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class RaidVillageGoal extends Goal {
	private static final int SCAN_RADIUS = 10;
	private final NkvdEntity nkvd;
	private int cooldown;

	public RaidVillageGoal(NkvdEntity nkvd) {
		this.nkvd = nkvd;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.nkvd.isVillageRaider() && this.nkvd.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void tick() {
		if (--this.cooldown > 0) {
			return;
		}
		this.cooldown = 25;
		if (!(this.nkvd.level() instanceof ServerLevel level)) {
			return;
		}

		BlockPos origin = this.nkvd.blockPosition();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
			for (int dy = -2; dy <= 3; dy++) {
				for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
					cursor.setWithOffset(origin, dx, dy, dz);
					BlockState state = level.getBlockState(cursor);
					if (this.shouldBreak(state)) {
						level.destroyBlock(cursor, true, this.nkvd);
						return;
					}
				}
			}
		}
	}

	private boolean shouldBreak(BlockState state) {
		if (state.is(BlockTags.CROPS) || state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
			return true;
		}
		return state.getBlock() instanceof ChestBlock;
	}
}
