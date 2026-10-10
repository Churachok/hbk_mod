package dev.kirill.hbk.item;

import dev.kirill.hbk.effect.FunnySpinEffect;
import dev.kirill.hbk.entity.ReferenceNpcEntity;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FunnyButtonItem extends Item {
	public static final double RADIUS = 50.0;

	public FunnyButtonItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isSpectator()) return InteractionResult.FAIL;
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.playSound(null, player.blockPosition(), ModSounds.CANNED_LAUGHTER,
					SoundSource.PLAYERS, 4.0f, 1.0f);
			for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS),
					entity -> entity.isAlive() && entity.distanceToSqr(player) <= RADIUS * RADIUS)) {
				boolean hostile = isHostile(mob);
				mob.forceAddEffect(new MobEffectInstance(ModEffects.FUNNY_SPIN,
						FunnySpinEffect.DURATION_TICKS, hostile ? 1 : 0, false, false, false), player);
			}
			player.getCooldowns().addCooldown(stack, FunnySpinEffect.DURATION_TICKS);
		}
		return InteractionResult.SUCCESS;
	}

	public static boolean isHostile(Mob mob) {
		return mob instanceof Enemy || mob.getTarget() != null
				|| mob instanceof ReferenceNpcEntity npc && npc.isNpc("gosha");
	}
}
