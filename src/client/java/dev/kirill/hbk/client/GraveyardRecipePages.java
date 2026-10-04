package dev.kirill.hbk.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.GraveyardRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws item slots over the recipe pages of the graveyard's written book. */
public final class GraveyardRecipePages {
	private static final Map<String, RecipeDiagram> CACHE = new HashMap<>();

	private GraveyardRecipePages() {
	}

	public static void render(GuiGraphicsExtractor graphics, int page, int screenWidth, int mouseX, int mouseY) {
		if (page < 1 || page > GraveyardRecipeBook.RECIPES.size()) {
			return;
		}
		RecipeDiagram diagram = CACHE.computeIfAbsent(GraveyardRecipeBook.RECIPES.get(page - 1), GraveyardRecipePages::load);
		if (diagram == null) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		int left = (screenWidth - 192) / 2 + 36;
		int top = 2;
		List<net.minecraft.util.FormattedCharSequence> title = font.split(diagram.result.getHoverName(), 114);
		for (int line = 0; line < Math.min(2, title.size()); line++) {
			graphics.text(font, title.get(line), left, top + 45 + line * 10, 0x38271C, false);
		}
		if (diagram.smithing) {
			for (int slot = 0; slot < 3; slot++) {
				drawSlot(graphics, font, diagram.inputs.get(slot), left + slot * 24, top + 89, mouseX, mouseY);
			}
			graphics.text(font, "→", left + 72, top + 94, 0x38271C, false);
			drawSlot(graphics, font, diagram.result, left + 92, top + 89, mouseX, mouseY);
		} else {
			for (int slot = 0; slot < 9; slot++) {
				drawSlot(graphics, font, diagram.inputs.get(slot),
						left + slot % 3 * 18, top + 76 + slot / 3 * 18, mouseX, mouseY);
			}
			graphics.text(font, "→", left + 58, top + 97, 0x38271C, false);
			drawSlot(graphics, font, diagram.result, left + 79, top + 94, mouseX, mouseY);
			graphics.text(font, Component.translatable(diagram.shapeless
					? "book.hbk.recipes.shapeless" : "book.hbk.recipes.shaped"),
					left, top + 139, 0x5B4737, false);
		}
	}

	private static void drawSlot(GuiGraphicsExtractor graphics, Font font, ItemStack item,
			int x, int y, int mouseX, int mouseY) {
		graphics.fill(x, y, x + 18, y + 18, 0xFF7A6854);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6AF87);
		if (!item.isEmpty()) {
			graphics.item(item, x + 1, y + 1);
			graphics.itemDecorations(font, item, x + 1, y + 1);
			if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
				graphics.setTooltipForNextFrame(font, item, mouseX, mouseY);
			}
		}
	}

	private static RecipeDiagram load(String name) {
		String path = "data/hbk/recipe/" + name + ".json";
		try (var stream = GraveyardRecipePages.class.getClassLoader().getResourceAsStream(path)) {
			if (stream == null) {
				HbkMod.LOGGER.warn("Missing graveyard recipe diagram: {}", path);
				return null;
			}
			JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject result = json.getAsJsonObject("result");
			ItemStack output = stack(result.get("id").getAsString(),
					result.has("count") ? result.get("count").getAsInt() : 1);
			List<ItemStack> inputs = new ArrayList<>();
			String type = json.get("type").getAsString();
			if (type.equals("minecraft:smithing_transform")) {
				inputs.add(stack(json.get("template").getAsString(), 1));
				inputs.add(stack(json.get("base").getAsString(), 1));
				inputs.add(stack(json.get("addition").getAsString(), 1));
				return new RecipeDiagram(inputs, output, false, true);
			}
			if (type.equals("minecraft:crafting_shaped")) {
				var pattern = json.getAsJsonArray("pattern");
				var keys = json.getAsJsonObject("key");
				for (int row = 0; row < 3; row++) {
					String symbols = row < pattern.size() ? pattern.get(row).getAsString() : "";
					for (int col = 0; col < 3; col++) {
						String symbol = col < symbols.length() ? symbols.substring(col, col + 1) : " ";
						inputs.add(symbol.equals(" ") ? ItemStack.EMPTY : stack(keys.get(symbol).getAsString(), 1));
					}
				}
				return new RecipeDiagram(inputs, output, false, false);
			}
			for (var ingredient : json.getAsJsonArray("ingredients")) {
				inputs.add(stack(ingredient.getAsString(), 1));
			}
			while (inputs.size() < 9) {
				inputs.add(ItemStack.EMPTY);
			}
			return new RecipeDiagram(inputs, output, true, false);
		} catch (Exception error) {
			HbkMod.LOGGER.warn("Could not read graveyard recipe diagram {}", path, error);
			return null;
		}
	}

	private static ItemStack stack(String id, int count) {
		if (id.equals("#minecraft:wool_carpets")) {
			return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:white_carpet")), count);
		}
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)), count);
	}

	private record RecipeDiagram(List<ItemStack> inputs, ItemStack result, boolean shapeless, boolean smithing) {
	}
}
