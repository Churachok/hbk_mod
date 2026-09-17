package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CurrantTinctureItem extends Item {
	public static final int EFFECT_TICKS = 20 * 120;

	public CurrantTinctureItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!level.isClientSide()) {
			user.removeEffect(ModEffects.DIABETES);
			user.addEffect(new MobEffectInstance(ModEffects.HAND_IMMORTALITY, EFFECT_TICKS, 0, false, true, true));
		}
		return super.finishUsingItem(stack, level, user);
	}
}
