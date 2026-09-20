package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;

public class ModEntityModelLayers {
	public static final ModelLayerLocation GIANT_BOSS = new ModelLayerLocation(HbkMod.id("giant_boss"), "main");
	public static final ModelLayerLocation ANTON = new ModelLayerLocation(HbkMod.id("anton"), "main");
	public static final ModelLayerLocation FLYING_CARPET = new ModelLayerLocation(HbkMod.id("flying_carpet"), "main");

	public static void register() {
		ModelLayerRegistry.registerModelLayer(GIANT_BOSS, GiantBossModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ANTON, ReferenceNpcModel::createAntonBodyLayer);
		ModelLayerRegistry.registerModelLayer(FLYING_CARPET, FlyingCarpetModel::createBodyLayer);
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			for (var material : UraniumArmorModel.Material.values()) {
				ModelLayerRegistry.registerModelLayer(uraniumArmor(slot, material), () -> UraniumArmorModel.createLayer(slot, material));
			}
		}
	}

	public static ModelLayerLocation uraniumArmor(EquipmentSlot slot, UraniumArmorModel.Material material) {
		return new ModelLayerLocation(HbkMod.id("uranium_armor"), slot.getName() + "_" + material.name().toLowerCase(java.util.Locale.ROOT));
	}
}
