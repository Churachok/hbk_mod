package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.NkvdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class NkvdRenderer extends HumanoidMobRenderer<NkvdEntity, GiantBossRenderState, GiantBossModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/nkvd.png");

	public NkvdRenderer(EntityRendererProvider.Context context) {
		super(context, new GiantBossModel(context.bakeLayer(ModEntityModelLayers.GIANT_BOSS)), 0.5f);
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
