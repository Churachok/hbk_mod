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
	public static final List<String> RECIPES = List.of(
			"buckwheat", "stew", "condensed_milk", "currant_tincture",
			"onigiri", "kirill_kettle", "hard_kirill_kettle",
			"balalaika_pickaxe", "shovel_sword", "sickle_and_hammer", "western_chestplate",
			"flying_carpet", "improved_oak_boat", "improved_spruce_boat", "improved_birch_boat",
			"improved_jungle_boat", "improved_acacia_boat", "improved_cherry_boat",
			"improved_dark_oak_boat", "improved_pale_oak_boat", "improved_mangrove_boat",
			"improved_bamboo_raft", "stalin_spawn_egg", "cj_spawn_egg",
			"mad_liberal_spawn_egg", "kirill_doom_spawn_egg", "founding_penis",
			"redstone_pickaxe_smithing", "uranium_helmet_smithing",
			"uranium_chestplate_smithing", "uranium_leggings_smithing",
			"uranium_boots_smithing");

	private GraveyardRecipeBook() {
	}

	public static ItemStack create() {
		List<Filterable<Component>> pages = new ArrayList<>();
		pages.add(Filterable.passThrough(Component.translatable("book.hbk.recipes.page.1")));
		for (String recipe : RECIPES) {
			pages.add(Filterable.passThrough(Component.translatable(recipe.endsWith("_smithing")
					? "book.hbk.recipes.smithing" : recipe.equals("hard_kirill_kettle")
					? "book.hbk.recipes.smelting" : "book.hbk.recipes.crafting")));
		}
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT,
				new WrittenBookContent(Filterable.passThrough("HBK: рецепты / recipes"), "HBK", 0, pages, true));
		return book;
	}
}
