package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import dev.kirill.hbk.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A fixed-strength trampoline; the next landing is safe even away from this block. */
public final class RoflBlock extends Block {
	public static final MapCodec<RoflBlock> CODEC = simpleCodec(RoflBlock::new);
	// Vanilla living-entity gravity and drag give an apex of about 25 blocks.
	public static final double LAUNCH_SPEED = 2.3;

	public RoflBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<RoflBlock> codec() {
		return CODEC;
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
		entity.resetFallDistance();
	}

	public static void launch(Entity entity) {
		var movement = entity.getDeltaMovement();
		entity.setDeltaMovement(movement.x, LAUNCH_SPEED, movement.z);
		entity.setOnGround(false);
		entity.resetFallDistance();
		entity.setAttached(ModAttachments.ROFL_SAFE_FALL, true);
		entity.hurtMarked = true;
	}
}
