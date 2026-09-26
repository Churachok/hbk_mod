package dev.kirill.hbk.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;

public final class ImprovedBoatRenderer extends AbstractBoatRenderer {
	private final ImprovedBoatModel model;

	public ImprovedBoatRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context, texture);
		this.model = new ImprovedBoatModel(context.bakeLayer(ModEntityModelLayers.IMPROVED_BOAT));
		this.shadowRadius = 1.0f;
	}

	@Override
	protected EntityModel<BoatRenderState> model() {
		return this.model;
	}
}
