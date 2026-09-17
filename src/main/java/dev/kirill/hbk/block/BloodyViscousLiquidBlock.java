package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The biome's thick blood-streaked flood: slow to cross and mildly harmful. */
public final class BloodyViscousLiquidBlock extends HalfTransparentBlock {
	public static final MapCodec<BloodyViscousLiquidBlock> CODEC = simpleCodec(BloodyViscousLiquidBlock::new);

	public BloodyViscousLiquidBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<BloodyViscousLiquidBlock> codec() {
		return CODEC;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean intersects) {
		if (!intersects) {
			return;
		}
		entity.makeStuckInBlock(state, new Vec3(0.28, 0.09, 0.28));
		if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity living
				&& living.tickCount % 30 == 0) {
			living.hurtServer(serverLevel, level.damageSources().magic(), 1.0f);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(10) == 0) {
			level.addParticle(ParticleTypes.LANDING_LAVA,
					pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(),
					0.0, 0.0, 0.0);
		}
	}
}
