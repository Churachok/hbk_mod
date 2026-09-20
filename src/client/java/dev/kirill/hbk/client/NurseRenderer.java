package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.NurseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class NurseRenderer extends HumanoidMobRenderer<NurseEntity, GiantBossRenderState, GiantBossModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/nurse.png");

	public NurseRenderer(EntityRendererProvider.Context context) {
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
