package dev.kirill.hbk.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.block.BeerCauldronBlock;
import dev.kirill.hbk.effect.BeerEffect;
import dev.kirill.hbk.item.BeerBottleItem;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;

/** Loaded cauldron models, real one-wheat interactions, synchronized tint stages and the beer HUD icon. */
public final class BeerCauldronClientGameTest implements FabricClientGameTest {
	private static final BlockPos CAULDRON = new BlockPos(0, 180, 2);

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(BeerCauldronClientGameTest::verifyModelsAndIcon);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var originalCamera = context.computeOnClient(client -> client.options.getCameraType());
			try {
				var server = world.getServer();
				server.runCommand("fill -8 179 -8 8 179 8 minecraft:stone");
				server.runCommand("tp @a 0.5 180.25 0.5 0 27");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setGameMode(GameType.SURVIVAL);
					// Real survival consumption, but no random ground-only bus/squirrel encounters in screenshots.
					player.setNoGravity(true);
					player.setOnGround(false);
					player.getInventory().clearContent();
					player.level().setBlockAndUpdate(CAULDRON,
							Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
					setHand(player, new ItemStack(Items.WHEAT, 3));
				});
				server.runCommand("tp @a 0.5 180.25 0.5 0 27");
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				context.waitFor(client -> client.level != null && client.player != null
						&& client.level.getBlockState(CAULDRON).is(Blocks.WATER_CAULDRON)
						&& client.player.getMainHandItem().is(Items.WHEAT)
						&& client.player.getMainHandItem().getCount() == 3, 100);
				context.runOnClient(client -> {
					var state = client.level.getBlockState(CAULDRON);
					int water = client.getBlockColors().getTintSource(state, 0).colorInWorld(state, client.level, CAULDRON);
					if ((water & 255) <= (water >> 16 & 255) || (water & 255) <= (water >> 8 & 255)) {
						throw new AssertionError("Fresh plains-biome water must retain its vanilla blue color");
					}
				});
				capture(context, "beer-cauldron-fresh-blue-water");

