package dev.kirill.hbk;

import dev.kirill.hbk.world.AnomalyZones;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

public final class AnomalyClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		int[] previousDistance = {0};
		context.runOnClient(client -> {
			previousDistance[0] = client.options.renderDistance().get();
			client.options.renderDistance().set(6);
			client.options.broadcastOptions();
		});
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
					.getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			var zones = new AnomalyZones.Zone[AnomalyZones.TYPE_COUNT];
			server.runOnServer(instance -> {
				long seed = AnomalyZones.layoutSeed(instance.overworld().getSeed());
				for (int i = 0; i < zones.length; i++) zones[i] = AnomalyZones.nearest(seed, 0, 0, i + 1);
			});
			context.waitFor(client -> AnomalyZones.clientSeed() != null, 60);
			for (int i = 0; i < zones.length; i++) {
				var zone = zones[i];
				server.runCommand("tp @a " + (zone.minX() + 96.5) + " 170 " + (zone.minZ() + 48.5) + " 0 -35");
				context.waitFor(client -> Math.abs(client.player.getX() - zone.minX() - 96.5) < 1
						&& Math.abs(client.player.getZ() - zone.minZ() - 48.5) < 1, 100);
				world.getConnection().waitForChunksRender(1200);
				context.waitTicks(10);
				context.runOnClient(client -> {
					client.gui.hud.getChat().clearMessages(true);
					double expected = 0.08 * zone.gravity(client.player.getX(), client.player.getZ());
					if (Math.abs(client.player.getGravity() - expected) > 1e-6) throw new AssertionError("Client gravity must match the radial field");
				});
				server.runOnServer(instance -> {
					int height = instance.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
							zone.minX() + 96, zone.minZ() + (zone.kind() == 2 ? 64 : 96));
					if (zone.kind() == 4) {
						var level = instance.overworld();
						if (height > level.getMinY() || !level.getBlockState(new net.minecraft.core.BlockPos(
								zone.minX() + 96, level.getMinY(), zone.minZ() + 96)).isAir()) {
							throw new AssertionError("The crater center must cut through bedrock into the void");
						}
					} else if (height < 80) throw new AssertionError("Actual world generation must create elevated terrain for anomaly " + zone.kind() + ": " + height);
				});
				double viewX = zone.minX() + (zone.kind() == 3 ? 20.5 : 96.5);
				double viewZ = zone.minZ() + (zone.kind() == 3 ? 96.5 : -16.5);
				int viewY = zone.kind() == 1 ? 218 : zone.kind() == 2 ? 160 : 150;
				int yaw = zone.kind() == 3 ? -90 : 0;
				int pitch = zone.kind() == 1 ? -4 : zone.kind() == 4 ? 55 : 8;
				server.runCommand("tp @a " + viewX + " " + viewY + " " + viewZ + " " + yaw + " " + pitch);
				context.waitFor(client -> Math.abs(client.player.getX() - viewX) < 1 && Math.abs(client.player.getZ() - viewZ) < 1, 60);
				world.getConnection().waitForChunksRender(1200);
				context.waitTicks(20);
				context.runOnClient(client -> {
					client.player.setXRot(pitch);
					client.player.xRotO = pitch;
					client.player.setYRot(yaw);
					client.player.yRotO = yaw;
					client.gui.hud.getChat().clearMessages(true);
				});
				context.waitTicks(3);
				context.takeScreenshot("hbk-anomaly-" + (i + 1));
			}
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var advancement = instance.getAdvancements().get(HbkMod.id("anomalist"));
				if (!player.getAdvancements().getOrStartProgress(advancement).isDone()) throw new AssertionError("Visiting all types must award Anomalist");
			});
			var zone = zones[3];
			server.runCommand("tp @a " + (zone.minX() + 96.5) + " 303 " + (zone.minZ() + 96.5));
			context.waitFor(client -> client.player != null && client.player.isDeadOrDying(), 60);
			context.waitTicks(3);
			context.takeScreenshot("hbk-anomaly-death");
		} finally {
			context.runOnClient(client -> {
				client.options.renderDistance().set(previousDistance[0]);
				client.options.broadcastOptions();
			});
		}
	}
}
