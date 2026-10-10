package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** The same 3D bottle item is visible in the extended paw until the handover. */
public final class SquirrelBeerLayer extends RenderLayer<SquirrelRenderState, SquirrelModel> {
	public SquirrelBeerLayer(RenderLayerParent<SquirrelRenderState, SquirrelModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
			SquirrelRenderState state, float yRot, float xRot) {
		if (state.beer.isEmpty() || state.giveProgress <= 0) return;
		poseStack.pushPose();
		this.getParentModel().translateToGiftPaw(poseStack);
		state.beer.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