				for (int wheat = 1; wheat <= 3; wheat++) {
					int expectedWheat = wheat;
					server.runOnServer(instance -> interact(instance.getPlayerList().getPlayers().getFirst()));
					context.waitFor(client -> client.level != null && client.player != null
							&& isBrew(client.level.getBlockState(CAULDRON), 3, expectedWheat)
							&& client.player.getMainHandItem().getCount() == 3 - expectedWheat, 100);
					context.runOnClient(client -> {
						var state = client.level.getBlockState(CAULDRON);
						int actual = client.getBlockColors().getTintSource(state, 0).colorInWorld(state, client.level, CAULDRON);
						int expected = expectedWheat == 1 ? BeerCauldronColors.FIRST_WHEAT_COLOR
								: expectedWheat == 2 ? BeerCauldronColors.SECOND_WHEAT_COLOR : BeerCauldronColors.READY_COLOR;
						if (actual != expected) throw new AssertionError("Every synchronized wheat addition must recolor the water");
					});
					capture(context, "beer-cauldron-wheat-" + wheat + (wheat == 3 ? "-ready-amber" : "-pale-yellow"));
				}

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					setHand(player, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
					interact(player);
				});
				waitForFilledBottle(context, 3);
				capture(context, "beer-cauldron-full-bottle-empty-pot");

				for (int level = 1; level <= 2; level++) {
					int amount = level;
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst();
						player.level().setBlockAndUpdate(CAULDRON,
								Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, amount));
						setHand(player, new ItemStack(Items.WHEAT, amount));
						for (int wheat = 0; wheat < amount; wheat++) interact(player);
					});
					context.waitFor(client -> client.level != null && isBrew(client.level.getBlockState(CAULDRON), amount, amount), 100);
					context.runOnClient(client -> {
						var state = client.level.getBlockState(CAULDRON);
						if (client.getBlockColors().getTintSource(state, 0).colorInWorld(state, client.level, CAULDRON)
								!= BeerCauldronColors.READY_COLOR) {
							throw new AssertionError("One wheat per water level must produce the same ready amber color");
						}
					});
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst();
						setHand(player, new ItemStack(ModItems.EMPTY_BEER_BOTTLE));
						interact(player);
					});
					waitForFilledBottle(context, amount);
					capture(context, "beer-cauldron-partial-bottle-" + amount + "-sips");
				}

				server.runOnServer(instance -> instance.getPlayerList().getPlayers().getFirst()
						.addEffect(new MobEffectInstance(ModEffects.BEER, BeerEffect.DURATION_TICKS)));
				context.waitFor(client -> client.player != null && client.player.hasEffect(ModEffects.BEER), 80);
				context.runOnClient(client -> {
					if (client.player.getActiveEffects().size() != 1) {
						throw new AssertionError("The beer mug HUD icon must represent the single intoxication effect");
					}
				});
				capture(context, "beer-intoxication-mug-icon");
			} finally {
				context.runOnClient(client -> {
					client.setScreenAndShow(null);
					client.options.setCameraType(originalCamera);
				});
			}
		}
	}

	private static boolean isBrew(BlockState state, int level, int wheat) {
		return state.is(ModBlocks.BEER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == level
				&& state.getValue(BeerCauldronBlock.WHEAT) == wheat;
	}

	private static void setHand(ServerPlayer player, ItemStack stack) {
		player.setItemSlot(EquipmentSlot.MAINHAND, stack);
		player.inventoryMenu.broadcastChanges();
	}

	private static void interact(ServerPlayer player) {
		var hit = new BlockHitResult(Vec3.atCenterOf(CAULDRON), Direction.NORTH, CAULDRON, false);
		if (!player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(),
				InteractionHand.MAIN_HAND, hit).consumesAction()) {
			throw new AssertionError("Wheat and empty bottles must interact with the actual cauldron block");
		}
		player.inventoryMenu.broadcastChanges();
	}

	private static void waitForFilledBottle(ClientGameTestContext context, int sips) {
		context.waitFor(client -> client.level != null && client.player != null
				&& client.level.getBlockState(CAULDRON).is(Blocks.CAULDRON)
				&& client.player.getMainHandItem().is(ModItems.BEER_BOTTLE)
				&& BeerBottleItem.getSips(client.player.getMainHandItem()) == sips, 100);
	}

	private static void capture(ClientGameTestContext context, String name) {
		context.waitTicks(40);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}

	private static void verifyModelsAndIcon(Minecraft client) {
		var models = client.getModelManager().getBlockStateModelSet();
		for (var state : ModBlocks.BEER_CAULDRON.getStateDefinition().getPossibleStates()) {
			var model = models.get(state);
			if (model == models.missingModel()) throw new AssertionError("Every beer cauldron state needs a loaded model: " + state);
			var parts = new ArrayList<BlockStateModelPart>();
			model.collectParts(RandomSource.create(0), parts);
			boolean surface = false;
			float expectedY = (6 + state.getValue(LayeredCauldronBlock.LEVEL) * 3) / 16.0f;
			for (var part : parts) {
				for (var quad : part.getQuads(null)) {
					if (quad.materialInfo().tintIndex() != 0) continue;
					surface = true;
					if (!quad.materialInfo().sprite().contents().name().equals(Identifier.withDefaultNamespace("block/water_still"))
							|| quad.direction() != Direction.UP || Math.abs(quad.position0().y() - expectedY) > 0.0001) {
						throw new AssertionError("The original animated water sprite must stay at the proper cauldron fill level");
					}
				}
			}
			if (!surface || client.getBlockColors().getTintSource(state, 0).color(state) != BeerCauldronColors.surfaceColor(state)) {
				throw new AssertionError("Beer must tint the actual surface quad without repainting the iron body");
			}
		}
		var first = ModBlocks.BEER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
				.setValue(BeerCauldronBlock.WHEAT, 1);
		var second = first.setValue(BeerCauldronBlock.WHEAT, 2);
		if (!client.getModelManager().requiresRender(first, second)) {
			throw new AssertionError("Changing wheat, even with unchanged fill geometry, must invalidate the cached render color");
		}
		var sprite = client.getAtlasManager().get(new SpriteId(Sheets.GUI_SHEET, Hud.getMobEffectSprite(ModEffects.BEER)));
		if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())
				|| !sprite.contents().name().equals(HbkMod.id("mob_effect/beer"))
				|| sprite.contents().width() != 18 || sprite.contents().height() != 18) {
			throw new AssertionError("Intoxication must load its own eighteen-pixel beer mug icon into the GUI atlas");
		}
		try (var stream = client.getResourceManager().getResourceOrThrow(HbkMod.id("textures/mob_effect/beer.png")).open();
				var image = NativeImage.read(stream)) {
			int amberPixels = 0;
			int foamPixels = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int pixel = image.getPixel(x, y);
					if ((pixel >>> 24) == 0) continue;
					int red = pixel >> 16 & 255, green = pixel >> 8 & 255, blue = pixel & 255;
					if (red > 150 && green > 80 && red > green && blue < red / 2) amberPixels++;
					if (red > 210 && green > 200 && blue > 165) foamPixels++;
				}
			}
			if (amberPixels < 10 || foamPixels < 5) {
				throw new AssertionError("The effect resource must contain amber beer and bright foam, not reuse nausea: amber="
						+ amberPixels + ", foam=" + foamPixels);
			}
		} catch (IOException error) {
			throw new UncheckedIOException(error);
		}
	}
}
