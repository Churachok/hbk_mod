package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/** Materials for equipment added by the mod. */
public final class ModArmorMaterials {
	private static final TagKey<Item> REPAIRS_URANIUM_ARMOR = TagKey.create(
			net.minecraft.core.registries.Registries.ITEM, HbkMod.id("repairs_uranium_armor")
	);
	private static final ResourceKey<EquipmentAsset> URANIUM_ASSET = EquipmentAssets.createId("uranium");

	/** Tougher than netherite, with a dedicated client-side equipment texture. */
	public static final ArmorMaterial URANIUM = new ArmorMaterial(
			45,
			Map.of(
					ArmorType.BOOTS, 4,
					ArmorType.LEGGINGS, 7,
					ArmorType.CHESTPLATE, 9,
					ArmorType.HELMET, 4,
					ArmorType.BODY, 9
			),
			18,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			3.5f,
			0.15f,
			REPAIRS_URANIUM_ARMOR,
			URANIUM_ASSET
	);

	private ModArmorMaterials() {
	}
}
