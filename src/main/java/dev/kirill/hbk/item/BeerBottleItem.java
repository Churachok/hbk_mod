package dev.kirill.hbk.item;

import dev.kirill.hbk.effect.BeerEffect;
import dev.kirill.hbk.block.BeerCauldronBlock;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class BeerBottleItem extends Item {
	public static final int MAX_SIPS = 3;

	public BeerBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return BeerCauldronBlock.tryFillBottle(context);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack portion = stack.copyWithCount(1);
		int sips = getSips(portion);
		// Keep vanilla drinking sounds, item-used statistics and consume advancements.
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide()) {
			user.addEffect(new MobEffectInstance(ModEffects.BEER, BeerEffect.DURATION_TICKS));
		}
		if (user.hasInfiniteMaterials()) return result;
		ItemStack remainder;
		if (sips > 1) {
			remainder = portion;
			remainder.set(ModDataComponents.BEER_SIPS, sips - 1);
			remainder.set(DataComponents.MAX_STACK_SIZE, 1);
		} else {
			remainder = portion.transmuteCopy(ModItems.EMPTY_BEER_BOTTLE, 1);
			remainder.remove(ModDataComponents.BEER_SIPS);
		}
		if (result.isEmpty()) return remainder;
		// Separate the started bottle without changing the other full bottles in this stack.
		if (user instanceof Player player) {
			if (!player.getInventory().add(remainder)) player.drop(remainder, false);
		} else if (!level.isClientSide()) {
			user.spawnAtLocation((net.minecraft.server.level.ServerLevel) level, remainder);
		}
		return result;
	}

	public static int getSips(ItemStack stack) {
		return stack.is(ModItems.BEER_BOTTLE)
				? Math.clamp(stack.getOrDefault(ModDataComponents.BEER_SIPS, MAX_SIPS), 1, MAX_SIPS) : 0;
	}

	@Override public boolean isBarVisible(ItemStack stack) { return getSips(stack) < MAX_SIPS; }
	@Override public int getBarWidth(ItemStack stack) { return Math.round(13.0f * getSips(stack) / MAX_SIPS); }
	@Override public int getBarColor(ItemStack stack) { return 0xD9A438; }

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.sips", getSips(stack), MAX_SIPS)
				.withStyle(ChatFormatting.GOLD));
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.buffs").withStyle(ChatFormatting.BLUE));
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.hunger").withStyle(ChatFormatting.RED));
		tooltip.accept(Component.translatable("item.hbk.beer_bottle.nausea").withStyle(ChatFormatting.GRAY));
	}
}
