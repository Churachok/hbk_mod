package dev.kirill.hbk.item;

import dev.kirill.hbk.player.MechanicsPlayerData;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BuckwheatItem extends Item {
	public BuckwheatItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide() && user instanceof MechanicsPlayerData data) {
			long now = level.getGameTime();
			if (StewItem.isRecent(data.hbk$getLastStewTick(), now)) {
				user.addEffect(new MobEffectInstance(ModEffects.HEARTY_LUNCH, StewItem.COMBO_TICKS, 0, false, true, true));
			}
			data.hbk$setLastBuckwheatTick(now);
		}
		return result;
	}
}
