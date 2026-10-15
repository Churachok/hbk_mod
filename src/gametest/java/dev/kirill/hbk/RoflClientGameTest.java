package dev.kirill.hbk;

import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real client rendering and player physics, including the server's landing damage check. */
public final class RoflClientGameTest implements FabricClientGameTest {
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
			server.runCommand("time set noon");
			server.runCommand("setblock 0 80 2 hbk:rofl_mushroom");
			server.runCommand("setblock 2 80 2 hbk:rofl_block");
			server.runCommand("tp @a 0.5 80 -2.5 -12 15");
			world.getConnection().waitForChunksRender();
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
			context.takeScreenshot("hbk-rofl-mushroom-and-block");
			server.runOnServer(instance -> {
				var inventory = instance.getPlayerList().getPlayers().getFirst().getInventory();
				inventory.setItem(0, new ItemStack(ModItems.ROFL_MUSHROOM));
				inventory.setItem(1, new ItemStack(ModItems.ROFL_BLOCK));
			});
			context.waitFor(client -> client.player.getInventory().getItem(1).is(ModItems.ROFL_BLOCK), 40);
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(3);
			context.takeScreenshot("hbk-rofl-inventory");
			context.setScreen(() -> null);
			server.runCommand("setblock 0 79 0 hbk:rofl_block");
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @a 0.5 82 0.5 0 0");
			context.waitFor(client -> client.player.getY() > 90, 80);
			// Returning to ordinary stone checks both the client and server safe-fall paths.
			server.runCommand("setblock 0 79 0 minecraft:stone");
			double[] peak = {90};
			context.waitFor(client -> {
				peak[0] = Math.max(peak[0], client.player.getY());
				return client.player.onGround();
			}, 120);
			if (peak[0] - 80 < 20 || peak[0] - 80 > 30) {
				throw new AssertionError("Player bounce must reach 20-30 blocks, got " + (peak[0] - 80));
			}
			context.waitTicks(5);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				if (player.getHealth() != 20) throw new AssertionError("Rofl flight landing on stone must keep all player health");
			});
		}
	}
}
