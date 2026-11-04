package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SkippingOwlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public final class SkippingOwlRenderer extends MobRenderer<SkippingOwlEntity, SkippingOwlRenderState, SkippingOwlModel> {
	public static final Identifier TEXTURE = HbkMod.id("textures/entity/skipping_owl.png");

	public SkippingOwlRenderer(EntityRendererProvider.Context context) {
		super(context, new SkippingOwlModel(context.bakeLayer(ModEntityModelLayers.SKIPPING_OWL)), 0.4f);
	}

	@Override
	public SkippingOwlRenderState createRenderState() { return new SkippingOwlRenderState(); }

	@Override
	public void extractRenderState(SkippingOwlEntity entity, SkippingOwlRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.waitingForPlayer = entity.isWaitingForPlayer();
		state.idleAge = entity.level().getGameTime() % 20 + tickProgress;
		state.performanceAge = entity.getPerformanceAge(tickProgress);
	}

	@Override
	public Identifier getTextureLocation(SkippingOwlRenderState state) { return TEXTURE; }

	@Override
	protected AABB getBoundingBoxForCulling(SkippingOwlEntity entity) {
		return super.getBoundingBoxForCulling(entity).inflate(1.0);
	}
}
