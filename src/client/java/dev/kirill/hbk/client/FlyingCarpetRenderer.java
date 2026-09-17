package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.FlyingCarpetEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class FlyingCarpetRenderer extends EntityRenderer<FlyingCarpetEntity, FlyingCarpetRenderState> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/flying_carpet.png");
	private final FlyingCarpetModel model;

	public FlyingCarpetRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new FlyingCarpetModel(context.bakeLayer(ModEntityModelLayers.FLYING_CARPET));
		this.shadowRadius = 0.9f;
	}

	@Override
	public FlyingCarpetRenderState createRenderState() {
		return new FlyingCarpetRenderState();
	}

	@Override
	public void extractRenderState(FlyingCarpetEntity entity, FlyingCarpetRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.yRot = entity.getYRot(tickProgress);
	}

	@Override
	public void submit(FlyingCarpetRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.translate(0.0f, 0.18f, 0.0f);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - state.yRot));
		poseStack.scale(-1.0f, -1.0f, 1.0f);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, cameraState);
	}
}
