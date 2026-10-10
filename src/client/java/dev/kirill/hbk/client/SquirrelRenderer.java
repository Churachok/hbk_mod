package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SquirrelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemDisplayContext;

public final class SquirrelRenderer extends MobRenderer<SquirrelEntity, SquirrelRenderState, SquirrelModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/squirrel.png");
	private final ItemModelResolver itemModelResolver;

	public SquirrelRenderer(EntityRendererProvider.Context context) {
		super(context, new SquirrelModel(context.bakeLayer(ModEntityModelLayers.SQUIRREL)), 0.25f);
		this.itemModelResolver = context.getItemModelResolver();
		this.addLayer(new SquirrelBeerLayer(this));
	}

	@Override
	public SquirrelRenderState createRenderState() {
		return new SquirrelRenderState();
	}

	@Override
	public void extractRenderState(SquirrelEntity entity, SquirrelRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.uprightAmount = entity.getUprightAmount(tickProgress);
		state.giveProgress = entity.getGiveProgress(tickProgress);
		this.itemModelResolver.updateForLiving(state.beer, entity.getMainHandItem(), ItemDisplayContext.FIXED, entity);
	}

	@Override
	public Identifier getTextureLocation(SquirrelRenderState state) {
		return TEXTURE;
	}
}
