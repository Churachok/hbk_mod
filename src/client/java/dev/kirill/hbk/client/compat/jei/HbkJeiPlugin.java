package dev.kirill.hbk.client.compat.jei;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.List;

/** Optional entrypoint: Fabric only loads this class when JEI requests its plugins. */
@JeiPlugin
public final class HbkJeiPlugin implements IModPlugin {
	public static final RecipeType<JagermeisterRecipe> JAGERMEISTER = RecipeType.create(
			HbkMod.MOD_ID, "jagermeister_cauldron", JagermeisterRecipe.class);
	private static volatile IJeiRuntime runtime;

	@Override
	public Identifier getPluginUid() {
		return HbkMod.id("jei");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new JagermeisterCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(JAGERMEISTER, List.of(JagermeisterRecipe.INSTANCE));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(JAGERMEISTER, Items.CAULDRON);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime availableRuntime) {
		runtime = availableRuntime;
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
	}

	public static IJeiRuntime runtime() {
		return runtime;
	}

	public enum JagermeisterRecipe {
		INSTANCE
	}

	private static final class JagermeisterCategory implements IRecipeCategory<JagermeisterRecipe> {
		private final IDrawable icon;

		private JagermeisterCategory(IGuiHelper guiHelper) {
			icon = guiHelper.createDrawableItemLike(ModItems.JAGERMEISTER_BOTTLE);
		}

		@Override
		public RecipeType<JagermeisterRecipe> getRecipeType() {
			return JAGERMEISTER;
		}

		@Override
		public Component getTitle() {
			return Component.translatable("jei.hbk.jagermeister.recipe");
		}

		@Override
		public int getWidth() {
			return 170;
		}

		@Override
		public int getHeight() {
			return 140;
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, JagermeisterRecipe recipe, IFocusGroup focuses) {
			var flowers = JagermeisterIngredients.items();
			for (int index = 0; index < flowers.size(); index++) {
				builder.addInputSlot(2 + index % 8 * 20, 2 + index / 8 * 20)
						.setSlotName("flower_" + index).setStandardSlotBackground().add(flowers.get(index));
			}
			builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 2, 70)
					.setSlotName("cauldron").setStandardSlotBackground().add(Items.CAULDRON);
			builder.addInputSlot(42, 70).setSlotName("water").setStandardSlotBackground().add(Items.WATER_BUCKET);
			builder.addInputSlot(82, 70).setSlotName("empty_bottle").setStandardSlotBackground()
					.add(ModItems.EMPTY_JAGERMEISTER_BOTTLE);
			builder.addOutputSlot(144, 70).setSlotName("filled_bottle").setOutputSlotBackground()
					.add(ModItems.JAGERMEISTER_BOTTLE);
			builder.setShapeless(154, 44);
		}

		@Override
		public void createRecipeExtras(IRecipeExtrasBuilder builder, JagermeisterRecipe recipe, IFocusGroup focuses) {
			builder.addRecipePlusSign().setPosition(25, 73);
			builder.addRecipePlusSign().setPosition(65, 73);
			builder.addRecipeArrow().setPosition(108, 70);
			builder.addScrollBoxWidget(166, 46, 2, 92)
					.setContents(List.of(Component.translatable("jei.hbk.jagermeister.info")));
		}

		@Override
		public Identifier getIdentifier(JagermeisterRecipe recipe) {
			return HbkMod.id("jagermeister_cauldron");
		}
	}
}
