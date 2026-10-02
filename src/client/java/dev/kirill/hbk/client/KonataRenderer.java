package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KonataEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class KonataRenderer extends HumanoidMobRenderer<KonataEntity, GiantBossRenderState, KonataModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/konata.png");

	public KonataRenderer(EntityRendererProvider.Context context) {
		super(context, new KonataModel(context.bakeLayer(ModEntityModelLayers.KONATA)), 0.4f);
	}

	@Override
	public GiantBossRenderState createRenderState() { return new GiantBossRenderState(); }

	@Override
	public void extractRenderState(KonataEntity entity, GiantBossRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.konataGoodGestureProgress = entity.getGoodGestureProgress(tickProgress);
		if (entity.isGoodGestureActive()) {
			state.walkAnimationSpeed = 0.0f;
		}
	}

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) { return TEXTURE; }
}
