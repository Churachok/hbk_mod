package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class ModEntityModelLayers {
	public static final ModelLayerLocation GIANT_BOSS = new ModelLayerLocation(HbkMod.id("giant_boss"), "main");
	public static final ModelLayerLocation FLYING_CARPET = new ModelLayerLocation(HbkMod.id("flying_carpet"), "main");

	public static void register() {
		ModelLayerRegistry.registerModelLayer(GIANT_BOSS, GiantBossModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(FLYING_CARPET, FlyingCarpetModel::createBodyLayer);
	}
}
