package dev.kirill.hbk.client;

import dev.kirill.hbk.entity.SquirrelEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Checks the two-legged pose and actual bottle rendering in an isolated flat world. */
public final class SquirrelClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			var root = SquirrelModel.createBodyLayer().bakeRoot();
			var model = new SquirrelModel(root);
			var state = new SquirrelRenderState();
			state.uprightAmount = 1;
			state.giveProgress = 1;
			model.setupAnim(state);
			if (root.getChild("front_right").y >= root.getChild("hind_right").y
					|| Math.abs(root.getChild("front_right").xRot + Math.PI / 2) > 0.001) {
				throw new AssertionError("The front paw must be raised and extended while standing on the hind legs");
			}
			float tail = root.getChild("tail").yRot;
			state.ageInTicks = 10;
			model.setupAnim(state);
			if (root.getChild("tail").yRot == tail) throw new AssertionError("The idle tail must keep moving");
			state.uprightAmount = state.giveProgress = 0;
			model.setupAnim(state);
			if (root.getChild("front_right").y != 20 || root.getChild("body").xRot != 0) {
				throw new AssertionError("The walking pose must restore all four paws");
			}
		});
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("tp @a 0.5 180 0.5 0 22");
			server.runCommand("time set day");
			world.getConnection().waitForChunksRender();
			server.runCommand("fill -12 179 -12 12 179 12 minecraft:stone");
			// The player may fall while the high-altitude chunks load, before the floor exists.
			server.runCommand("tp @a 0.5 180 0.5 0 30");
			context.waitTicks(2);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var squirrel = ModEntityTypes.SQUIRREL.create(player.level(), EntitySpawnReason.EVENT);
				squirrel.setEncounterOwner(player);
				squirrel.snapTo(player.position().add(0, 0, 1.6));
				squirrel.setOnGround(true);
				squirrel.tickCount = SquirrelEntity.LIFETIME_TICKS;
				player.level().addFreshEntity(squirrel);
			});
			context.waitFor(client -> client.level != null && java.util.stream.StreamSupport.stream(
					client.level.entitiesForRendering().spliterator(), false).anyMatch(entity ->
					entity instanceof SquirrelEntity squirrel && squirrel.getGiveProgress(0) > 0.75
							&& squirrel.getMainHandItem().is(ModItems.BEER_BOTTLE)), 80);
			context.takeScreenshot("squirrel-beer-handover");
			server.waitFor(instance -> instance.getPlayerList().getPlayers().getFirst().getInventory()
					.contains(stack -> stack.is(ModItems.BEER_BOTTLE)), 80);
			context.waitFor(client -> client.level != null && java.util.stream.StreamSupport.stream(
					client.level.entitiesForRendering().spliterator(), false)
					.noneMatch(entity -> entity instanceof SquirrelEntity), 80);
		}
	}
}
