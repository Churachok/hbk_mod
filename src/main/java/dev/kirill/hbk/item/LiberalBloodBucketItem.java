package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LiberalBloodBucketItem extends Item {
	public static final int DURATION_TICKS = 60 * 20;

	public LiberalBloodBucketItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!level.isClientSide()) {
			user.removeAllEffects();
			user.addEffect(new MobEffectInstance(ModEffects.YOUNG_LIBERAL, DURATION_TICKS));
			user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS));
			user.addEffect(new MobEffectInstance(MobEffects.HASTE, DURATION_TICKS));
		}
		return super.finishUsingItem(stack, level, user);
	}
}
