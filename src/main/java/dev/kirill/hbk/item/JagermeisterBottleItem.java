package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.mechanic.HuntersInstinct;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Seven drink uses, each adding three minutes of Hunter's Instinct without stacking its level. */
public final class JagermeisterBottleItem extends Item {
	public static final int MAX_SIPS = 7;

	public JagermeisterBottleItem(Properties properties) {
		super(properties);
	}

	public static int getSips(ItemStack stack) {
		return stack.is(ModItems.JAGERMEISTER_BOTTLE)
				? Math.clamp(stack.getOrDefault(ModDataComponents.JAGERMEISTER_SIPS, MAX_SIPS), 1, MAX_SIPS) : 0;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack portion = stack.copyWithCount(1);
		int sips = getSips(portion);
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!level.isClientSide()) HuntersInstinct.drink(user);
		if (user.hasInfiniteMaterials()) return result;
		ItemStack remainder;
		if (sips > 1) {
			remainder = portion;
			remainder.set(ModDataComponents.JAGERMEISTER_SIPS, sips - 1);
			remainder.set(DataComponents.MAX_STACK_SIZE, 1);
		} else {
			remainder = portion.transmuteCopy(ModItems.EMPTY_JAGERMEISTER_BOTTLE, 1);
			remainder.remove(ModDataComponents.JAGERMEISTER_SIPS);
			remainder.set(DataComponents.MAX_STACK_SIZE, ModItems.EMPTY_JAGERMEISTER_BOTTLE.getDefaultMaxStackSize());
		}
		if (result.isEmpty()) return remainder;
		if (user instanceof Player player) {
			if (!player.getInventory().add(remainder)) player.drop(remainder, false);
		} else if (!level.isClientSide()) {
			user.spawnAtLocation((net.minecraft.server.level.ServerLevel) level, remainder);
		}
		return result;
	}

	@Override public boolean isBarVisible(ItemStack stack) { return getSips(stack) < MAX_SIPS; }
	@Override public int getBarWidth(ItemStack stack) { return Math.round(13.0f * getSips(stack) / MAX_SIPS); }
	@Override public int getBarColor(ItemStack stack) { return 0xA65E2D; }

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.hbk.jagermeister_bottle.sips", getSips(stack), MAX_SIPS)
				.withStyle(ChatFormatting.GOLD));
		tooltip.accept(Component.translatable("item.hbk.jagermeister_bottle.effect").withStyle(ChatFormatting.GREEN));
	}
}
