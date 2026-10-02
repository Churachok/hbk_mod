package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KonataEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class Test3KonataRenderer extends HumanoidMobRenderer<KonataEntity, GiantBossRenderState, Test3KonataModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/konata_test2.png");

	public Test3KonataRenderer(EntityRendererProvider.Context context) {
		super(context, new Test3KonataModel(context.bakeLayer(ModEntityModelLayers.TEST3_KONATA)), 0.36f);
	}

	@Override
	protected void scale(GiantBossRenderState state, PoseStack poseStack) {
		poseStack.scale(0.92f, 0.91f, 0.92f);
	}

	@Override
	public GiantBossRenderState createRenderState() {
		return new GiantBossRenderState();
	}

	@Override
	public void extractRenderState(KonataEntity entity, GiantBossRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.konataGoodGestureProgress = entity.getGoodGestureProgress(tickProgress);
		if (entity.isGoodGestureActive()) {
			state.walkAnimationSpeed = 0.0f;
		}
	}

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) {
		return TEXTURE;
	}
}
