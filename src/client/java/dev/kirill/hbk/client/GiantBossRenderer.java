package dev.kirill.hbk.client;

import dev.kirill.hbk.entity.GiantBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class GiantBossRenderer<T extends GiantBossEntity> extends HumanoidMobRenderer<T, GiantBossRenderState, GiantBossModel> {
	private final Identifier texture;

	public GiantBossRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context, new GiantBossModel(context.bakeLayer(ModEntityModelLayers.GIANT_BOSS)), 3.0f);
		this.texture = texture;
	}

	@Override
	public void extractRenderState(T entity, GiantBossRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.scale = GiantBossEntity.MODEL_SCALE;
	}

	@Override
	public GiantBossRenderState createRenderState() {
		return new GiantBossRenderState();
	}

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) {
		return this.texture;
	}
}
