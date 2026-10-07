package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OnigiriItem extends Item {
	public static final int DURATION_TICKS = 30 * 20;

	public OnigiriItem(Properties properties) {
		super(properties);
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
