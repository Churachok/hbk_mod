package dev.kirill.hbk.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Shift + RMB adds destruction without replacing the original RMB abilities. */
public class DestructiveMemberItem extends Item {
	public enum Ability {
		SIEGE(4.0f, 10, 12 * 20), BITE(3.5f, 8, 8 * 20),
		ROAR(4.5f, 0, 15 * 20), HAMMER(6.0f, 12, 15 * 20),
		BOMBING_RUN(3.0f, 20, 18 * 20);

		final float power;
		final double range;
		final int cooldown;

		Ability(float power, double range, int cooldown) {
			this.power = power;
			this.range = range;
			this.cooldown = cooldown;
		}
	}

	private final Ability ability;

	protected DestructiveMemberItem(Properties properties, Ability ability) {
		super(properties);
		this.ability = ability;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return player.isShiftKeyDown() ? useDestruction(level, player, hand) : super.use(level, player, hand);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		return player != null && player.isShiftKeyDown()
				? useDestruction(context.getLevel(), player, context.getHand()) : super.useOn(context);
	}

	protected final InteractionResult useDestruction(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || stack.getItem() != this || player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		Vec3 center = this.ability == Ability.ROAR ? player.position()
				: MemberDestruction.aimedPosition(serverLevel, player, this.ability.range);
		player.getCooldowns().addCooldown(stack, this.ability.cooldown);
		if (this.ability == Ability.BOMBING_RUN) {
			Vec3 look = player.getLookAngle();
			Vec3 direction = new Vec3(look.x, 0, look.z).normalize();
			if (direction.lengthSqr() < 1.0E-4) {
				var facing = player.getDirection();
				direction = new Vec3(facing.getStepX(), 0, facing.getStepZ());
			}
			for (int i = -1; i <= 1; i++) {
				MemberDestruction.explode(serverLevel, player, center.add(direction.scale(i * 4)), this.ability.power);
			}
		} else {
			MemberDestruction.explode(serverLevel, player, center, this.ability.power);
		}
		player.swing(hand, true);
		return InteractionResult.SUCCESS_SERVER;
	}

	public static void addAbilityTooltip(Item item, Consumer<Component> tooltip) {
		tooltip.accept(Component.translatable(item.getDescriptionId() + ".ability").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		addAbilityTooltip(this, tooltip);
	}
}
