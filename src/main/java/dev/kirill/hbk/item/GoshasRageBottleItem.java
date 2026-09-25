package dev.kirill.hbk.item;

import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GoshasRageBottleItem extends Item {
	public GoshasRageBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!level.isClientSide()) {
			user.addEffect(new MobEffectInstance(ModEffects.GOSHAS_RAGE,
					GoshasRageEffect.DURATION_TICKS, 0, false, true, true));
		}
		return super.finishUsingItem(stack, level, user);
	}
}
