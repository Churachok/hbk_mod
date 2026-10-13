package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class YoungLiberalClient {
	public static final Identifier POST_EFFECT = HbkMod.id("young_liberal");
	public static final int DAY_SKY_COLOR = 0xFFFF55C7;
	public static final int NIGHT_SKY_COLOR = 0xFF45BFFF;

	private YoungLiberalClient() {
	}

	public static boolean isActive() {
		var player = Minecraft.getInstance().player;
		return player != null && player.hasEffect(ModEffects.YOUNG_LIBERAL);
	}

	public static boolean isNight(float sunAngle) {
		return Math.cos(sunAngle) < 0;
	}

	public static int skyColor(float sunAngle) {
		return isNight(sunAngle) ? NIGHT_SKY_COLOR : DAY_SKY_COLOR;
	}
}
