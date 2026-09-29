package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KirillSecondEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class KirillSecondRenderer extends HumanoidMobRenderer<KirillSecondEntity, GiantBossRenderState, KirillSecondModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/kirill_v2.png");

	public KirillSecondRenderer(EntityRendererProvider.Context context) {
		super(context, new KirillSecondModel(context.bakeLayer(ModEntityModelLayers.KIRILL_V2)), 0.55f);
	}

	@Override
	public void extractRenderState(KirillSecondEntity entity, GiantBossRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.unknownGrabProgress = entity.getGrabAnimationProgress(tickProgress);
		if (entity.isGrabbing()) {
			state.walkAnimationSpeed = 0.0f;
		}
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
