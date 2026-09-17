package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A still, glowing chemical pool. It behaves as a dangerous non-flowing liquid block. */
public final class ToxicWaterBlock extends HalfTransparentBlock {
	public static final MapCodec<ToxicWaterBlock> CODEC = simpleCodec(ToxicWaterBlock::new);

	public ToxicWaterBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<ToxicWaterBlock> codec() {
		return CODEC;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean intersects) {
		if (intersects && level instanceof ServerLevel serverLevel && entity instanceof LivingEntity living
				&& living.tickCount % 20 == 0) {
			living.hurtServer(serverLevel, level.damageSources().magic(), 2.0f);
			living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(5) == 0) {
			level.addParticle(ParticleTypes.NOXIOUS_GAS,
					pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(),
					0.0, 0.015, 0.0);
		}
	}
}
