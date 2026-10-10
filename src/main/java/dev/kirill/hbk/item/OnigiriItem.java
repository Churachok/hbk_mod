package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OnigiriItem extends Item {
	public static final int DURATION_TICKS = 30 * 20;

	public OnigiriItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		return feed(stack, player, target);
	}

	public static InteractionResult feed(ItemStack stack, Player player, LivingEntity target) {
		if (player.isSpectator() || !(target instanceof Mob) || !target.isAlive() || stack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!player.level().isClientSide()) {
			// Some bosses reject ordinary potion effects; the food works on every mob.
			target.forceAddEffect(new MobEffectInstance(ModEffects.ONIGIRI, DURATION_TICKS), player);
			if (!player.hasInfiniteMaterials()) {
				stack.shrink(1);
			}
			player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.8f, 1.0f);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide()) {
			user.addEffect(new MobEffectInstance(ModEffects.ONIGIRI, DURATION_TICKS));
		}
		return result;
	}
}
