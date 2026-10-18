package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.registry.ModAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** A timed appearance on the existing player: no replacement entity, attributes or inventory changes. */
public final class FurryTransformation {
	public static final int NO_COLOR = -1;
	public static final int COLOR_COUNT = 16;
	public static final int DURATION_TICKS = 5 * 60 * 20;

	private FurryTransformation() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
		});
	}

	/** Reads the same synced attachment for local and remote player renderers without creating client-side state. */
	public static int getColorId(Player player) {
		if (player == null) return NO_COLOR;
		int color = player.getAttachedOrElse(ModAttachments.FURRY_COLOR, NO_COLOR);
		return color >= 0 && color < COLOR_COUNT ? color : NO_COLOR;
	}

	public static boolean isActive(Player player) {
		return getColorId(player) != NO_COLOR;
	}

	public static boolean start(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) return false;
		long now = player.level().getServer().overworld().getGameTime();
		player.setAttached(ModAttachments.FURRY_EXPIRES_AT, now + DURATION_TICKS);
		player.setAttached(ModAttachments.FURRY_COLOR, player.getRandom().nextInt(COLOR_COUNT));
		return true;
	}

	public static void tick(ServerPlayer player) {
		if (!player.hasAttached(ModAttachments.FURRY_COLOR) && !player.hasAttached(ModAttachments.FURRY_EXPIRES_AT)) return;
		long expiresAt = player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L);
		long now = player.level().getServer().overworld().getGameTime();
		if (!player.isAlive() || !isActive(player) || expiresAt <= now) {
			player.removeAttached(ModAttachments.FURRY_COLOR);
			player.removeAttached(ModAttachments.FURRY_EXPIRES_AT);
		}
	}
}
