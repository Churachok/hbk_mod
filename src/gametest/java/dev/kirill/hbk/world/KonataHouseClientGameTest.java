package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.stream.StreamSupport;

/** Real template, inhabitants, block interactions and networked basement screenshots. */
public final class KonataHouseClientGameTest implements FabricClientGameTest {
	private static final BlockPos ORIGIN = new BlockPos(0, 150, 0);
	private static final BlockPos HATCH = ORIGIN.offset(2, 11, 2);
	private static final BlockPos CARPET = HATCH.above();
	private static final BlockPos PISTON = ORIGIN.offset(6, 2, 5);
	private static final BlockPos LEVER = ORIGIN.offset(7, 2, 5);

	@Override
	public void runTest(ClientGameTestContext context) {
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
				server.runCommand("tp @a 3.5 162 3.5 135 48");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					var level = player.level();
					var template = level.getStructureManager().getOrCreate(HbkMod.id("konata_house"));
					if (!template.getSize().equals(new net.minecraft.core.Vec3i(9, 18, 9))) {
						throw new AssertionError("Konata's real house template must contain the eleven-block basement offset");
					}
					var settings = new StructurePlaceSettings().setRotation(Rotation.NONE).setIgnoreEntities(true);
					if (!template.placeInWorld(level, ORIGIN, ORIGIN, settings, RandomSource.create(0), 3)) {
						throw new AssertionError("The full Konata house template must be placeable");
					}
					var carpet = level.getBlockState(CARPET);
					var hatch = level.getBlockState(HATCH);
					if (!carpet.is(Blocks.CARPET.pink()) || !hatch.is(Blocks.CHERRY_TRAPDOOR)
							|| hatch.getValue(BlockStateProperties.OPEN)) {
						throw new AssertionError("The placed server template must retain its concealed closed hatch: carpet="
								+ carpet + ", hatch=" + hatch);
					}
					ModStructurePopulator.populateKonataBasement(level, ORIGIN, Rotation.NONE,
							ModWorldData.get(level), "konata_basement_client_test");
					player.getInventory().clearContent();
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					player.getAbilities().flying = true;
					player.onUpdateAbilities();
					player.inventoryMenu.broadcastChanges();
				});
				// Chunks can take long enough to load that the first teleport falls to the flat world.
				server.runCommand("tp @a 3.5 162 3.5 135 48");
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				world.getConnection().waitForChunksRender();
				context.waitFor(KonataHouseClientGameTest::hasWhiteSheepInCherryBoat, 100);
				// Entity packets can arrive before the chunk's batched block updates.
				try {
					context.waitFor(KonataHouseClientGameTest::hasHiddenHatch, 100);
				} catch (RuntimeException | AssertionError failure) {
					String states = context.computeOnClient(client -> client.level == null ? "no client level"
							: "carpet=" + client.level.getBlockState(CARPET) + ", hatch=" + client.level.getBlockState(HATCH));
					throw new AssertionError("The server's concealed hatch must synchronize to the client: " + states, failure);
				}
				context.runOnClient(client -> {
					if (!hasHiddenHatch(client)) {
						throw new AssertionError("The closed cherry hatch must be concealed directly beneath pink carpet: carpet="
								+ client.level.getBlockState(CARPET) + ", hatch=" + client.level.getBlockState(HATCH));
					}
				});
				capture(context, "konata-house-hidden-hatch");

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					var level = player.level();
					level.removeBlock(CARPET, false);
					var hit = new BlockHitResult(Vec3.atCenterOf(HATCH), Direction.UP, HATCH, false);
					if (!player.gameMode.useItemOn(player, level, ItemStack.EMPTY,
							InteractionHand.MAIN_HAND, hit).consumesAction()) {
						throw new AssertionError("The uncovered trapdoor must open through the real block interaction");
					}
				});
				context.waitFor(client -> client.level != null && client.level.getBlockState(CARPET).isAir()
						&& client.level.getBlockState(HATCH).is(Blocks.CHERRY_TRAPDOOR)
						&& client.level.getBlockState(HATCH).getValue(BlockStateProperties.OPEN), 80);
				context.runOnClient(client -> {
					for (int y = 1; y <= 10; y++) {
						if (!client.level.getBlockState(ORIGIN.offset(2, y, 2)).is(Blocks.LADDER)) {
							throw new AssertionError("The concealed entrance needs an unbroken ladder to the basement");
						}
					}
				});
				server.runCommand("tp @a 2.5 162 2.5 0 87");
				capture(context, "konata-house-open-basement-shaft");

				server.runCommand("tp @a 2.5 151.5 2.5 -33 12");
				context.waitFor(client -> client.player != null && client.player.getY() < ORIGIN.getY() + 2, 80);
				context.runOnClient(client -> {
					if (!hasWhiteSheepInCherryBoat(client)) {
						throw new AssertionError("The adult white sheep must remain seated in the cherry boat on the client");
					}
					if (!client.level.getBlockState(PISTON).is(Blocks.STICKY_PISTON)
								|| !client.level.getBlockState(ORIGIN.offset(5, 2, 5)).is(Blocks.END_ROD)
							|| !client.level.getBlockState(LEVER).is(Blocks.LEVER)) {
						throw new AssertionError("The basement must render the piston, end rod and lever one block above their old positions");
					}
					for (int x = 3; x <= 4; x++) {
						var slab = client.level.getBlockState(ORIGIN.offset(x, 1, 5));
						if (!slab.is(Blocks.CHERRY_SLAB) || slab.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.BOTTOM) {
							throw new AssertionError("The boat's two bottom cherry platform slabs must synchronize to the client");
						}
					}
				});
				capture(context, "konata-house-basement-white-sheep");

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					var hit = new BlockHitResult(Vec3.atCenterOf(LEVER), Direction.EAST, LEVER, false);
					if (!player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY,
							InteractionHand.MAIN_HAND, hit).consumesAction()) {
						throw new AssertionError("The mechanism must be powered by interacting with its actual lever");
					}
				});
				context.waitFor(client -> client.level != null
						&& client.level.getBlockState(LEVER).is(Blocks.LEVER)
						&& client.level.getBlockState(LEVER).getValue(BlockStateProperties.POWERED)
						&& client.level.getBlockState(PISTON).is(Blocks.STICKY_PISTON)
						&& client.level.getBlockState(PISTON).getValue(BlockStateProperties.EXTENDED)
						&& client.level.getBlockState(ORIGIN.offset(5, 2, 5)).is(Blocks.PISTON_HEAD)
						&& client.level.getBlockState(ORIGIN.offset(4, 2, 5)).is(Blocks.END_ROD), 80);
				capture(context, "konata-house-basement-piston-extended");
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					var hit = new BlockHitResult(Vec3.atCenterOf(LEVER), Direction.EAST, LEVER, false);
					if (!player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY,
							InteractionHand.MAIN_HAND, hit).consumesAction()) {
						throw new AssertionError("The same lever must switch the sticky piston off again");
					}
				});
				context.waitFor(client -> client.level != null
						&& client.level.getBlockState(LEVER).is(Blocks.LEVER)
						&& !client.level.getBlockState(LEVER).getValue(BlockStateProperties.POWERED)
						&& client.level.getBlockState(PISTON).is(Blocks.STICKY_PISTON)
						&& !client.level.getBlockState(PISTON).getValue(BlockStateProperties.EXTENDED)
						&& client.level.getBlockState(ORIGIN.offset(5, 2, 5)).is(Blocks.END_ROD)
						&& client.level.getBlockState(ORIGIN.offset(4, 2, 5)).isAir(), 80);
				capture(context, "konata-house-basement-sticky-piston-retracted");
			} finally {
				context.runOnClient(client -> {
					client.setScreenAndShow(null);
					client.options.setCameraType(originalCamera);
				});
			}
		}
	}

	private static boolean hasWhiteSheepInCherryBoat(Minecraft client) {
		return client.level != null && StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
				.anyMatch(entity -> entity instanceof Sheep sheep && !sheep.isBaby()
						&& sheep.getColor() == DyeColor.WHITE && sheep.getVehicle() != null
						&& Math.abs(sheep.getVehicle().getY() - ORIGIN.getY() - 1.5) < 0.03
						&& sheep.getVehicle().getType() == EntityTypes.CHERRY_BOAT);
	}

	private static boolean hasHiddenHatch(Minecraft client) {
		return client.level != null && client.level.getBlockState(CARPET).is(Blocks.CARPET.pink())
				&& client.level.getBlockState(HATCH).is(Blocks.CHERRY_TRAPDOOR)
				&& !client.level.getBlockState(HATCH).getValue(BlockStateProperties.OPEN);
	}

	private static void capture(ClientGameTestContext context, String name) {
		context.waitTicks(40);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}
}
