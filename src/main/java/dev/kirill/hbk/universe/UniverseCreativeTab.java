package dev.kirill.hbk.universe;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One ordered catalogue, shared by the creative tab and the main HBK tab's exclusion. */
public final class UniverseCreativeTab {
	public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, HbkMod.id("worlds"));
	private static final Set<String> CONTENTS = contents();
	private UniverseCreativeTab() {}
	private static Set<String> contents() {
		Set<String> ids = new LinkedHashSet<>(List.of("time_furnace", "redstone_pickaxe", "fractured_block", "spatial_shard"));
		for (int f = 0; f < 10; f++) {
			String family = UniverseMaterials.FAMILIES.get(f), species = UniverseBotany.SPECIES.get(f);
			UniverseMaterials.ROLES.forEach(r -> ids.add(family + "_" + r));
			ids.add(family + "_loam");
			UniverseMaterials.RESOURCES.forEach(r -> ids.add(family + "_" + r));
			UniverseBotany.ROLES.forEach(r -> ids.add(species + "_" + r));
			ids.add(species + "_fruit"); ids.add(species + "_ration");
			UniverseEquipment.ROLES.forEach(r -> ids.add(family + "_" + r));
		}
		for (var alloy : UniverseAlloys.CATALOG) {
			for (String suffix : List.of("sample", "ingot", "block")) ids.add(alloy.id() + "_" + suffix);
			alloy.rewards().forEach(r -> ids.add(r.id()));
		}
		return java.util.Collections.unmodifiableSet(ids);
	}
	public static boolean contains(Item item) {
		var id = BuiltInRegistries.ITEM.getKey(item);
		return id.getNamespace().equals(HbkMod.MOD_ID) && CONTENTS.contains(id.getPath());
	}
	public static List<Item> items() {
		return CONTENTS.stream().map(id -> BuiltInRegistries.ITEM.getValue(HbkMod.id(id))).toList();
	}
	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.hbk.worlds"))
				.icon(() -> new ItemStack(ModItems.TIME_FURNACE))
				.displayItems((parameters, output) -> items().forEach(output::accept)).build());
	}
}
