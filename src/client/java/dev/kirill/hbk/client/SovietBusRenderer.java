package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SovietBusEntity;
import net.minecraft.util.LightCoordsUtil;
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

public final class SovietBusRenderer extends EntityRenderer<SovietBusEntity, SovietBusRenderer.State> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/soviet_bus.png");
	private final SovietBusModel body;
	private final SovietBusModel glass;
	private final SovietBusModel lights;

	public SovietBusRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.body = new SovietBusModel(context.bakeLayer(ModEntityModelLayers.sovietBus(SovietBusModel.Material.BODY)),
				SovietBusModel.Material.BODY);
		this.glass = new SovietBusModel(context.bakeLayer(ModEntityModelLayers.sovietBus(SovietBusModel.Material.GLASS)),
				SovietBusModel.Material.GLASS);
		this.lights = new SovietBusModel(context.bakeLayer(ModEntityModelLayers.sovietBus(SovietBusModel.Material.LIGHTS)),
				SovietBusModel.Material.LIGHTS);
		this.shadowRadius = 2.0f;
	}

	@Override
	protected AABB getBoundingBoxForCulling(SovietBusEntity entity) {
		return entity.getBoundingBox().move(entity.getAnimationOffset(1));
	}

	@Override
	public boolean shouldRender(SovietBusEntity entity, Frustum frustum, double x, double y, double z) {
		// The departure model can be 320 blocks from the network anchor; cull the
		// actual vehicle, not its original stop or vanilla's short mob draw range.
		return entity.isWaiting() ? super.shouldRender(entity, frustum, x, y, z)
				: frustum.isVisible(getBoundingBoxForCulling(entity).inflate(2));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SovietBusEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		var offset = entity.getAnimationOffset(partialTick);
		state.x += offset.x;
		state.z += offset.z;
		state.moving = !entity.isWaiting();
		state.outlineColor = EntityRenderState.NO_OUTLINE;
		state.yRot = entity.getYRot(partialTick);
		state.wheelRotation = entity.getWheelRotation(partialTick);
	}

	@Override
	protected float getShadowRadius(State state) {
		// No fake shadow remains at the stop; distant/unloaded terrain needs no shadow queries.
		return state.moving ? 0 : super.getShadowRadius(state);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - state.yRot));
		float scale = SovietBusEntity.MODEL_SCALE;
		poseStack.scale(-scale, -scale, scale);
		collector.submitModel(body, state, poseStack, TEXTURE, state.lightCoords,
				OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE, null);
		collector.submitModel(lights, state, poseStack, TEXTURE, LightCoordsUtil.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE, null);
		// This overload's final int is the OUTLINE, not a white (-1) tint.
		collector.order(1).submitModel(glass, state, poseStack, RenderTypes.entityTranslucent(TEXTURE),
				state.lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, cameraState);
	}

	public static final class State extends EntityRenderState {
		public float yRot;
		public float wheelRotation;
		public boolean moving;
	}
}
