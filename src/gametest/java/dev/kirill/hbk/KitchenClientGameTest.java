package dev.kirill.hbk;

import dev.kirill.hbk.client.FunnySpinRenderState;
import dev.kirill.hbk.entity.FunnySpinAccess;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.world.GraveyardRecipeBook;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real client checks: item sprites, guide diagrams and three-axis spin synchronization/rendering. */
public final class KitchenClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("fill -12 79 -12 12 79 12 minecraft:stone");
			server.runCommand("tp @a 0.5 80 0.5 0 0");
			server.runCommand("time set midnight");
			world.getConnection().waitForChunksRender();
			int[] ids = server.computeOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var level = player.level();
				var cow = EntityTypes.COW.spawn(level, new BlockPos(-2, 80, 6), EntitySpawnReason.COMMAND);
				var zombie = EntityTypes.ZOMBIE.spawn(level, new BlockPos(2, 80, 6), EntitySpawnReason.COMMAND);
				cow.setNoAi(true);
				zombie.setNoAi(true);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FUNNY_BUTTON));
				ModItems.FUNNY_BUTTON.use(level, player, InteractionHand.MAIN_HAND);
				return new int[]{cow.getId(), zombie.getId()};
			});
			context.waitFor(client -> client.level.getEntity(ids[0]) instanceof FunnySpinAccess cow && cow.hbk$isFunnySpinning()
					&& client.level.getEntity(ids[1]) instanceof FunnySpinAccess zombie && zombie.hbk$isFunnySpinning(), 80);
			context.runOnClient(client -> {
				var state = client.getEntityRenderDispatcher().extractEntity(client.level.getEntity(ids[0]), 0.5f);
				if (!((FunnySpinRenderState) state).hbk$isFunnySpinning()) {
					throw new AssertionError("Client render state must carry the synchronized spin flag");
				}
			});
			context.waitTicks(4);
			context.takeScreenshot("hbk-funny-spin");
			server.waitFor(instance -> {
				var entity = instance.getPlayerList().getPlayers().getFirst().level().getEntity(ids[1]);
				return entity == null || !entity.isAlive();
			}, 150);
			context.waitFor(client -> client.level.getEntity(ids[0]) instanceof FunnySpinAccess cow && !cow.hbk$isFunnySpinning(), 40);
			server.runOnServer(instance -> {
				var inventory = instance.getPlayerList().getPlayers().getFirst().getInventory();
				var items = java.util.List.of(ModItems.FUNNY_BUTTON, ModItems.DENIS_DOSHIRAK, ModItems.DOSHIRAK_KETTLE,
						ModItems.HARD_DOSHIRAK_KETTLE, ModItems.GOSHAS_DANDRUFF, ModItems.ONIGIRI,
						ModItems.KIRILL_KETTLE, ModItems.HARD_KIRILL_KETTLE);
				for (int i = 0; i < items.size(); i++) inventory.setItem(i, new ItemStack(items.get(i)));
			});
			context.waitFor(client -> client.player.getInventory().getItem(4).is(ModItems.GOSHAS_DANDRUFF)
					&& client.player.getInventory().getItem(7).is(ModItems.HARD_KIRILL_KETTLE), 40);
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(3);
			context.takeScreenshot("hbk-kitchen-items");
			var guide = context.computeOnClient(client -> new BookViewScreen(BookViewScreen.BookAccess.fromItem(GraveyardRecipeBook.create())));
			context.setScreen(() -> guide);
			context.runOnClient(client -> guide.setPage(GraveyardRecipeBook.RECIPES.indexOf("funny_button") + 1));
			context.waitTicks(3);
			context.takeScreenshot("hbk-funny-button-recipe");
			context.runOnClient(client -> guide.setPage(GraveyardRecipeBook.RECIPES.indexOf("hard_doshirak_kettle") + 1));
			context.waitTicks(3);
			context.takeScreenshot("hbk-noodle-kettle-recipe");
			context.runOnClient(client -> guide.setPage(GraveyardRecipeBook.RECIPES.size() + 3));
			context.waitTicks(3);
			context.takeScreenshot("hbk-dandruff-guide");
			context.setScreen(() -> null);
		}
	}
}
