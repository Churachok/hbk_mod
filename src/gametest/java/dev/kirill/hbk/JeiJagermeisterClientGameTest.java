package dev.kirill.hbk;

import dev.kirill.hbk.client.compat.jei.HbkJeiPlugin;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.registry.ModItems;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;
import java.util.Optional;

/** Explicit optional-runtime scenario: -PhbkJei=true -PhbkClientTest=jagermeister_jei. */
public final class JeiJagermeisterClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded("jei")) {
			throw new AssertionError("The jagermeister_jei scenario requires -PhbkJei=true");
		}
		var originalSelectedLanguage = context.computeOnClient(client -> client.getLanguageManager().getSelected());
		var originalOptionLanguage = context.computeOnClient(client -> client.options.languageCode);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			changeLanguage(context, "ru_ru", "ru_ru");
			world.getConnection().waitForChunksRender();
			context.waitFor(client -> HbkJeiPlugin.runtime() != null, 300);
			context.runOnClient(client -> {
				if (!Component.translatable("jei.hbk.jagermeister.info").getString().equals(
						"Наполни котёл водой. Добавь в котёл по одному цветку каждого из 20 видов. Получится 3 полные бутылки.")) {
					throw new AssertionError("The Russian memo must contain exactly the three requested sentences");
				}
				var runtime = HbkJeiPlugin.runtime();
				var manager = runtime.getRecipeManager();
				var category = manager.getRecipeCategory(HbkJeiPlugin.JAGERMEISTER);
				if (category == null) throw new AssertionError("JEI must discover the optional hbk plugin entrypoint");
				var recipes = manager.createRecipeLookup(HbkJeiPlugin.JAGERMEISTER).get().toList();
				if (!recipes.equals(List.of(HbkJeiPlugin.JagermeisterRecipe.INSTANCE))) {
					throw new AssertionError("Exactly one cauldron memo must be registered, not a crafting recipe");
				}
				var ingredients = manager.getRecipeIngredients(category, recipes.getFirst());
				var inputs = ingredients.getIngredients(RecipeIngredientRole.INPUT).stream()
						.flatMap(ingredient -> ingredient.getItemStack().stream()).toList();
				for (Item flower : JagermeisterIngredients.items()) {
					if (inputs.stream().filter(stack -> stack.is(flower)).count() != 1) {
						throw new AssertionError("Every active recipe flower needs its own JEI input icon: " + flower);
					}
				}
				if (JagermeisterIngredients.TOTAL_INGREDIENTS != 20 || inputs.size() != JagermeisterIngredients.TOTAL_INGREDIENTS + 2
						|| inputs.stream().noneMatch(stack -> stack.is(ModItems.EMPTY_JAGERMEISTER_BOTTLE))
						|| inputs.stream().noneMatch(stack -> stack.is(Items.WATER_BUCKET))
						|| inputs.stream().noneMatch(stack -> stack.is(Items.GOLDEN_DANDELION))
						|| inputs.stream().anyMatch(stack -> stack.is(Items.DANDELION))
						|| inputs.stream().anyMatch(stack -> stack.is(Items.CLOSED_EYEBLOSSOM))) {
					throw new AssertionError("The memo needs twenty flowers including golden dandelion, water and the correct empty bottle, but neither removed flower");
				}
				var outputs = ingredients.getIngredients(RecipeIngredientRole.OUTPUT).stream()
						.flatMap(ingredient -> ingredient.getItemStack().stream()).toList();
				if (outputs.size() != 1 || !outputs.getFirst().is(ModItems.JAGERMEISTER_BOTTLE)) {
					throw new AssertionError("Looking up the filled bottle must lead to the cauldron memo");
				}
				var outputFocus = runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,
						VanillaTypes.ITEM_STACK, new ItemStack(ModItems.JAGERMEISTER_BOTTLE));
				var focusedCategories = manager.createRecipeCategoryLookup().limitFocus(List.of(outputFocus)).get().toList();
				if (focusedCategories.size() != 1 || !focusedCategories.getFirst().getRecipeType().equals(HbkJeiPlugin.JAGERMEISTER)
						|| manager.createRecipeLookup(RecipeTypes.INFORMATION).limitFocus(List.of(outputFocus)).get().findAny().isPresent()) {
					throw new AssertionError("A full-bottle recipe lookup must expose only the flower cauldron category, not generic information");
				}
				var layout = manager.createRecipeLayoutDrawable(category, recipes.getFirst(),
						runtime.getJeiHelpers().getFocusFactory().createFocusGroup(List.of(outputFocus))).orElseThrow();
				// Reserve space for JEI's title, category tabs and navigation even in the default 854x480 window.
				if (layout.getRectWithBorder().getHeight() + 88 > client.getWindow().getGuiScaledHeight()) {
					throw new AssertionError("The entire cauldron memo, including its scroll box, must fit the GUI height");
				}
				for (int index = 0; index < JagermeisterIngredients.TOTAL_INGREDIENTS; index++) {
					var slot = layout.getRecipeSlotsView().findSlotByName("flower_" + index).orElseThrow();
					ItemStack displayed = slot.getDisplayedItemStack().orElseThrow();
					if (!displayed.is(JagermeisterIngredients.items().get(index))) {
						throw new AssertionError("Flower icons must be simultaneous fixed slots, not a cycling alternative");
					}
				}
				// Use the same output lookup as clicking the full bottle; do not force a category open.
				runtime.getRecipesGui().show(outputFocus);
			});
			context.waitFor(client -> client.gui.screen() != null
					&& client.gui.screen().getClass().getName().equals("mezz.jei.gui.recipes.RecipesGui"), 100);
			context.getInput().setCursorPos(0, 0);
			context.waitTicks(40);
			context.runOnClient(client -> {
				var visible = visibleRecipe(client);
				if (!visible.getRecipeCategory().getRecipeType().equals(HbkJeiPlugin.JAGERMEISTER)
						|| visible.getRecipe() != HbkJeiPlugin.JagermeisterRecipe.INSTANCE) {
					throw new AssertionError("The actual menu opened by the full-bottle lookup must display the flower cauldron recipe");
				}
				for (int index = 0; index < JagermeisterIngredients.TOTAL_INGREDIENTS; index++) {
					var flower = visible.getRecipeSlotsView().findSlotByName("flower_" + index).orElseThrow()
							.getDisplayedItemStack().orElseThrow();
					if (!flower.is(JagermeisterIngredients.items().get(index))) {
						throw new AssertionError("The opened full-bottle recipe must actually display every active flower");
					}
				}
				client.gui.hud.getChat().clearMessages(false);
			});
			context.takeScreenshot("jagermeister-jei-" + JagermeisterIngredients.TOTAL_INGREDIENTS + "-flower-cauldron-memo");
			context.runOnClient(client -> client.setScreenAndShow(null));
		} finally {
			changeLanguage(context, originalSelectedLanguage, originalOptionLanguage);
		}
	}

	private static void changeLanguage(ClientGameTestContext context, String selectedLanguage, String optionLanguage) {
		var reload = context.computeOnClient(client -> {
			client.getLanguageManager().setSelected(selectedLanguage);
			client.options.languageCode = optionLanguage;
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone(), 300);
		// Called on the gametest thread after completion, never blocking the render thread on resource loading.
		reload.getNow(null);
	}

	private static IRecipeLayoutDrawable<?> visibleRecipe(Minecraft client) {
		try {
			var screen = client.gui.screen();
			// JEI 30.39's public runtime method exposes the displayed layout; IRecipesGui itself has no category getter.
			var findLayout = screen.getClass().getMethod("getRecipeLayoutUnderMouse", double.class, double.class);
			var unwrapLayout = Class.forName("mezz.jei.gui.recipes.IRecipeLayoutWithButtons", false,
					screen.getClass().getClassLoader()).getMethod("getRecipeLayout");
			for (int y = 8; y < screen.height; y += 16) {
				for (int x = 8; x < screen.width; x += 16) {
					var hit = (Optional<?>) findLayout.invoke(screen, (double) x, (double) y);
					if (hit.isPresent()) return (IRecipeLayoutDrawable<?>) unwrapLayout.invoke(hit.get());
				}
			}
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Unable to inspect the actual JEI recipe layout with the verified 30.39 runtime API", error);
		}
		throw new AssertionError("The full-bottle output lookup must open a visible recipe layout, not an empty or fallback menu");
	}
}
