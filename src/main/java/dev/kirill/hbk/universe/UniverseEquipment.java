package dev.kirill.hbk.universe;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.List;

/** Ten parallel mineral sets: vanilla diamond statistics, recipes, geometry and mechanics. */
public final class UniverseEquipment {
	public static final List<String> ROLES = List.of("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots");
	private static final Item[][] ITEMS = new Item[10][9];
	private UniverseEquipment() {}
	public static void register() {
		for (int f = 0; f < 10; f++) {
			String family = UniverseMaterials.FAMILIES.get(f);
			var repair = TagKey.create(Registries.ITEM, HbkMod.id("repairs_" + family + "_equipment"));
			var baseTool = ToolMaterial.DIAMOND;
			var tool = new ToolMaterial(baseTool.incorrectBlocksForDrops(), baseTool.durability(), baseTool.speed(),
					baseTool.attackDamageBonus(), baseTool.enchantmentValue(), repair);
			var baseArmor = ArmorMaterials.DIAMOND;
			var armor = new ArmorMaterial(baseArmor.durability(), baseArmor.defense(), baseArmor.enchantmentValue(), baseArmor.equipSound(),
					baseArmor.toughness(), baseArmor.knockbackResistance(), repair, ResourceKey.create(EquipmentAssets.ROOT_ID, HbkMod.id(family)));
			for (int r = 0; r < ROLES.size(); r++) {
				var key = ResourceKey.create(Registries.ITEM, HbkMod.id(family + "_" + ROLES.get(r)));
				var properties = new Item.Properties().setId(key);
				switch (r) {
					case 0 -> properties.sword(tool, 3, -2.4f);
					case 1 -> properties.pickaxe(tool, 1, -2.8f);
					case 2 -> properties.axe(tool, 5, -3);
					case 3 -> properties.shovel(tool, 1.5f, -3);
					case 4 -> properties.hoe(tool, -3, 0);
					default -> properties.humanoidArmor(armor, switch (r) {
						case 5 -> ArmorType.HELMET; case 6 -> ArmorType.CHESTPLATE; case 7 -> ArmorType.LEGGINGS; default -> ArmorType.BOOTS;
					});
				}
				ITEMS[f][r] = Registry.register(BuiltInRegistries.ITEM, key, new Item(properties));
			}
		}
	}
	public static Item item(int family, String role) { return ITEMS[family][ROLES.indexOf(role)]; }
	public static List<String> recipes() {
		return UniverseMaterials.FAMILIES.stream().flatMap(f -> ROLES.stream().map(r -> f + "_" + r)).toList();
	}
}
