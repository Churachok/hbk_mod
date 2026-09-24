package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.CatgirlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class CatgirlRenderer extends HumanoidMobRenderer<CatgirlEntity, GiantBossRenderState, CatgirlModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/catgirl.png");

	public CatgirlRenderer(EntityRendererProvider.Context context) {
		super(context, new CatgirlModel(context.bakeLayer(ModEntityModelLayers.CATGIRL)), 0.5f);
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
