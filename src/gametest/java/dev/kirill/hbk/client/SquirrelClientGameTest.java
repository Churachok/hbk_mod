package dev.kirill.hbk.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SquirrelEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Checks the two-legged pose and actual bottle rendering in an isolated flat world. */
public final class SquirrelClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			try (var stream = client.getResourceManager().getResourceOrThrow(
					HbkMod.id("textures/entity/squirrel.png")).open(); var texture = NativeImage.read(stream)) {
				if (texture.getWidth() != 64 || texture.getHeight() != 64) {
					throw new AssertionError("The tail UV atlas must remain 64x64");
				}
				for (int y = 26; y < 28; y++) {
					for (int x = 24; x < 36; x++) {
						int expected = y == 26 && (x == 27 || x == 28) ? 0xFF4E2718 : 0xFFF4D39E;
						if (texture.getPixel(x, y) != expected) {
							throw new AssertionError("The nose must be centered on the muzzle front, not a side face");
						}
					}
				}
				var rearColors = new java.util.HashSet<Integer>();
				for (int y = 40; y < 49; y++) {
					for (int x = 0; x < 20; x++) {
						int pixel = texture.getPixel(x, y);
						if ((pixel >>> 24) != 255) throw new AssertionError("Every tail face must be opaque");
						if (x >= 14 && y >= 44) rearColors.add(pixel);
					}
				}
				if (rearColors.size() < 3) throw new AssertionError("The tail rear must have visible fur texture, not a flat patch");
				var tipColors = new java.util.HashSet<Integer>();
				for (int y = 40; y < 49; y++) {
					for (int x = 32; x < 52; x++) {
						int pixel = texture.getPixel(x, y);
						if ((pixel >>> 24) != 255 || (pixel >> 16 & 255) < 240
								|| (pixel >> 8 & 255) < 220 || (pixel & 255) < 185) {
							throw new AssertionError("All tail-tip faces must have opaque pale fur");
						}
						tipColors.add(pixel);
					}
				}
				if (tipColors.size() < 3) throw new AssertionError("The light tip must retain pixel fur detail");
			} catch (IOException error) {
				throw new UncheckedIOException(error);
			}
			if (client.getSoundManager().getSoundEvent(ModSounds.SQUIRREL_GIFT.location()) == null) {
				throw new AssertionError("The gift sound must load in the client sound registry");
			}
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
			var random = RandomSource.create(0);
			float bodyEdge = root.getChild("body").getRandomCube(random).maxX;
			for (var name : new String[]{"front_left", "front_right", "hind_left", "hind_right"}) {
				var paw = root.getChild(name);
				var cube = paw.getRandomCube(random);
				float outerEdge = Math.abs(paw.x + (name.endsWith("left") ? cube.maxX : cube.minX));
				if (outerEdge - bodyEdge < 0.15f) {
					throw new AssertionError("Paw and body side faces must not be coplanar: " + name);
				}
			}
			var tip = root.getChild("tail").getChild("middle").getChild("tip").getRandomCube(random);
			for (var face : tip.polygons) {
				for (var vertex : face.vertices()) {
					if (vertex.u() < 32f / 64 || vertex.u() > 52f / 64
							|| vertex.v() < 40f / 64 || vertex.v() > 49f / 64) {
						throw new AssertionError("The terminal tail segment must use its own light UV net");
					}
				}
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
				squirrel.snapTo(player.getX(), player.getY(), player.getZ() + 2, 0, 0);
				squirrel.setNoAi(true);
				squirrel.setOnGround(true);
				player.level().addFreshEntity(squirrel);
			});
			context.waitFor(client -> client.level != null && java.util.stream.StreamSupport.stream(
					client.level.entitiesForRendering().spliterator(), false).anyMatch(entity -> entity instanceof SquirrelEntity), 80);
			context.waitTicks(10);
			context.takeScreenshot("squirrel-tail-rear");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.level().getEntitiesOfClass(SquirrelEntity.class, player.getBoundingBox().inflate(5)).forEach(SquirrelEntity::discard);
			});
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
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.removeAllEffects();
				player.addEffect(new MobEffectInstance(ModEffects.BEER, 1960));
			});
			context.waitFor(client -> client.player != null && client.player.hasEffect(ModEffects.BEER), 80);
			context.runOnClient(client -> {
				if (client.player.getActiveEffects().size() != 1 || client.player.hasEffect(MobEffects.NAUSEA)
						|| client.player.getEffectBlendFactor(MobEffects.NAUSEA, 0) <= 0) {
					throw new AssertionError("One intoxication effect must drive actual vanilla nausea rendering");
				}
			});
			context.takeScreenshot("beer-intoxication-single-effect");
			server.runOnServer(instance -> instance.getPlayerList().getPlayers().getFirst().removeAllEffects());
			context.waitFor(client -> client.player != null && !client.player.hasEffect(ModEffects.BEER), 80);
			context.runOnClient(client -> {
				if (client.player.getEffectBlendFactor(MobEffects.NAUSEA, 0) != 0) {
					throw new AssertionError("Removing intoxication must also stop its nausea rendering");
				}
			});
		}
	}
}
