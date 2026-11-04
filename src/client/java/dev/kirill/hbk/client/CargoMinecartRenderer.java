package dev.kirill.hbk.client;

/** Distinct renderer identity lets the texture mixin leave every vanilla minecart untouched. */
public final class CargoMinecartRenderer extends net.minecraft.client.renderer.entity.MinecartRenderer {
	public CargoMinecartRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super(context, net.minecraft.client.model.geom.ModelLayers.CHEST_MINECART);
	}
}
