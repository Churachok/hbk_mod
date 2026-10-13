package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.GraveyardRecipePages;
import dev.kirill.hbk.world.GraveyardRecipeBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin {
	@Shadow private BookViewScreen.BookAccess bookAccess;
	@Shadow private int currentPage;

	@Inject(method = "init", at = @At("TAIL"))
	private void hbk$upgradeOldGuide(CallbackInfo ci) {
		if (bookAccess.getPageCount() != GraveyardRecipeBook.PAGE_COUNT && hbk$isGuide()) {
			((BookViewScreen) (Object) this).setBookAccess(
					BookViewScreen.BookAccess.fromItem(GraveyardRecipeBook.create()));
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void hbk$drawRecipeDiagram(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo ci) {
		if (bookAccess.getPageCount() != GraveyardRecipeBook.PAGE_COUNT
				|| !hbk$isGuide()) {
			return;
		}
		GraveyardRecipePages.render(graphics, currentPage, ((BookViewScreen) (Object) this).width, mouseX, mouseY);
	}

	private boolean hbk$isGuide() {
		return bookAccess.getPageCount() > 0
				&& bookAccess.getPage(0).getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("book.hbk.recipes.page.1");
	}
}
