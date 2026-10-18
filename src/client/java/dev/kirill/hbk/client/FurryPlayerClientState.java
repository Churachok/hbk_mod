package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.mechanic.FurryTransformation;
import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

/** Reads the server-synchronized player attachment; no client timer or substitute entity. */
public final class FurryPlayerClientState {
	private static final Identifier BLACK = HbkMod.id("textures/entity/black_silhouette.png");
	private static final Identifier[] TEXTURES = DyeColor.VALUES.stream()
			.map(color -> HbkMod.id("textures/entity/pink_furry_wolf_" + color.getName() + ".png"))
			.toArray(Identifier[]::new);
	private static final FurryPlayerModel HAND_MODEL = new FurryPlayerModel();

	private FurryPlayerClientState() {
	}

	public static int getColorId(AvatarRenderState state) {
		var level = Minecraft.getInstance().level;
		return level != null && level.getEntity(state.id) instanceof Player player
				? FurryTransformation.getColorId(player) : -1;
	}

	public static boolean isActive(AvatarRenderState state) {
		return !ProgenitorClientState.isActive(state) && getColorId(state) >= 0;
	}

	public static Identifier texture(int colorId) {
		return TEXTURES[Math.floorMod(colorId, TEXTURES.length)];
	}

	public static Identifier firstPersonTexture(Player player) {
		return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WESTERN_CHESTPLATE)
				? BLACK : texture(FurryTransformation.getColorId(player));
	}

	public static boolean renderFirstPersonHand(Player player, boolean right, PoseStack poseStack,
			SubmitNodeCollector collector, int light) {
		if (!FurryTransformation.isActive(player) || ProgenitorTransformation.isActive(player)) {
			return false;
		}
		collector.submitModelPart(HAND_MODEL.firstPersonArm(right), poseStack,
				RenderTypes.entityTranslucent(firstPersonTexture(player)), light, OverlayTexture.NO_OVERLAY, null);
		return true;
	}
}
