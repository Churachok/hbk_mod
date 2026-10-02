package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KonataEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class TestKonataRenderer extends HumanoidMobRenderer<KonataEntity, GiantBossRenderState, TestKonataModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/konata.png");

	public TestKonataRenderer(EntityRendererProvider.Context context) {
		super(context, new TestKonataModel(context.bakeLayer(ModEntityModelLayers.TEST_KONATA)), 0.4f);
	}

	@Override
	public GiantBossRenderState createRenderState() { return new GiantBossRenderState(); }

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) { return TEXTURE; }
}
