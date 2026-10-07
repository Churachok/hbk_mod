package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SquirrelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class SquirrelRenderer extends MobRenderer<SquirrelEntity, LivingEntityRenderState, SquirrelModel> {
	private static final Identifier TEXTURE = HbkMod.id("textures/entity/squirrel.png");

	public SquirrelRenderer(EntityRendererProvider.Context context) {
		super(context, new SquirrelModel(context.bakeLayer(ModEntityModelLayers.SQUIRREL)), 0.25f);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
