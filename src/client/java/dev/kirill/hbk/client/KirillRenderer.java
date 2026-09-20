package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KirillEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class KirillRenderer extends HumanoidMobRenderer<KirillEntity, GiantBossRenderState, GiantBossModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/kirill.png");

	public KirillRenderer(EntityRendererProvider.Context context) {
		super(context, new GiantBossModel(context.bakeLayer(ModEntityModelLayers.GIANT_BOSS), false), 0.45f);
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
