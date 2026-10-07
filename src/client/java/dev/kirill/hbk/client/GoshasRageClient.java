package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.client.mixin.GameRendererInvoker;
import dev.kirill.hbk.registry.ModEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Owns the post-processing effect while the local player has Gosha's Rage. */
public final class GoshasRageClient {
	public static final Identifier POST_EFFECT = HbkMod.id("goshas_rage");
	public static final Identifier ENTITY_MASK_EFFECT = HbkMod.id("entity_mask");
	public static final Identifier CURRANT_EFFECT = HbkMod.id("currant_inversion");
	private static boolean ownsPostEffect;

	private GoshasRageClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(GoshasRageClient::tick);
	}

	public static boolean isActive() {
		var player = Minecraft.getInstance().player;
		return player != null && player.hasEffect(ModEffects.GOSHAS_RAGE);
	}

	private static void tick(Minecraft client) {
		if (client.player != null && client.player.hasEffect(ModEffects.HAND_IMMORTALITY)) {
			((GameRendererInvoker) client.gameRenderer).hbk$setPostEffect(CURRANT_EFFECT);
			ownsPostEffect = true;
		} else if (isActive()) {
			((GameRendererInvoker) client.gameRenderer).hbk$setPostEffect(POST_EFFECT);
			ownsPostEffect = true;
		} else if (KirillGlassesClient.isActive()) {
			((GameRendererInvoker) client.gameRenderer).hbk$setPostEffect(KirillGlassesClient.POST_EFFECT);
			ownsPostEffect = true;
		} else if (ownsPostEffect) {
			client.gameRenderer.checkEntityPostEffect(client.getCameraEntity());
			ownsPostEffect = false;
		}
	}
}
