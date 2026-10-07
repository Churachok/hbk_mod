package dev.kirill.hbk.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EntityType;

import java.util.function.Consumer;

public final class ImprovedBoatItem extends BoatItem {
	public ImprovedBoatItem(EntityType<? extends AbstractBoat> type, Properties properties) {
		super(type, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.hbk.improved_boat.fuel_hint")
				.withStyle(ChatFormatting.GRAY));
	}
}
