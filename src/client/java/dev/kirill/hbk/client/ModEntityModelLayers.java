package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;

public class ModEntityModelLayers {
	public static final ModelLayerLocation GIANT_BOSS = new ModelLayerLocation(HbkMod.id("giant_boss"), "main");
	public static final ModelLayerLocation MAD_LIBERAL = new ModelLayerLocation(HbkMod.id("mad_liberal"), "main");
	public static final ModelLayerLocation ANTON = new ModelLayerLocation(HbkMod.id("anton"), "main");
	public static final ModelLayerLocation FLYING_CARPET = new ModelLayerLocation(HbkMod.id("flying_carpet"), "main");
	public static final ModelLayerLocation IMPROVED_BOAT = new ModelLayerLocation(HbkMod.id("improved_boat"), "main");
	public static final ModelLayerLocation PINK_FURRY_WOLF = new ModelLayerLocation(HbkMod.id("pink_furry_wolf"), "main");
	public static final ModelLayerLocation CATGIRL = new ModelLayerLocation(HbkMod.id("catgirl"), "main");
	public static final ModelLayerLocation SQUIRREL = new ModelLayerLocation(HbkMod.id("squirrel"), "main");
	public static final ModelLayerLocation KONATA = new ModelLayerLocation(HbkMod.id("konata"), "main");
	public static final ModelLayerLocation TEST_KONATA = new ModelLayerLocation(HbkMod.id("test"), "main");
	public static final ModelLayerLocation TEST2_KONATA = new ModelLayerLocation(HbkMod.id("test2"), "main");
	public static final ModelLayerLocation TEST3_KONATA = new ModelLayerLocation(HbkMod.id("test3"), "main");
	public static final ModelLayerLocation KIRILL_V2 = new ModelLayerLocation(HbkMod.id("kirill_v2"), "main");

	public static void register() {
		for (var material : KirillGlassesModel.Material.values()) {
			ModelLayerRegistry.registerModelLayer(kirillGlasses(material), () -> KirillGlassesModel.createLayer(material));
		}
		ModelLayerRegistry.registerModelLayer(GIANT_BOSS, GiantBossModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(MAD_LIBERAL, MadLiberalModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ANTON, ReferenceNpcModel::createAntonBodyLayer);
		ModelLayerRegistry.registerModelLayer(FLYING_CARPET, FlyingCarpetModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(IMPROVED_BOAT, ImprovedBoatModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(PINK_FURRY_WOLF, PinkFurryWolfModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(CATGIRL, CatgirlModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SQUIRREL, SquirrelModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(KONATA, KonataModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(TEST_KONATA, TestKonataModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(TEST2_KONATA, Test2KonataModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(TEST3_KONATA, Test3KonataModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(KIRILL_V2, KirillSecondModel::createBodyLayer);
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			for (var material : UraniumArmorModel.Material.values()) {
				ModelLayerRegistry.registerModelLayer(uraniumArmor(slot, material), () -> UraniumArmorModel.createLayer(slot, material));
			}
		}
	}

	public static ModelLayerLocation uraniumArmor(EquipmentSlot slot, UraniumArmorModel.Material material) {
		return new ModelLayerLocation(HbkMod.id("uranium_armor"), slot.getName() + "_" + material.name().toLowerCase(java.util.Locale.ROOT));
	}

	public static ModelLayerLocation kirillGlasses(KirillGlassesModel.Material material) {
		return new ModelLayerLocation(HbkMod.id("kirill_glasses"), material.name().toLowerCase(java.util.Locale.ROOT));
	}
}
