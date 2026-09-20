package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies the set bonus only while all four uranium armour pieces are equipped. */
public final class UraniumArmorEffects {
	private static final Identifier MAX_HEALTH_ID = HbkMod.id("uranium_armor_max_health");
	private static final Identifier ATTACK_DAMAGE_ID = HbkMod.id("uranium_armor_attack_damage");
	private static final Identifier SPEED_ID = HbkMod.id("uranium_armor_speed");

	private static final AttributeModifier MAX_HEALTH = new AttributeModifier(
			MAX_HEALTH_ID, 20.0, AttributeModifier.Operation.ADD_VALUE
	);
	private static final AttributeModifier ATTACK_DAMAGE = new AttributeModifier(
			ATTACK_DAMAGE_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);
	private static final AttributeModifier SPEED = new AttributeModifier(
			SPEED_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

	private UraniumArmorEffects() {
	}

	/** Radiation protection needs only one correctly equipped uranium piece, not the set bonus. */
	public static boolean hasRadiationProtection(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.URANIUM_HELMET)
				|| entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.URANIUM_CHESTPLATE)
				|| entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.URANIUM_LEGGINGS)
				|| entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.URANIUM_BOOTS);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				update(player);
			}
		});
	}

	private static void update(ServerPlayer player) {
		boolean completeSet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.URANIUM_HELMET)
				&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.URANIUM_CHESTPLATE)
				&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.URANIUM_LEGGINGS)
				&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.URANIUM_BOOTS);

		updateModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH, completeSet);
		updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE, completeSet);
		updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, completeSet);

		if (completeSet) {
			// A full set keeps both hunger and saturation full, including while sprinting.
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(20.0f);
		} else {
			AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
				player.setHealth((float) maxHealth.getValue());
			}
		}
	}

	private static void updateModifier(AttributeInstance attribute, AttributeModifier modifier, boolean enabled) {
		if (attribute == null) {
			return;
		}
		if (enabled) {
			attribute.addOrUpdateTransientModifier(modifier);
		} else {
			attribute.removeModifier(modifier.id());
		}
	}
}
