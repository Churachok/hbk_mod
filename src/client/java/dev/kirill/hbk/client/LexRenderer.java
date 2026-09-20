package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;

public final class LexRenderer extends CatRenderer {
	public LexRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(CatRenderState state) {
		return HbkMod.id("textures/entity/lex.png");
	}
}
