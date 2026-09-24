package dev.kirill.hbk.client;

import dev.kirill.hbk.entity.HumanoidBossEntity;
import dev.kirill.hbk.entity.MadLiberalEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class HumanoidBossRenderer<T extends HumanoidBossEntity>
		extends HumanoidMobRenderer<T, GiantBossRenderState, GiantBossModel> {
	private final Identifier[] textures;
	private final float scale;

	public HumanoidBossRenderer(EntityRendererProvider.Context context, float scale, Identifier... textures) {
		this(context, ModEntityModelLayers.GIANT_BOSS, scale, textures);
	}

	public HumanoidBossRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
			float scale, Identifier... textures) {
		super(context, layer == ModEntityModelLayers.MAD_LIBERAL
				? new MadLiberalModel(context.bakeLayer(layer))
				: new GiantBossModel(context.bakeLayer(layer), true), 0.65f);
		this.textures = textures;
		this.scale = scale;
	}

	@Override
	public void extractRenderState(T entity, GiantBossRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.scale = this.scale;
		state.armorCrackStage = entity instanceof MadLiberalEntity liberal ? liberal.getArmorCrackStage() : 0;
	}

	@Override
	public GiantBossRenderState createRenderState() {
		return new GiantBossRenderState();
	}

	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) {
		return this.textures[Math.min(state.armorCrackStage, this.textures.length - 1)];
	}
}
