package dev.kirill.hbk;

import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real client: native models, equipment, inventory menus and survival area mining. */
public final class UniverseAlloyClientGameTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false); ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("fill -18 79 -6 18 79 22 minecraft:stone"); server.runCommand("time set noon");
			server.runOnServer(instance -> {
				var level = instance.overworld(); int i = 0;
				for (var a : UniverseAlloys.CATALOG) for (var r : a.rewards()) if (r.role().equals("device")) {
					level.setBlockAndUpdate(new BlockPos(i % 7 * 3 - 9, 80, i / 7 * 4 + 8), UniverseAlloys.device(r.id()).defaultBlockState()); i++;
				}
				String[][] sets = {{"mining_helmet", "gliding_chestplate", "sail_boots", "drill_pickaxe"}, {"hermetic_helmet", "gravity_chestplate", "anchor_boots", "smelting_pickaxe"}, {"mining_helmet", "gliding_chestplate", "diving_boots", "rift_sword"}};
				for (int n = 0; n < sets.length; n++) {
					var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, n * 4 - 4, 80, 4);
					stand.setShowArms(true); stand.setNoGravity(true); stand.setYRot(180);
					stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(UniverseAlloys.item(sets[n][0]))); stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(UniverseAlloys.item(sets[n][1])));
					stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(UniverseAlloys.item(sets[n][2]))); stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(UniverseAlloys.item(sets[n][3]))); level.addFreshEntity(stand);
				}
				var cart = UniverseAlloys.CARGO_MINECART.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND); cart.setPos(8, 80, 4); level.addFreshEntity(cart);
				var player = instance.getPlayerList().getPlayers().getFirst(); player.teleportTo(level, 0, 84, -6, java.util.Set.of(), 0, 18, true); player.getAbilities().flying = true; player.onUpdateAbilities();
			});
			world.getConnection().waitForChunksRender(); context.waitTicks(10); context.takeScreenshot("hbk-alloys-vanilla-equipment-and-devices");
			context.runOnClient(client -> {
				var resolver = client.getItemModelResolver();
				for (var a : UniverseAlloys.CATALOG) {
					var ids = new java.util.ArrayList<String>(); ids.add(a.id() + "_sample"); ids.add(a.id() + "_ingot"); ids.add(a.id() + "_block"); a.rewards().forEach(r -> ids.add(r.id()));
					for (String id : ids) {
						var state = new net.minecraft.client.renderer.item.ItemStackRenderState(); resolver.updateForLiving(state, new ItemStack(UniverseAlloys.item(id)), ItemDisplayContext.GUI, client.player);
						if (state.isEmpty()) throw new AssertionError("Empty alloy model: " + id);
						var material = state.pickParticleMaterial(client.level.getRandom());
						if (material != null && material.sprite().contents().name().getPath().equals("missingno")) throw new AssertionError("Missing alloy texture: " + id);
					}
				}
				for (String language : java.util.List.of("en_us", "ru_ru")) {
					try (var stream = getClass().getClassLoader().getResourceAsStream("assets/hbk/lang/" + language + ".json")) {
						var translations = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
						if (client.font.split(Component.literal(translations.get("book.hbk.recipes.info.universe_alloys").getAsString()), 114).size() > 14) throw new AssertionError("Alloy book page overflows: " + language);
					} catch (java.io.IOException e) { throw new AssertionError(e); }
				}
			});
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst(); var container = new SimpleContainer(54); int i = 0;
				for (var a : UniverseAlloys.CATALOG) container.setItem(i++, new ItemStack(UniverseAlloys.item(a.id() + "_ingot")));
				for (int n = 0; n < 9; n++) container.setItem(45 + n, new ItemStack(UniverseAlloys.item(UniverseAlloys.CATALOG.get(n).id() + "_sample")));
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> net.minecraft.world.inventory.ChestMenu.sixRows(id, inv, container), Component.literal("45 сплавов / 45 alloys")));
			});
			context.waitForScreen(ContainerScreen.class); context.waitTicks(5); context.takeScreenshot("hbk-alloys-forty-five-ingots");
			server.runOnServer(instance -> instance.getPlayerList().getPlayers().getFirst().closeContainer()); context.waitTicks(2);
			context.runOnClient(client -> {
				var screen = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(client.player, client.level.enabledFeatures(), true);
				client.setScreenAndShow(screen);
				var tabs = (net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen) screen;
				var worlds = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(UniverseCreativeTab.KEY);
				if (!tabs.setSelectedTab(worlds) || tabs.getSelectedTab() != worlds || screen.getMenu().items.size() != UniverseCreativeTab.items().size())
					throw new AssertionError("Worlds tab must be reachable through the native creative inventory, including Fabric pagination");
			});
			context.waitTicks(4); context.takeScreenshot("hbk-worlds-creative-catalogue");
			context.runOnClient(client -> {
				var screen = (net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) client.gui.screen();
				screen.getMenu().scrollTo(1);
			});
			context.waitTicks(3); context.takeScreenshot("hbk-worlds-creative-alloys");
			context.runOnClient(client -> client.setScreenAndShow(null));
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.getAbilities().flying = false; player.onUpdateAbilities();
				player.teleportTo(level, .5, 80, -.5, java.util.Set.of(), 0, 0, true);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UniverseAlloys.item("drill_pickaxe")));
				BlockPos centre = new BlockPos(0, 81, 2);
				for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) level.setBlockAndUpdate(centre.offset(x, y, 0), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(centre.east(), UniverseMaterials.state(0, 4, 0)); level.setBlockAndUpdate(centre.west(), Blocks.BARREL.defaultBlockState());
				if (!player.gameMode.destroyBlock(centre)) throw new AssertionError("Could not mine centre rock");
				if (!level.getBlockState(centre.east()).is(UniverseMaterials.block(0, 4)) || !level.getBlockState(centre.west()).is(Blocks.BARREL)) throw new AssertionError("Drill damaged ore/container");
				if (!level.getBlockState(centre.above()).isAir() || player.getMainHandItem().getDamageValue() != 7) throw new AssertionError("Drill must mine seven eligible blocks and charge durability for each");
			});
			context.waitTicks(5); context.takeScreenshot("hbk-alloys-survival-drill");
		}
	}
}
