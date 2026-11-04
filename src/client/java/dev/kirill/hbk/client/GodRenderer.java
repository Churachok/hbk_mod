package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.GodEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** The supplied video is an 80-frame, transparent circular billboard at 20 fps. */
public final class GodRenderer extends EntityRenderer<GodEntity, GodRenderer.State> {
	private static final Identifier ATLAS = HbkMod.id("textures/entity/god_atlas.png");
	public GodRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
	@Override public State createRenderState() { return new State(); }
	@Override protected AABB getBoundingBoxForCulling(GodEntity entity) {
		var center = entity.position().add(entity.visualOffset());
		return new AABB(center.x - 1.5, center.y - 1.5, center.z - 1.5, center.x + 1.5, center.y + 1.5, center.z + 1.5);
	}
	@Override public boolean shouldRender(GodEntity entity, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(getBoundingBoxForCulling(entity));
	}
	@Override public void extractRenderState(GodEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		var offset = entity.visualOffset();
		state.x += offset.x; state.y += offset.y; state.z += offset.z;
		state.frame = Math.floorMod(entity.tickCount, 80);
		state.outlineColor = EntityRenderState.NO_OUTLINE;
	}
	@Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		float u0 = (state.frame % 8) / 8f, v0 = (state.frame / 8) / 10f;
		float u1 = u0 + 1 / 8f, v1 = v0 + 1 / 10f;
		poses.pushPose();
		poses.mulPose(camera.orientation);
		collector.submitCustomGeometry(poses, RenderTypes.entityCutout(ATLAS), (pose, vertices) -> {
			vertex(vertices, pose, -1.5f, -1.5f, u0, v1);
			vertex(vertices, pose, 1.5f, -1.5f, u1, v1);
			vertex(vertices, pose, 1.5f, 1.5f, u1, v0);
			vertex(vertices, pose, -1.5f, 1.5f, u0, v0);
		});
		poses.popPose();
	}
	private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v) {
		vertices.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(0xF000F0).setNormal(pose, 0, 0, 1);
	}
	public static final class State extends EntityRenderState { public int frame; }
}
