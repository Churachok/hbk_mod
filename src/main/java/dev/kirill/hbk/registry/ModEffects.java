package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.effect.SimpleModEffect;
import dev.kirill.hbk.effect.SoulfulnessEffect;
import dev.kirill.hbk.effect.DiabetesEffect;
import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.effect.HeartyLunchEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModEffects {
	public static final Holder.Reference<MobEffect> HAND_IMMORTALITY = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("hand_immortality"),
			new SimpleModEffect(MobEffectCategory.BENEFICIAL, 0x8A2BE2)
	);

	public static final Holder.Reference<MobEffect> SOULFULNESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("soulfulness"),
			new SoulfulnessEffect(0xE3A84B)
	);

	public static final Holder.Reference<MobEffect> SWEET_LIFE = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("sweet_life"),
			new SimpleModEffect(MobEffectCategory.BENEFICIAL, 0xF4D58A)
					.addAttributeModifier(
							Attributes.MOVEMENT_SPEED,
							HbkMod.id("sweet_life_speed"),
							0.20,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					)
	);

	public static final Holder.Reference<MobEffect> DROWSINESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("drowsiness"),
			new SimpleModEffect(MobEffectCategory.HARMFUL, 0x625878)
					.addAttributeModifier(
							Attributes.MOVEMENT_SPEED,
							HbkMod.id("drowsiness_freeze"),
							-1.0,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					)
	);

	public static final Holder.Reference<MobEffect> DIABETES = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("diabetes"),
			new DiabetesEffect(0xA85E4D)
	);

	public static final Holder.Reference<MobEffect> EXPIRED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("expired"),
			new SimpleModEffect(MobEffectCategory.HARMFUL, 0x71804A)
	);

	public static final Holder.Reference<MobEffect> HEARTY_LUNCH = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("hearty_lunch"),
			new HeartyLunchEffect(0xC9823B)
	);

	public static final Holder.Reference<MobEffect> HEAVINESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("heaviness"),
			new SimpleModEffect(MobEffectCategory.HARMFUL, 0x596273)
					.addAttributeModifier(
							Attributes.MOVEMENT_SPEED,
							HbkMod.id("heaviness_speed"),
							-0.20,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					)
	);

	public static final Holder.Reference<MobEffect> GOSHAS_RAGE = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			HbkMod.id("goshas_rage"),
			new GoshasRageEffect(0x00A7C8)
					.addAttributeModifier(
							Attributes.MAX_HEALTH,
							HbkMod.id("goshas_rage_health"),
							-0.5,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					)
	);

	private ModEffects() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered mob effects for {}", HbkMod.MOD_ID);
	}
}
