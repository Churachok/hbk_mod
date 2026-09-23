package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.PinkFurryWolfEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class PinkFurryWolfRenderer extends HumanoidMobRenderer<PinkFurryWolfEntity, PinkFurryWolfRenderState, PinkFurryWolfModel> {
	private static final Identifier[] TEXTURES = net.minecraft.world.item.DyeColor.VALUES.stream()
			.map(color -> HbkMod.id("textures/entity/pink_furry_wolf_" + color.getName() + ".png"))
			.toArray(Identifier[]::new);

	public PinkFurryWolfRenderer(EntityRendererProvider.Context context) {
		super(context, new PinkFurryWolfModel(context.bakeLayer(ModEntityModelLayers.PINK_FURRY_WOLF)), 0.55f);
	}

	@Override
	public PinkFurryWolfRenderState createRenderState() {
		return new PinkFurryWolfRenderState();
	}

	@Override
	public void extractRenderState(PinkFurryWolfEntity entity, PinkFurryWolfRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.fleeing = entity.isFleeing();
		state.furColorId = entity.getFurColor().getId();
	}

	@Override
	public Identifier getTextureLocation(PinkFurryWolfRenderState state) {
		return TEXTURES[Math.floorMod(state.furColorId, TEXTURES.length)];
	}
}
