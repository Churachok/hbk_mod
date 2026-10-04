package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.NkvdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class NkvdRenderer extends HumanoidMobRenderer<NkvdEntity, GiantBossRenderState, GiantBossModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/nkvd.png");
	private static final Identifier GUARD_TEXTURE = HbkMod.id("textures/entity/nkvd_pipe_guard.png");

	public NkvdRenderer(EntityRendererProvider.Context context) {
		super(context, new GiantBossModel(context.bakeLayer(ModEntityModelLayers.GIANT_BOSS)), 0.5f);
	}

	@Override
	public GiantBossRenderState createRenderState() {
		return new GiantBossRenderState();
	}

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) {
		return state.pipeGuard ? GUARD_TEXTURE : TEXTURE;
	}

	@Override
	public void extractRenderState(NkvdEntity entity, GiantBossRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.pipeGuard = entity.isPipeGuard();
	}
}
