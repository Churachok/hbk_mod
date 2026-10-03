package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.NkvdEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reusable trophy from Stalin's dacha: summons a three-man protective patrol. */
public final class StalinPipeItem extends Item {
	public StalinPipeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
				player.getX(), player.getEyeY(), player.getZ(), 24, 0.45, 0.2, 0.45, 0.025);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.CAMPFIRE_CRACKLE,
				SoundSource.PLAYERS, 0.8f, 0.7f);
		int spawned = 0;
		for (int i = 0; i < 3; i++) {
			double angle = (i + 0.5) * Math.PI * 2.0 / 3.0 + Math.toRadians(player.getYRot());
			BlockPos center = player.blockPosition().offset(
					(int) Math.round(Math.cos(angle) * 3), 0,
					(int) Math.round(Math.sin(angle) * 3));
			for (int dy = 2; dy >= -2; dy--) {
				BlockPos pos = center.offset(0, dy, 0);
				if (!serverLevel.getBlockState(pos.below()).isSolid()
						|| !serverLevel.getBlockState(pos).isAir()
						|| !serverLevel.getBlockState(pos.above()).isAir()) {
					continue;
				}
				NkvdEntity guard = ModEntityTypes.NKVD.spawn(serverLevel, pos, EntitySpawnReason.REINFORCEMENT);
				if (guard != null) {
					guard.setGuardOwner(player);
					spawned++;
				}
				break;
			}
		}
		if (spawned > 0) {
			player.getCooldowns().addCooldown(stack, 20 * 60);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
