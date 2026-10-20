package dev.kirill.hbk.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/** A written field guide to every recipe currently shipped in data/hbk/recipe. */
public final class GraveyardRecipeBook {
	public static final List<String> DIAGRAM_RECIPES = java.util.stream.Stream.concat(List.of(
			"buckwheat", "stew", "condensed_milk", "currant_tincture",
			"onigiri", "kirill_kettle", "hard_kirill_kettle",
			"denis_doshirak", "doshirak_kettle", "hard_doshirak_kettle", "funny_button",
			"balalaika_pickaxe", "shovel_sword", "sickle_and_hammer", "western_chestplate",
			"flying_carpet", "improved_oak_boat", "improved_spruce_boat", "improved_birch_boat",
			"improved_jungle_boat", "improved_acacia_boat", "improved_cherry_boat",
			"improved_dark_oak_boat", "improved_pale_oak_boat", "improved_mangrove_boat",
			"improved_bamboo_raft", "stalin_spawn_egg", "cj_spawn_egg",
			"mad_liberal_spawn_egg", "kirill_doom_spawn_egg", "founding_penis",
			"redstone_pickaxe_smithing", "uranium_helmet_smithing",
			"uranium_chestplate_smithing", "uranium_leggings_smithing",
			"uranium_boots_smithing", "time_furnace").stream(),
			dev.kirill.hbk.universe.UniverseMaterials.recipes().stream()).toList();
	/** Complete recipe catalogue; the larger botanical sets have two compact guide pages per family. */
	public static final List<String> RECIPES = java.util.stream.Stream.of(DIAGRAM_RECIPES,
			dev.kirill.hbk.universe.UniverseBotany.recipes(), dev.kirill.hbk.universe.UniverseEquipment.recipes(), dev.kirill.hbk.universe.UniverseAlloys.recipes()).flatMap(List::stream).toList();
	public static final List<String> INFO_PAGES = java.util.stream.Stream.concat(List.of("doshirak", "funny_button", "dandruff", "feeding_onigiri", "lebedev_head", "bus",
			"liberal_blood", "young_liberal", "universe_travel", "universe_ecology", "universe_equipment", "universe_mining", "universe_alloys").stream(),
			dev.kirill.hbk.universe.UniverseBotany.SPECIES.stream().flatMap(s -> java.util.stream.Stream.of(s + "_flora", s + "_crafts"))).toList();
	public static final int PAGE_COUNT = 1 + DIAGRAM_RECIPES.size() + INFO_PAGES.size();

	private GraveyardRecipeBook() {
	}

	public static ItemStack create() {
		List<Filterable<Component>> pages = new ArrayList<>();
		pages.add(Filterable.passThrough(Component.translatable("book.hbk.recipes.page.1")));
		for (String recipe : DIAGRAM_RECIPES) {
			pages.add(Filterable.passThrough(Component.translatable(recipe.endsWith("_smithing")
					? "book.hbk.recipes.smithing" : recipe.equals("hard_kirill_kettle") || recipe.equals("hard_doshirak_kettle")
					? "book.hbk.recipes.smelting" : "book.hbk.recipes.crafting")));
		}
		for (String page : INFO_PAGES) {
			pages.add(Filterable.passThrough(Component.translatable("book.hbk.recipes.info." + page)));
		}
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT,
				new WrittenBookContent(Filterable.passThrough("HBK: рецепты / recipes"), "HBK", 0, pages, true));
		return book;
	}
}
