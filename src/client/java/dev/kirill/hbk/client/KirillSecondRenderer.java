package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KirillSecondEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KirillSecondRenderer extends HumanoidMobRenderer<KirillSecondEntity, GiantBossRenderState, KirillSecondModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/kirill_v2.png");
	private static final Identifier PORTAL_TEXTURE = HbkMod.id("textures/entity/unknown_portal.png");

	public KirillSecondRenderer(EntityRendererProvider.Context context) {
		super(context, new KirillSecondModel(context.bakeLayer(ModEntityModelLayers.KIRILL_V2)), 0.55f);
	}

	@Override
	protected AABB getBoundingBoxForCulling(KirillSecondEntity entity) {
		AABB bounds = super.getBoundingBoxForCulling(entity);
		return entity.isPortalVisible() ? bounds.inflate(5.0, 2.0, 5.0) : bounds;
	}

	@Override
	public void extractRenderState(KirillSecondEntity entity, GiantBossRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.unknownGrabProgress = entity.getGrabAnimationProgress(tickProgress);
		state.unknownPortalVisible = entity.isPortalVisible();
		state.unknownPortalOpenAge = entity.tickCount + tickProgress - entity.getPortalOpenStartTick();
		state.unknownPortalCloseAge = entity.getPortalCloseStartTick() < 0 ? -1.0f
				: entity.tickCount + tickProgress - entity.getPortalCloseStartTick();
		Vec3 portalPosition = entity.getPortalPosition();
		state.unknownPortalX = portalPosition.x;
		state.unknownPortalY = portalPosition.y;
		state.unknownPortalZ = portalPosition.z;
		if (entity.isGrabbing()) {
			state.walkAnimationSpeed = 0.0f;
		}
	}

	@Override
	public void submit(GiantBossRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		if (state.unknownPortalVisible) {
			float progress = Mth.clamp(state.unknownPortalOpenAge / 12.0f, 0.0f, 1.0f);
			float inverse = 1.0f - progress;
			float scale = 0.08f + 0.92f * (1.0f - inverse * inverse * inverse);
			if (state.unknownPortalCloseAge >= 0.0f) {
				float closing = Mth.clamp(state.unknownPortalCloseAge / 30.0f, 0.0f, 1.0f);
				scale *= 1.0f - closing * closing * (3.0f - 2.0f * closing);
			}
			poseStack.pushPose();
			poseStack.translate(state.unknownPortalX - state.x,
					state.unknownPortalY - state.y + 1.4,
					state.unknownPortalZ - state.z);
			poseStack.mulPose(camera.orientation);
			poseStack.translate(0.0, 0.0, -0.15);
			poseStack.scale(scale, scale, 1.0f);
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(PORTAL_TEXTURE),
					(pose, vertices) -> {
						portalVertex(vertices, pose, -2.5f, -1.40625f, 0.0f, 1.0f);
						portalVertex(vertices, pose, 2.5f, -1.40625f, 1.0f, 1.0f);
						portalVertex(vertices, pose, 2.5f, 1.40625f, 1.0f, 0.0f);
						portalVertex(vertices, pose, -2.5f, 1.40625f, 0.0f, 0.0f);
					});
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}

	private static void portalVertex(VertexConsumer vertices, PoseStack.Pose pose,
			float x, float y, float u, float v) {
		vertices.addVertex(pose, x, y, 0.0f).setColor(-1).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
				.setNormal(pose, 0.0f, 0.0f, 1.0f);
	}

	@Override
	public GiantBossRenderState createRenderState() {
		return new GiantBossRenderState();
	}

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) {
		return TEXTURE;
	}
}
