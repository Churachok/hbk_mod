package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/** Client-only vision and outline resources for Kirill's glasses. */
public final class KirillGlassesClient {
	public static final Identifier POST_EFFECT = HbkMod.id("kirill_glasses");
	public static final Identifier ENTITY_MASK_EFFECT = HbkMod.id("red_entity_mask");

	private KirillGlassesClient() {
	}

	public static boolean isActive() {
		var player = Minecraft.getInstance().player;
		return player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.KIRILL_GLASSES);
	}

	public static boolean hasMobOutline() {
		return GoshasRageClient.isActive() || isActive();
	}
}
