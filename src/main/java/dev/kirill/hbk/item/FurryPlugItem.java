package dev.kirill.hbk.item;

import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A breeding interaction with the targeted adult; it has no self-use or body rendering. */
public final class FurryPlugItem extends Item {
	public FurryPlugItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		return useOnTarget(stack, player, target);
	}

	public static InteractionResult useOnTarget(ItemStack stack, Player player, LivingEntity target) {
		if (!stack.is(ModItems.FURRY_PLUG) || player == target || !player.isAlive() || player.isSpectator()
				|| !FurryBreeding.canEnterLove(target)) {
			return InteractionResult.PASS;
		}
		if (!player.level().isClientSide()) {
			if (!FurryBreeding.enterLove(target)) return InteractionResult.PASS;
			Item item = stack.getItem();
			if (!player.hasInfiniteMaterials()) stack.shrink(1);
			player.awardStat(Stats.ITEM_USED.get(item));
		}
		return InteractionResult.SUCCESS;
	}
}
