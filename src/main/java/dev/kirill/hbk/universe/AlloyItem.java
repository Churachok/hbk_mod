package dev.kirill.hbk.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Consumer;

public final class AlloyItem extends Item {
	private final String kind;
	public AlloyItem(String kind, Properties properties) { super(properties); this.kind = kind; }
	public String kind() { return kind; }
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		var result = AlloyActions.use(kind, level, player, hand);
		return result == InteractionResult.PASS ? super.use(level, player, hand) : result;
	}
	@Override public InteractionResult useOn(UseOnContext context) {
		var result = AlloyActions.useOn(kind, context);
		return result == InteractionResult.PASS ? super.useOn(context) : result;
	}
	@Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
		boolean result = super.mineBlock(stack, level, state, pos, entity);
		if (entity instanceof net.minecraft.server.level.ServerPlayer player) AlloyActions.mine(kind, stack, player, state, pos);
		return result;
	}
	@Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		super.inventoryTick(stack, level, entity, slot);
		if (entity instanceof Player player) AlloyActions.inventoryTick(kind, stack, player, slot);
	}
	@Override public boolean canFitInsideContainerItems() { return !kind.equals("sample_bag") && !kind.equals("carrying_grips"); }
	@Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flags) {
		lines.accept(Component.translatable("tooltip.hbk.alloy." + kind).withStyle(net.minecraft.ChatFormatting.GRAY));
		if (kind.equals("air_tank")) lines.accept(Component.translatable("message.hbk.alloy.air", AlloyActions.number(stack, "air", 1200) / 20));
		if (kind.equals("heat_cartridge")) lines.accept(Component.translatable("message.hbk.alloy.heat", AlloyActions.number(stack, "heat", 0)));
	}
}
