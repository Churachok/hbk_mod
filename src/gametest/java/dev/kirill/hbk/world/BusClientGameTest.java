package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.client.SovietBusMeshChecks;
import dev.kirill.hbk.entity.SovietBusEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Set;

/** Real client smoke test, screenshots and a mouse-driven end-to-end ride in an isolated normal world. */
public final class BusClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		SovietBusMeshChecks.verify();
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			ui.setGenerateStructures(true);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("fill -24 179 -24 24 179 24 minecraft:stone");
			server.runCommand("tp @a 0.5 180 0.5 0 0");
			server.runCommand("time set day");
			world.getConnection().waitForChunksRender();
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				if (SovietBusEvent.start(player) != SovietBusEvent.StartResult.STARTED) {
					throw new AssertionError("Bus must spawn in a clear prepared test scene");
				}
			});
			world.getConnection().waitForClientboundEntityUpdates(ModEntityTypes.SOVIET_BUS);
			context.waitFor(client -> client.level != null && java.util.stream.StreamSupport.stream(
					client.level.entitiesForRendering().spliterator(), false)
					.anyMatch(entity -> entity instanceof SovietBusEntity bus && bus.isWaiting()), 200);
			context.takeScreenshot("bus67-front");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.teleportTo(player.level(), -7.5, 180, -5.5, Set.of(), -36, 1, false);
			});
			context.waitTicks(10);
			context.takeScreenshot("bus67-three-quarter");
			server.runCommand("tp @a 0.5 180 0.5 0 0");
			context.waitTicks(10);
			context.waitFor(client -> client.hitResult instanceof EntityHitResult hit
					&& hit.getEntity() instanceof SovietBusEntity, 100);
			context.getInput().pressMouse(1);
			server.waitFor(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var advancement = instance.getAdvancements().get(HbkMod.id("bus_fare"));
				return player.getAdvancements().getOrStartProgress(advancement).isDone();
			}, 2600);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("bus67-arrival");
		}
	}
}
