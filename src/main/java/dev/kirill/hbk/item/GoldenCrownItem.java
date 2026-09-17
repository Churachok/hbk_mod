package dev.kirill.hbk.item;

import dev.kirill.hbk.player.MechanicsPlayerData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GoldenCrownItem extends Item {
	public static final int EFFECT_TICKS = 20 * 5;

	public GoldenCrownItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			ItemStack stack = player.getItemInHand(hand);
			if (!player.isCreative()) {
				stack.shrink(1);
			}
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS, 0, false, true, true));
			((MechanicsPlayerData) player).hbk$setGooseTicks(EFFECT_TICKS);
		}
		return InteractionResult.SUCCESS;
	}
}
