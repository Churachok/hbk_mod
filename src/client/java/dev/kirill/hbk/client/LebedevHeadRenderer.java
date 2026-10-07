package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.LebedevHeadEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class LebedevHeadRenderer extends EntityRenderer<LebedevHeadEntity, LebedevHeadRenderer.State> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/lebedev_head.png");
	private final LebedevHeadModel model;

	public LebedevHeadRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new LebedevHeadModel(context.bakeLayer(ModEntityModelLayers.LEBEDEV_HEAD));
		this.shadowRadius = 0.4f;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LebedevHeadEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot(partialTick);
		state.roll = entity.getRoll(partialTick);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - state.yRot));
		poseStack.translate(0.0f, 0.4f, 0.0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(-state.roll));
		poseStack.translate(0.0f, -0.4f, 0.0f);
		poseStack.scale(-1.6f, -1.6f, 1.6f);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords,
				OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, cameraState);
	}

	public static final class State extends EntityRenderState {
		public float yRot;
		public float roll;
	}
}
