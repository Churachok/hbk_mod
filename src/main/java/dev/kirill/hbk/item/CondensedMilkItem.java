package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.player.MechanicsPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CondensedMilkItem extends Item {
	public static final int SWEET_LIFE_TICKS = 20 * 30;

	public CondensedMilkItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!level.isClientSide()) {
			boolean secondCan = user.hasEffect(ModEffects.SWEET_LIFE) || user.hasEffect(ModEffects.DROWSINESS);
			user.addEffect(new MobEffectInstance(ModEffects.SWEET_LIFE, SWEET_LIFE_TICKS, 0, false, true, true));
			if (user instanceof MechanicsPlayerData data) {
				data.hbk$setSweetLifeTicks(SWEET_LIFE_TICKS);
			}
			if (secondCan) {
				user.addEffect(new MobEffectInstance(ModEffects.DIABETES, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
				if (user instanceof ServerPlayer player) {
					player.sendSystemMessage(Component.translatable("message.hbk.condensed_milk.diabetes"));
				}
			}
		}
		return super.finishUsingItem(stack, level, user);
	}
}
