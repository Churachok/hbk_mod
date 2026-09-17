package dev.kirill.hbk.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A sword whose successful melee hit launches its wielder like Wind Burst III. */
public final class CarrierMemberItem extends Item {
	private static final double WIND_BURST_III_STRENGTH = 2.2;

	public CarrierMemberItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker instanceof ServerPlayer serverPlayer)
				|| !(attacker.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		serverPlayer.setIgnoreFallDamageFromCurrentImpulse(true, serverPlayer.position());
		serverPlayer.setDeltaMovement(
				serverPlayer.getDeltaMovement().x,
				Math.max(serverPlayer.getDeltaMovement().y, WIND_BURST_III_STRENGTH),
				serverPlayer.getDeltaMovement().z
		);
		serverPlayer.applyPostImpulseGraceTime(10);
		serverPlayer.hurtMarked = true;
		serverPlayer.needsSync = true;
		serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
				serverPlayer.getX(), serverPlayer.getY() + 0.1, serverPlayer.getZ(),
				1, 0.0, 0.0, 0.0, 0.0);
		serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
				SoundSource.PLAYERS, 1.0f, 1.0f);
	}
}
