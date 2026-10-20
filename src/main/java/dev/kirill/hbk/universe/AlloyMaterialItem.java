package dev.kirill.hbk.universe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/** The player can discover ingredients and smithing bases directly in vanilla tooltips. */
public final class AlloyMaterialItem extends Item {
	private final UniverseAlloys.Alloy alloy;
	private final boolean sample;
	public AlloyMaterialItem(UniverseAlloys.Alloy alloy, boolean sample, Properties properties) { super(properties); this.alloy = alloy; this.sample = sample; }
	@Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flags) {
		lines.accept(Component.translatable("tooltip.hbk.alloy.ingredients", new ItemStack(UniverseMaterials.resource(alloy.first(), 0)).getHoverName(),
				new ItemStack(UniverseMaterials.resource(alloy.second(), 0)).getHoverName()).withStyle(ChatFormatting.GRAY));
		if (sample) lines.accept(Component.translatable(AlloyActions.number(stack, "washed", 0) == 1 ? "tooltip.hbk.alloy.washed" : "tooltip.hbk.alloy.blasting").withStyle(ChatFormatting.GRAY));
		else {
			lines.accept(Component.translatable("tooltip.hbk.alloy.smithing").withStyle(ChatFormatting.GRAY));
			for (var reward : alloy.rewards()) lines.accept(Component.translatable("tooltip.hbk.alloy.upgrade",
					Component.translatable("item.minecraft." + reward.base()), new ItemStack(UniverseAlloys.item(reward.id())).getHoverName()).withStyle(ChatFormatting.GRAY));
		}
	}
}
