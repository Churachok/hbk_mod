package dev.kirill.hbk.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HardKirillKettleItem extends Item {
	public static final int DURATION_TICKS = 30 * 20;

	public HardKirillKettleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide()) {
			user.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION_TICKS));
			user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS));
		}
		return result;
	}
}
