package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.network.JagermeisterRescuePayload;
import dev.kirill.hbk.registry.ModEffects;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Local vision only: other clients never receive glowing flags or a synthetic night-vision effect. */
public final class HuntersInstinctClient {
	public static final double HIGHLIGHT_RADIUS = HuntersInstinctEffect.HIGHLIGHT_RADIUS;

	private HuntersInstinctClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(JagermeisterRescuePayload.TYPE, (payload, context) ->
				context.client().execute(HuntersInstinctClient::showRescueAnimation));
	}

	public static boolean hasNightVision(LivingEntity viewer) {
		return viewer != null && viewer == Minecraft.getInstance().player
				&& viewer.hasEffect(ModEffects.HUNTERS_INSTINCT);
	}

	public static boolean shouldHighlight(Entity entity) {
		var viewer = Minecraft.getInstance().player;
		return hasNightVision(viewer) && entity instanceof LivingEntity && entity != viewer
				&& !entity.isRemoved() && entity.level() == viewer.level()
				&& viewer.distanceToSqr(entity) <= HIGHLIGHT_RADIUS * HIGHLIGHT_RADIUS;
	}

	public static void showRescueAnimation() {
		var client = Minecraft.getInstance();
		if (client.player != null) client.gameRenderer.displayItemActivation(animationStack());
	}

	/** A fresh render-only carrier: never registered as a separate item or placed in an inventory. */
	public static ItemStack animationStack() {
		var stack = new ItemStack(Items.TOTEM_OF_UNDYING);
		stack.set(DataComponents.ITEM_MODEL, HbkMod.id("jagermeister_stag"));
		return stack;
	}
}
