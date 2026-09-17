package dev.kirill.hbk.item;

import dev.kirill.hbk.player.MechanicsPlayerData;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class StewItem extends Item {
	public static final int COMBO_TICKS = 20 * 30;
	private static final int HEAVINESS_TICKS = 20 * 60 * 2;
	private static final float EXPIRED_CHANCE = 0.20f;

	public StewItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, level, user);
		if (!(level instanceof ServerLevel serverLevel)) {
			return result;
		}

		if (serverLevel.getRandom().nextFloat() < EXPIRED_CHANCE) {
			user.hurtServer(serverLevel, user.damageSources().magic(), 4.0f);
			user.addEffect(new MobEffectInstance(ModEffects.EXPIRED, 20 * 5, 0, false, true, true));
			if (user instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("message.hbk.stew.expired"));
			}
		}

		if (user instanceof MechanicsPlayerData data) {
			long now = level.getGameTime();
			if (isRecent(data.hbk$getLastBuckwheatTick(), now)) {
				user.addEffect(new MobEffectInstance(ModEffects.HEARTY_LUNCH, COMBO_TICKS, 0, false, true, true));
			}

			int count = isRecent(data.hbk$getLastStewTick(), now)
					? data.hbk$getConsecutiveStew() + 1
					: 1;
			data.hbk$setLastStewTick(now);
			if (count >= 5) {
				user.addEffect(new MobEffectInstance(ModEffects.HEAVINESS, HEAVINESS_TICKS, 0, false, true, true));
				data.hbk$setConsecutiveStew(0);
				if (user instanceof ServerPlayer player) {
					player.sendOverlayMessage(Component.translatable("message.hbk.stew.heaviness"));
				}
			} else {
				data.hbk$setConsecutiveStew(count);
			}
		}
		return result;
	}

	static boolean isRecent(long previous, long now) {
		return previous != Long.MIN_VALUE && now >= previous && now - previous <= COMBO_TICKS;
	}
}
