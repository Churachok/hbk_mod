package dev.kirill.hbk.client;

import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;

public final class ProgenitorClientState {
	private ProgenitorClientState() {
	}

	public static boolean isActive(AvatarRenderState state) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level != null
				&& minecraft.level.getEntity(state.id) instanceof Player player
				&& ProgenitorTransformation.isActive(player);
	}
}
