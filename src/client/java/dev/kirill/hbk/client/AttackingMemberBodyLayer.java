package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders the selected weapon against the front of the lower torso. */
public final class AttackingMemberBodyLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public AttackingMemberBodyLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light,
			AvatarRenderState state, float yRot, float xRot) {
		if (!ModItems.isMemberWeapon(state.getMainHandItemStack())) {
			return;
		}

		poseStack.pushPose();
		// The crossbar is centred on the torso-to-legs seam. Rotating around X
		// turns the former vertical column into a horizontal forward extension:
		// two white cubes and the magenta tip point wherever the player faces.
		// Z positions the rear face of the centre cube against the torso surface.
		poseStack.translate(0.0f, 0.75f, -0.625f);
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));
		state.getMainHandItemState().submit(
				poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, state.outlineColor
		);
		poseStack.popPose();
	}
}
