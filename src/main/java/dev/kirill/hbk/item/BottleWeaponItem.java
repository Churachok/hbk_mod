package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.block.BeerCauldronBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

/** The reusable empty bottle becomes a single-use sharp bottle only after a successful hit. */
public final class BottleWeaponItem extends Item {
	public static final float BROKEN_CHANCE = 0.05f;
	public static final int POISON_TICKS = 10 * 20;
	private final boolean broken;

	public BottleWeaponItem(Properties properties, boolean broken) {
		super(properties);
		this.broken = broken;
	}

	public static ItemAttributeModifiers attackAttributes(double totalDamage) {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
						totalDamage - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
						-2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker.level() instanceof ServerLevel level) || attacker.getMainHandItem() != stack) return;
		if (this.broken) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0), attacker);
			stack.shrink(1);
			level.playSound(null, attacker.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
		} else if (attacker.getRandom().nextFloat() < BROKEN_CHANCE) {
			// Leave the old, detached stack nonempty: vanilla still checks it after this callback.
			attacker.setItemSlot(EquipmentSlot.MAINHAND, stack.transmuteCopy(ModItems.BROKEN_BEER_BOTTLE, 1));
			level.playSound(null, attacker.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return this.broken ? InteractionResult.PASS : BeerCauldronBlock.tryFillBottle(context);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		if (this.broken) {
			tooltip.accept(Component.translatable("item.hbk.broken_beer_bottle.poison").withStyle(ChatFormatting.DARK_GREEN));
			tooltip.accept(Component.translatable("item.hbk.broken_beer_bottle.single_use").withStyle(ChatFormatting.RED));
		} else {
			tooltip.accept(Component.translatable("item.hbk.empty_beer_bottle.break_chance").withStyle(ChatFormatting.GRAY));
		}
	}
}
