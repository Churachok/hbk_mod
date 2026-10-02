package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KonataEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class Test2KonataRenderer extends HumanoidMobRenderer<KonataEntity, GiantBossRenderState, Test2KonataModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/konata_test2.png");

	public Test2KonataRenderer(EntityRendererProvider.Context context) {
		super(context, new Test2KonataModel(context.bakeLayer(ModEntityModelLayers.TEST2_KONATA)), 0.4f);
	}

	@Override
	public GiantBossRenderState createRenderState() { return new GiantBossRenderState(); }

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) { return TEXTURE; }
}
