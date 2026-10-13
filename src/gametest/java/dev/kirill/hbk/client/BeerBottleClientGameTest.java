package dev.kirill.hbk.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Checks loaded bottle models and both grips, with real GUI/hand screenshots. */
public final class BeerBottleClientGameTest implements FabricClientGameTest {
	private static final ItemDisplayContext[] CONTEXTS = {
			ItemDisplayContext.GUI,
			ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
			ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(BeerBottleClientGameTest::verifySourceModels);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var originalCamera = context.computeOnClient(client -> client.options.getCameraType());
			var originalMainHand = context.computeOnClient(client -> client.options.mainHand().get());
			try {
				var server = world.getServer();
				server.runCommand("tp @a 0.5 180 0.5 0 0");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				server.runCommand("fill -12 179 -12 12 179 12 minecraft:stone");
				server.runCommand("tp @a 0.5 180 0.5 0 0");
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.getInventory().clearContent();
					player.getInventory().setSelectedSlot(0);
					player.getInventory().setItem(0, new ItemStack(ModItems.BEER_BOTTLE));
					player.getInventory().setItem(1, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
					player.getInventory().setItem(2, new ItemStack(ModItems.BROKEN_BEER_BOTTLE));
					player.inventoryMenu.broadcastChanges();
				});
				context.waitFor(client -> client.player != null
						&& client.player.getInventory().getItem(0).is(ModItems.BEER_BOTTLE)
						&& client.player.getInventory().getItem(1).is(ModItems.EMPTY_BEER_BOTTLE)
						&& client.player.getInventory().getItem(2).is(ModItems.BROKEN_BEER_BOTTLE), 80);
				context.runOnClient(client -> {
					client.options.mainHand().set(HumanoidArm.RIGHT);
					verifyLoadedModels(client);
					client.options.setCameraType(CameraType.FIRST_PERSON);
				});
				// The hotbar renders all three GUI models without opening a creative
				// catalogue tab that redirects away from the prepared inventory.
				context.waitTicks(40);
				context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
				context.takeScreenshot("beer-bottles-gui");

				Item[] items = {ModItems.BEER_BOTTLE, ModItems.EMPTY_BEER_BOTTLE, ModItems.BROKEN_BEER_BOTTLE};
				String[] names = {"full", "empty", "broken"};
				for (int i = 0; i < items.length; i++) {
					Item item = items[i];
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst();
						player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
						player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(item));
						player.inventoryMenu.broadcastChanges();
					});
					context.waitFor(client -> client.player != null
							&& client.player.getMainHandItem().is(item) && client.player.getOffhandItem().is(item), 80);
					context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
					// Weapon attack-speed changes also lower the main hand until its
					// attack/swap cooldown has recovered. Capture the settled grip.
					context.waitTicks(40);
					context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
					context.takeScreenshot("beer-bottle-" + names[i] + "-first-person-both-hands");
					context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
					context.waitTicks(5);
					context.takeScreenshot("beer-bottle-" + names[i] + "-third-person-both-hands");
				}
			} finally {
				context.runOnClient(client -> {
					client.setScreenAndShow(null);
					client.options.setCameraType(originalCamera);
					client.options.mainHand().set(originalMainHand);
				});
			}
		}
	}

	private static void verifyLoadedModels(Minecraft client) {
		for (var item : new Item[]{ModItems.BEER_BOTTLE, ModItems.EMPTY_BEER_BOTTLE, ModItems.BROKEN_BEER_BOTTLE}) {
			for (var display : CONTEXTS) {
				var state = new ItemStackRenderState();
				client.getItemModelResolver().updateForLiving(state, new ItemStack(item), display, client.player);
				if (state.isEmpty()) throw new AssertionError("Bottle model must render in " + display + ": " + item);
				var particle = state.pickParticleMaterial(RandomSource.create(0));
				if (particle == null || particle.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())
						|| !particle.sprite().contents().name().equals(HbkMod.id("item/beer_bottle"))) {
					throw new AssertionError("Bottle must resolve its real green atlas, not a missing model: " + item);
				}
				var box = state.getModelBoundingBox();
				if (box.getYsize() < 0.3 || box.getXsize() < 0.08 || box.getZsize() < 0.08) {
					throw new AssertionError("Each bottle must retain a visible three-dimensional silhouette in " + display);
				}
			}
		}
	}

	private static void verifySourceModels(Minecraft client) {
		var full = readJson(client, "models/item/beer_bottle.json");
		if (full.getAsJsonArray("elements").size() != 6) {
			throw new AssertionError("The intact bottle and squirrel gift must retain six simple cuboids");
		}
		var empty = readJson(client, "models/item/empty_beer_bottle.json");
		if (!empty.get("parent").getAsString().equals("hbk:item/beer_bottle")) {
			throw new AssertionError("Empty bottles must keep the original green glass/label silhouette");
		}
		var broken = readJson(client, "models/item/broken_beer_bottle.json");
		var elements = broken.getAsJsonArray("elements");
		if (elements.size() < 12) throw new AssertionError("The broken bottle needs thin walls and a jagged rim");
		double lowest = 16;
		for (var value : elements) {
			var element = value.getAsJsonObject();
			var from = element.getAsJsonArray("from");
			var to = element.getAsJsonArray("to");
			lowest = Math.min(lowest, from.get(1).getAsDouble());
			for (int axis = 0; axis < 3; axis++) {
				if (from.get(axis).getAsDouble() < 0 || to.get(axis).getAsDouble() > 16
						|| from.get(axis).getAsDouble() >= to.get(axis).getAsDouble()) {
					throw new AssertionError("Bottle cuboids must remain non-degenerate and inside their 16-unit cell");
				}
			}
			double cavityBottom = Math.max(4.5, from.get(1).getAsDouble());
			double cavityTop = Math.min(9, to.get(1).getAsDouble());
			if (cavityBottom < cavityTop && contains(from, to, 8, (cavityBottom + cavityTop) / 2, 8)) {
				throw new AssertionError("A floor/body cube must not close the shattered cavity");
			}
		}
		if (Math.abs(lowest - 4.5) > 0.0001) throw new AssertionError("The base/lower bottle half must be missing");
		for (var name : new String[]{"firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand"}) {
			var transform = broken.getAsJsonObject("display").getAsJsonObject(name);
			if (transform.getAsJsonArray("rotation").get(2).getAsDouble() != 180
					|| Math.abs(transform.getAsJsonArray("translation").get(1).getAsDouble()
							- (12.5 - 8) * transform.getAsJsonArray("scale").get(1).getAsDouble()) > 0.0001) {
				throw new AssertionError("The inverted fragment must be held at its neck in " + name);
			}
		}
		for (var name : new String[]{"beer_bottle", "empty_beer_bottle", "broken_beer_bottle"}) {
			var item = readJson(client, "items/" + name + ".json");
			if (!item.getAsJsonObject("model").get("model").getAsString().equals("hbk:item/" + name)) {
				throw new AssertionError("Each bottle state needs its own client item definition: " + name);
			}
		}
	}

	private static boolean contains(JsonArray from, JsonArray to, double x, double y, double z) {
		double[] point = {x, y, z};
		for (int i = 0; i < point.length; i++) {
			if (point[i] <= from.get(i).getAsDouble() || point[i] >= to.get(i).getAsDouble()) return false;
		}
		return true;
	}

	private static JsonObject readJson(Minecraft client, String path) {
		try (var stream = client.getResourceManager().getResourceOrThrow(HbkMod.id(path)).open();
				var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException error) {
			throw new UncheckedIOException(error);
		}
	}
}
