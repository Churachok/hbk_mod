package dev.kirill.hbk.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DenisDoshirakItem extends Item {
	public DenisDoshirakItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (level instanceof ServerLevel serverLevel) {
			user.hurtServer(serverLevel, level.damageSources().generic(), 1.0f);
		}
		return result;
	}
}
