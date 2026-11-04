package dev.kirill.hbk;

import com.mojang.blaze3d.platform.NativeImage;
import dev.kirill.hbk.client.GodEncounterClient;
import dev.kirill.hbk.entity.GodEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModSounds;
import dev.kirill.hbk.world.GodEncounter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

public final class GodClientGameTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			try (var stream = client.getResourceManager().getResourceOrThrow(HbkMod.id("textures/entity/god_atlas.png")).open();
				 var image = NativeImage.read(stream)) {
				if (image.getWidth() != 2048 || image.getHeight() != 2560) throw new AssertionError("GOD needs 80 video frames");
				for (int frame = 0; frame < 80; frame++) {
					int x = frame % 8 * 256, y = frame / 8 * 256;
					if ((image.getPixel(x, y) >>> 24) != 0 || (image.getPixel(x + 255, y + 255) >>> 24) != 0
							|| (image.getPixel(x + 128, y + 128) >>> 24) != 255)
						throw new AssertionError("Every Telegram frame must have transparent corners and an opaque face");
				}
			} catch (IOException error) { throw new UncheckedIOException(error); }
			if (client.getSoundManager().getSoundEvent(ModSounds.GOD_LOOP.location()) == null)
				throw new AssertionError("The video's loop must load in the client sound registry");
			if (!(GodEncounterClient.volumeAt(200) > 0 && GodEncounterClient.volumeAt(200) < GodEncounterClient.volumeAt(100)
					&& GodEncounterClient.volumeAt(100) < GodEncounterClient.volumeAt(10) && GodEncounterClient.volumeAt(0) == 1))
				throw new AssertionError("The loop must remain audible at spawn and get louder on approach");
		});
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("tp @a 0.5 180 0.5 0 0");
			server.runCommand("time set day");
			world.getConnection().waitForChunksRender();
			server.runCommand("execute as @p at @s run god");
			context.waitFor(client -> GodEncounterClient.active() && GodEncounterClient.titleTicks() > 0, 100);
			context.waitFor(client -> GodEncounterClient.soundPlaying(), 20);
			context.takeScreenshot("god-arrival-title");
			world.getConnection().waitForClientboundEntityUpdates(ModEntityTypes.GOD);
			context.waitTicks(42);
			context.runOnClient(client -> {
				if (GodEncounterClient.titleTicks() != 0) throw new AssertionError("The full-screen title must end after two seconds");
				if (java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
						.noneMatch(entity -> entity instanceof GodEntity)) throw new AssertionError("GOD must render even outside simulation distance");
			});
			context.waitTicks(45);
			context.runOnClient(client -> {
				if (!GodEncounterClient.active() || !GodEncounterClient.soundPlaying())
					throw new AssertionError("The video sound must keep looping after its first four seconds");
			});
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var position = GodEncounter.get(instance).chase(player).position();
				player.teleportTo(player.level(), position.x, position.y - player.getBbHeight() / 2, position.z - 18,
						Set.of(), 0, 0, false);
			});
			context.waitTicks(4);
			context.takeScreenshot("god-nextbot-circle");
			server.waitFor(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				return !player.isAlive() && player.getAdvancements().getOrStartProgress(instance.getAdvancements().get(HbkMod.id("god"))).isDone();
			}, 120);
			context.waitFor(client -> !GodEncounterClient.active() && !GodEncounterClient.soundPlaying(), 40);
			context.takeScreenshot("god-caught-advancement");
		}
	}
}
