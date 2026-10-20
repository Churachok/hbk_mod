package dev.kirill.hbk;

import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import dev.kirill.hbk.client.TimeFurnaceScreen;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class TimeFurnaceClientGameTest implements FabricClientGameTest {
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
			server.runCommand("fill -6 79 -6 6 79 6 minecraft:stone");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("tp @a 2.8 80 -3.5 28 12");
			var pos = new BlockPos(0, 80, 0);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.level().setBlockAndUpdate(pos, ModBlocks.TIME_FURNACE.defaultBlockState());
				ModBlocks.TIME_FURNACE.setPlacedBy(player.level(), pos, player.level().getBlockState(pos), player, new ItemStack(ModItems.TIME_FURNACE));
				player.getInventory().setItem(0, new ItemStack(ModItems.TIME_FURNACE));
			});
			world.getConnection().waitForChunksRender();
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
			context.takeScreenshot("hbk-time-furnace-world");
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(2);
			context.takeScreenshot("hbk-time-furnace-item");
			context.setScreen(() -> null);
			openFromChimney(server, pos);
			context.waitForScreen(TimeFurnaceScreen.class);
			context.runOnClient(client -> {
				var screen = (TimeFurnaceScreen) client.gui.screen();
				var field = (EditBox) screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
				field.insertText("12a45");
				if (!field.getValue().isEmpty()) throw new AssertionError("Letters must be rejected");
				field.insertText("00123");
			});
			context.waitTicks(2);
			context.takeScreenshot("hbk-time-furnace-screen");
			context.clickScreenButton("Save");
			server.waitFor(instance -> ((TimeFurnaceBlockEntity) instance.getPlayerList().getPlayers().getFirst()
					.level().getBlockEntity(pos)).dimensionCode().equals("00123"), 40);
			openFromChimney(server, pos);
			context.waitForScreen(TimeFurnaceScreen.class);
			context.runOnClient(client -> {
				var field = (EditBox) client.gui.screen().children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
				if (!field.getValue().equals("00123")) throw new AssertionError("Reopening must synchronize leading zeros from the server");
				field.setValue("99999");
			});
			context.getInput().pressKey(GLFW.GLFW_KEY_TAB);
			context.getInput().pressKey(GLFW.GLFW_KEY_TAB);
			context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
			server.runOnServer(instance -> {
				var furnace = (TimeFurnaceBlockEntity) instance.getPlayerList().getPlayers().getFirst().level().getBlockEntity(pos);
				if (!furnace.dimensionCode().equals("00123")) throw new AssertionError("Cancel must not change the destination");
			});
		}
	}

	private static void openFromChimney(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, BlockPos pos) {
		server.runOnServer(instance -> {
			var player = instance.getPlayerList().getPlayers().getFirst();
			player.level().getBlockState(pos.above()).useWithoutItem(player.level(), player,
					new BlockHitResult(Vec3.atCenterOf(pos.above()), Direction.NORTH, pos.above(), false));
		});
	}
}
