package dev.kirill.hbk.universe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import java.util.function.Consumer;

public final class AlloyDeviceItem extends BlockItem {
	private final String kind;
	public AlloyDeviceItem(Block block, String kind, Properties properties) { super(block, properties); this.kind = kind; }
	@Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flags) {
		super.appendHoverText(stack, context, display, lines, flags);
		lines.accept(Component.translatable("tooltip.hbk.alloy." + kind).withStyle(ChatFormatting.GRAY));
	}
}
