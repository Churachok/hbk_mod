package dev.kirill.hbk;

import dev.kirill.hbk.client.TimeFurnaceScreen;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.universe.UniverseCode;
import dev.kirill.hbk.universe.UniverseManager;
import dev.kirill.hbk.universe.UniverseMaterials;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class UniverseClientGameTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		int distance = context.computeOnClient(client -> client.options.renderDistance().get());
		try {
			context.runOnClient(client -> { client.options.renderDistance().set(6); client.options.broadcastOptions(); });
			runScenario(context);
		} finally {
			context.runOnClient(client -> { client.options.renderDistance().set(distance); client.options.broadcastOptions(); });
		}
	}
	private void runScenario(ClientGameTestContext context) {
		context.runOnClient(client -> {
			for (String language : java.util.List.of("en_us", "ru_ru")) {
				try (var stream = UniverseClientGameTest.class.getClassLoader().getResourceAsStream("assets/hbk/lang/" + language + ".json")) {
					var translations = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
					for (String species : dev.kirill.hbk.universe.UniverseBotany.SPECIES) for (String section : java.util.List.of("flora", "crafts")) {
						String key = "book.hbk.recipes.info." + species + "_" + section;
						int lines = client.font.split(net.minecraft.network.chat.Component.literal(translations.get(key).getAsString()), 114).size();
						if (lines > 14) throw new AssertionError("Botanical guide page overflows: " + language + " " + key + " lines=" + lines);
					}
					for (String page : java.util.List.of("universe_travel", "universe_equipment", "universe_mining")) {
						int lines = client.font.split(net.minecraft.network.chat.Component.literal(translations.get("book.hbk.recipes.info." + page).getAsString()), 114).size();
						if (lines > 14) throw new AssertionError("Universe guide page overflows: " + language + " " + page + " lines=" + lines);
					}
				} catch (java.io.IOException exception) { throw new AssertionError(exception); }
			}
		});
		net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
		BlockPos marker = new BlockPos(3, 280, 3);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false); ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runOnServer(instance -> {
				var level = instance.overworld();
				for (int f = 0; f < 10; f++) {
					int x0 = f % 5 * 5 - 10, z0 = f / 5 * 6 + 8;
					for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) level.setBlockAndUpdate(new BlockPos(x0 + x, 79, z0 + z), dev.kirill.hbk.universe.UniverseBotany.block(f, "planks").defaultBlockState());
					for (int h = 0; h < 3; h++) {
						level.setBlockAndUpdate(new BlockPos(x0, 80 + h, z0 + 2), dev.kirill.hbk.universe.UniverseBotany.block(f, "log").defaultBlockState());
						level.setBlockAndUpdate(new BlockPos(x0 + 1, 80 + h, z0 + 2), dev.kirill.hbk.universe.UniverseBotany.block(f, "stripped_log").defaultBlockState());
					}
					for (int i = 0; i < 4; i++) level.setBlockAndUpdate(new BlockPos(x0 + i, 80, z0), dev.kirill.hbk.universe.UniverseBotany.block(f, java.util.List.of("stairs", "slab", "fence", "lamp").get(i)).defaultBlockState());
					level.setBlockAndUpdate(new BlockPos(x0 + 3, 79, z0 + 3), dev.kirill.hbk.universe.UniverseBotany.block(f, "soil").defaultBlockState());
					level.setBlockAndUpdate(new BlockPos(x0 + 3, 80, z0 + 3), dev.kirill.hbk.universe.UniverseBotany.block(f, "plant").defaultBlockState().setValue(dev.kirill.hbk.universe.UniverseCropBlock.AGE, 3));
				}
				var player = instance.getPlayerList().getPlayers().getFirst(); player.getAbilities().flying = true; player.onUpdateAbilities();
				player.teleportTo(level, 1, 87, -1, java.util.Set.of(), 0, 24, true);
			});
			world.getConnection().waitForChunksRender(); context.waitTicks(5);
			context.takeScreenshot("hbk-botany-building-palette");
			server.runOnServer(instance -> {
				var level = instance.overworld();
				for (int f = 0; f < 10; f++) for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) {
					int wx = f * 4 - 20 + x, wz = z + 28;
					for (int y = 79; y <= 83; y++) level.setBlockAndUpdate(new BlockPos(wx, y, wz), y == 83
							? dev.kirill.hbk.universe.UniverseBotany.block(f, "soil").defaultBlockState() : y == 82 ? UniverseMaterials.loam(f).defaultBlockState()
							: UniverseMaterials.state(f, y == 81 ? 1 : 0, x % 4));
					if (x == 2 && z == 1) level.setBlockAndUpdate(new BlockPos(wx, 84, wz), dev.kirill.hbk.universe.UniverseBotany.block(f, "plant").defaultBlockState().setValue(dev.kirill.hbk.universe.UniverseCropBlock.AGE, 3));
				}
				var player = instance.getPlayerList().getPlayers().getFirst(); player.teleportTo(level, 0, 88, -4, java.util.Set.of(), 0, 13, true);
			});
			world.getConnection().waitForChunksRender(); context.waitTicks(5);
			context.takeScreenshot("hbk-terrain-family-transitions");
			server.runOnServer(instance -> {
				var level = instance.overworld();
				for (int f = 0; f < 10; f++) {
					double x = f * 3 - 13.5;
					for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(BlockPos.containing(x + dx, 79, 50 + dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
					var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, x, 80, 50.5);
					stand.setNoGravity(true); stand.setShowArms(true); stand.setYRot(180);
					for (var slot : java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET)) {
						String role = switch (slot) { case HEAD -> "helmet"; case CHEST -> "chestplate"; case LEGS -> "leggings"; default -> "boots"; };
						stand.setItemSlot(slot, new ItemStack(dev.kirill.hbk.universe.UniverseEquipment.item(f, role)));
					}
					stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(dev.kirill.hbk.universe.UniverseEquipment.item(f, "pickaxe")));
					level.addFreshEntity(stand);
				}
				var player = instance.getPlayerList().getPlayers().getFirst(); player.teleportTo(level, 0, 83, 35, java.util.Set.of(), 0, 12, true);
			});
			world.getConnection().waitForChunksRender(); context.waitTicks(10);
			context.takeScreenshot("hbk-universe-equipment-ten-sets");
			server.runCommand("fill -6 79 -6 6 79 6 minecraft:stone");
			server.runCommand("tp @a 2.8 80 -3.5 28 12");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst(); var pos = new BlockPos(0, 80, 0);
				player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 11));
				player.level().setBlockAndUpdate(pos, ModBlocks.TIME_FURNACE.defaultBlockState());
				ModBlocks.TIME_FURNACE.setPlacedBy(player.level(), pos, player.level().getBlockState(pos), player, new ItemStack(ModItems.TIME_FURNACE));
				player.level().getBlockState(pos).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
			});
			context.waitForScreen(TimeFurnaceScreen.class);
			context.runOnClient(client -> ((EditBox) client.gui.screen().children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow()).setValue("13857"));
			context.takeScreenshot("hbk-universe-furnace-address");
			context.clickScreenButton("Travel");
			server.waitFor(instance -> instance.getPlayerList().getPlayers().getFirst().level().dimension().equals(UniverseManager.key(new UniverseCode("13857"))), 200);
			world.getConnection().waitForChunksRender(); context.waitTicks(20);
			context.runOnClient(client -> {
				if (!client.level.dimension().equals(UniverseManager.key(new UniverseCode("13857")))) throw new AssertionError("Client must receive a dynamic dimension key");
				client.gui.hud.getChat().clearMessages(true);
			});
			context.takeScreenshot("hbk-universe-13857");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				if (player.getInventory().getItem(0).getCount() != 11) throw new AssertionError("Inventory lost during travel");
				var pos = player.blockPosition().offset(2, 0, 2);
				player.level().setBlockAndUpdate(pos, UniverseMaterials.state(1, 3, 2));
			});
			context.waitTicks(5);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var station = dev.kirill.hbk.universe.UniverseArrival.findReturnFurnace(player.level(), player.blockPosition());
				if (station == null) throw new AssertionError("Travel did not create the return furnace");
				player.level().getBlockState(station).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(station), Direction.NORTH, station, false));
			});
			context.waitForScreen(TimeFurnaceScreen.class);
			context.takeScreenshot("hbk-universe-return-furnace");
			context.clickScreenButton("Return home");
			server.waitFor(instance -> instance.getPlayerList().getPlayers().getFirst().level().dimension().equals(Level.OVERWORLD), 100);
			server.runCommand("execute as @a run universe tp 10011");
			context.waitFor(client -> client.level != null && client.level.dimension().equals(UniverseManager.key(new UniverseCode("10011"))), 200);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true; player.onUpdateAbilities();
			});
			server.runCommand("execute as @a at @s run tp @s ~ ~20 ~ 20 38");
			world.getConnection().waitForChunksRender(); context.waitTicks(30);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
			context.takeScreenshot("hbk-universe-islands-10011");
			// Inspect the actual rendered ecology of every geometry, not only density-only chunks.
			for (int family = 0; family < 10; family++) {
				String code = family + "0011";
				server.runCommand("execute as @a run universe tp " + code);
				context.waitFor(client -> client.level != null && client.level.dimension().equals(UniverseManager.key(new UniverseCode(code))), 200);
				final int geometry = family;
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level();
					BlockPos target = null;
					for (int cx = -2; cx <= 2 && target == null; cx++) for (int cz = -2; cz <= 2 && target == null; cz++) {
						var chunk = level.getChunk(cx, cz);
						search: for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 280; y > -40; y--) {
							var pos = new BlockPos(cx * 16 + x, y, cz * 16 + z);
							if (chunk.getBlockState(pos).is(dev.kirill.hbk.universe.UniverseEcology.HABITATS.get(geometry).log())) { target = pos; break search; }
						}
					}
					if (target == null) throw new AssertionError("No natural timber near the origin in " + code);
					double x = target.getX() + 14.5, z = target.getZ() - 21.5, y = Math.min(310, target.getY() + (geometry == 8 ? -12 : 12));
					while (y > -40 && y < 310 && !level.getBlockState(BlockPos.containing(x, y, z)).isAir()) y += geometry == 8 ? -1 : 1;
					float yaw = (float) Math.toDegrees(Math.atan2(-(target.getX() - x), target.getZ() - z));
					float pitch = (float) -Math.toDegrees(Math.atan2(target.getY() - y, Math.hypot(target.getX() - x, target.getZ() - z)));
					player.getAbilities().flying = true; player.onUpdateAbilities();
					player.teleportTo(level, x, y, z, java.util.Set.of(), yaw, pitch, true);
					for (var other : instance.getAllLevels()) if (other != level && dev.kirill.hbk.universe.UniverseProfile.of(other) != null) UniverseManager.unload(instance, other);
				});
				world.getConnection().waitForChunksRender(); context.waitTicks(25);
				context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
				context.takeScreenshot("hbk-universe-ecology-" + code);
			}
			context.runOnClient(client -> {
				var guide = new net.minecraft.client.gui.screens.inventory.BookViewScreen(net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess.fromItem(dev.kirill.hbk.world.GraveyardRecipeBook.create()));
				client.setScreenAndShow(guide);
				guide.setPage(1 + dev.kirill.hbk.world.GraveyardRecipeBook.DIAGRAM_RECIPES.size() + dev.kirill.hbk.world.GraveyardRecipeBook.INFO_PAGES.indexOf("sailwood_crafts"));
			});
			context.takeScreenshot("hbk-botany-crafting-guide");
			context.runOnClient(client -> client.setScreenAndShow(null));
			// Real positional fog and particles, sampled in an existing open part of a gas pocket.
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var p = dev.kirill.hbk.universe.UniverseProfile.of(player.level());
				var zone = dev.kirill.hbk.universe.UniverseHazards.nearest(p, player.position(), dev.kirill.hbk.universe.UniverseHazards.gasKind(p));
				BlockPos target = null;
				for (int dy = -8; dy <= 8 && target == null; dy++) for (int dx = -14; dx <= 14 && target == null; dx++) for (int dz = -14; dz <= 14 && target == null; dz++) {
					var pos = BlockPos.containing(zone.center()).offset(dx, dy, dz);
					if (zone.strength(Vec3.atCenterOf(pos)) > .5 && player.level().getBlockState(pos).isAir() && player.level().getBlockState(pos.above()).isAir()) target = pos;
				}
				if (target == null) throw new AssertionError("Gas pocket has no accessible air");
				player.teleportTo(player.level(), target.getX() + .5, target.getY(), target.getZ() + .5, java.util.Set.of(), 20, 8, true);
			});
			world.getConnection().waitForChunksRender(); context.waitTicks(40);
			context.runOnClient(client -> {
				var p = dev.kirill.hbk.universe.UniverseProfile.of(client.level);
				var zone = dev.kirill.hbk.universe.UniverseHazards.at(p, client.player.position());
				if (zone == null || !zone.kind().gas) throw new AssertionError("Client/server gas fields disagree");
				if (dev.kirill.hbk.universe.UniverseEnvironment.hazardFogEnd(p, client.player.position(), 256) >= 64) throw new AssertionError("Gas must create local fog");
			});
			context.takeScreenshot("hbk-universe-local-gas");
			server.runCommand("execute as @a run universe tp 80046");
			context.waitFor(client -> client.level != null && client.level.dimension().equals(UniverseManager.key(new UniverseCode("80046"))), 200);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var pos = player.blockPosition();
				int roof = pos.getY() + 8;
				while (roof < 300 && !player.level().getBlockState(new BlockPos(pos.getX(), roof, pos.getZ())).isSolidRender()) roof++;
				player.teleportTo(player.level(), player.getX(), roof - 12, player.getZ(), java.util.Set.of(), 0, -75, true);
			});
			world.getConnection().waitForChunksRender(); context.waitTicks(20);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
			context.takeScreenshot("hbk-universe-inverted-80046");
			server.runOnServer(instance -> {
				var level = instance.getPlayerList().getPlayers().getFirst().level();
				level.setBlockAndUpdate(marker, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
				((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(marker)).setItem(0, new ItemStack(Items.EMERALD, 7));
			});
			save = world.getWorldSave();
		}
		// A real server stop/start while the player is inside a lazily created dimension.
		try (var reopened = save.open()) {
			var server = reopened.getServer();
			server.waitFor(instance -> instance.getPlayerList().getPlayers().getFirst().level().dimension().equals(UniverseManager.key(new UniverseCode("80046"))), 200);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) player.level().getBlockEntity(marker);
				if (chest == null || chest.getItem(0).getCount() != 7) throw new AssertionError("Universe container lost after server restart");
				if (player.getInventory().getItem(0).getCount() != 11) throw new AssertionError("Inventory lost after server restart");
			});
			server.runCommand("execute as @a run universe return");
			server.waitFor(instance -> instance.getPlayerList().getPlayers().getFirst().level().dimension().equals(Level.OVERWORLD), 100);
		}
	}
}
