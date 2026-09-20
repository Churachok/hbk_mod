package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.ReferenceNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class ReferenceNpcRenderer extends HumanoidMobRenderer<ReferenceNpcEntity, GiantBossRenderState, GiantBossModel> {
	private final Identifier texture;

	public ReferenceNpcRenderer(EntityRendererProvider.Context context, String name) {
		super(context, new GiantBossModel(context.bakeLayer(name.equals("anton")
				? ModEntityModelLayers.ANTON : ModEntityModelLayers.GIANT_BOSS), false), 0.45f);
		this.texture = HbkMod.id("textures/entity/" + name + ".png");
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
