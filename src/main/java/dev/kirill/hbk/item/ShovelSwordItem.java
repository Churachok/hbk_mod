package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ShovelSwordItem extends Item {
	public ShovelSwordItem(Properties properties) {
		super(properties);
	}

	public static boolean isAttackMode(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ATTACK_MODE, false);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean attackMode = !isAttackMode(stack);
		stack.set(ModDataComponents.ATTACK_MODE, attackMode);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable(
					attackMode ? "message.hbk.shovel_sword.attack" : "message.hbk.shovel_sword.shovel"
			));
		}
		player.playSound(net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN, 0.6f, attackMode ? 1.25f : 0.85f);
		return InteractionResult.SUCCESS;
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return isAttackMode(stack) ? 0.0f : super.getDestroySpeed(stack, state);
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return !isAttackMode(stack) && super.isCorrectToolForDrops(stack, state);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isAttackMode(stack) || super.isFoil(stack);
	}
}
