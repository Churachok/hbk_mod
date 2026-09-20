package dev.kirill.hbk.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared, bounded explosions: no self-damage, fire, or bypass of build permissions. */
public final class MemberDestruction {
	private MemberDestruction() {
	}

	public static Vec3 aimedPosition(ServerLevel level, Player player, double range) {
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getLookAngle().scale(range));
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player)).getLocation();
	}

	public static void explode(ServerLevel level, Player player, Vec3 position, float power) {
		if (player.isSpectator() || !level.isLoaded(BlockPos.containing(position))
				|| !level.getWorldBorder().isWithinBounds(BlockPos.containing(position))) {
			return;
		}
		ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
			@Override
			public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
				return entity != player && super.shouldDamageEntity(explosion, entity);
			}

			@Override
			public float getKnockbackMultiplier(Entity entity) {
				return entity == player ? 0.0f : super.getKnockbackMultiplier(entity);
			}

			@Override
			public boolean shouldBlockExplode(Explosion explosion, BlockGetter blocks,
					BlockPos pos, BlockState state, float strength) {
				return player.mayBuild() && level.getWorldBorder().isWithinBounds(pos)
						&& level.mayInteract(player, pos) && state.getDestroySpeed(blocks, pos) >= 0
						&& super.shouldBlockExplode(explosion, blocks, pos, state, strength);
			}
		};
		level.explode(player, null, calculator, position, power, false, Level.ExplosionInteraction.TNT);
	}
}
