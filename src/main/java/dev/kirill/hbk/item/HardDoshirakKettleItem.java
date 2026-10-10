package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HardDoshirakKettleItem extends Item {
	public HardDoshirakKettleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide()) {
			// Speed II is the tea's original 20% plus the noodles' extra 20%.
			user.addEffect(new MobEffectInstance(MobEffects.SPEED, HardKirillKettleItem.DURATION_TICKS, 1));
			user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HardKirillKettleItem.DURATION_TICKS));
			user.addEffect(new MobEffectInstance(ModEffects.DOSHIRAK, HardKirillKettleItem.DURATION_TICKS));
		}
		return result;
	}
}
