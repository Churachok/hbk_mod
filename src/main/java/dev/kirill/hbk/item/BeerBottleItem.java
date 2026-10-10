package dev.kirill.hbk.item;

import dev.kirill.hbk.effect.BeerEffect;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class BeerBottleItem extends Item {
	public BeerBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide()) {
			user.addEffect(new MobEffectInstance(ModEffects.BEER, BeerEffect.DURATION_TICKS));
			user.addEffect(new MobEffectInstance(MobEffects.HUNGER, BeerEffect.DURATION_TICKS, 0));
			user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BeerEffect.DURATION_TICKS, 0));
			user.addEffect(new MobEffectInstance(MobEffects.STRENGTH, BeerEffect.DURATION_TICKS, 0));
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.buffs").withStyle(ChatFormatting.BLUE));
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.hunger").withStyle(ChatFormatting.RED));
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.nausea").withStyle(ChatFormatting.GRAY));
	}
}
