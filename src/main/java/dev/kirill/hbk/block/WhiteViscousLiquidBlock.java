package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A still pearly flood that heavily slows anything wading through it. */
public final class WhiteViscousLiquidBlock extends HalfTransparentBlock {
	public static final MapCodec<WhiteViscousLiquidBlock> CODEC = simpleCodec(WhiteViscousLiquidBlock::new);

	public WhiteViscousLiquidBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<WhiteViscousLiquidBlock> codec() {
		return CODEC;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean intersects) {
		if (intersects) {
			entity.makeStuckInBlock(state, new Vec3(0.34, 0.12, 0.34));
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(12) == 0) {
			level.addParticle(ParticleTypes.WHITE_ASH,
					pos.getX() + random.nextDouble(), pos.getY() + 0.88, pos.getZ() + random.nextDouble(),
					0.0, 0.005, 0.0);
		}
	}
}
