package dev.kirill.hbk;

import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Actual dynamic dimensions, promoted loot containers and survival ore mining. */
public final class UniverseMinesClientGameTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		int distance = context.computeOnClient(client -> client.options.renderDistance().get());
		try {
			context.runOnClient(client -> { client.options.renderDistance().set(5); client.options.broadcastOptions(); });
			try (var world = context.worldBuilder().adjustSettings(ui -> {
				ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); ui.setAllowCommands(true); ui.setGenerateStructures(false);
			}).create()) {
				var server = world.getServer();
				for (String code : java.util.List.of("00000", "80000")) {
					server.runCommand("execute as @a run universe tp " + code);
					context.waitFor(client -> client.level != null && client.level.dimension().equals(UniverseManager.key(new UniverseCode(code))), 200);
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level();
						var generator = (UniverseChunkGenerator) level.getChunkSource().getGenerator();
						UniverseMines.Site site = null;
						for (int x = -1; x <= 1 && site == null; x++) for (int z = -1; z <= 1; z++) {
							var candidate = generator.mines().site(x, z); if (candidate.isPresent()) { site = candidate.get(); break; }
						}
						if (site == null) throw new AssertionError("No reachable mine in " + code);
						BlockPos view = site.pos(14, site.slope() * 12, 0);
						player.teleportTo(level, view.getX() + .5, view.getY(), view.getZ() + .5, java.util.Set.of(), site.direction().toYRot(), 5, true);
						player.getAbilities().flying = true; player.onUpdateAbilities();
						player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400));
						level.getChunk(Math.floorDiv(site.cache().getX(), 16), Math.floorDiv(site.cache().getZ(), 16));
						if (!(level.getBlockEntity(site.cache()) instanceof ChestBlockEntity chest)) throw new AssertionError("Generated cache lost its block entity");
						chest.unpackLootTable(player); boolean mineral = false;
						for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(UniverseMaterials.resource(generator.profile().geometry(), 0))) mineral = true;
						if (!mineral) throw new AssertionError("Cache has no local mining reward");
						if (code.equals("00000")) {
							BlockPos ore = null;
							for (int v = -3; v <= 3 && ore == null; v++) for (int h = 0; h <= 3; h++) {
								BlockPos pos = site.pos(43, site.slope() * 12 + h, v);
								if (level.getBlockState(pos).is(UniverseMaterials.block(0, 4))) { ore = pos; break; }
							}
							if (ore == null) throw new AssertionError("Working face has no exposed ore");
							player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
							player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
							if (UniverseMining.permits(level, level.getBlockState(ore), player.getMainHandItem())) throw new AssertionError("Mine ores must still require a diamond-tier tool");
							player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
							if (!player.gameMode.destroyBlock(ore) || player.getMainHandItem().getDamageValue() != 1) throw new AssertionError("Mine ore must be harvestable in survival with normal durability");
							player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
						}
					});
					world.getConnection().waitForChunksRender(); context.waitTicks(15);
					context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
					context.takeScreenshot("hbk-worlds-abandoned-mine-" + code);
				}
			}
		} finally {
			context.runOnClient(client -> { client.options.renderDistance().set(distance); client.options.broadcastOptions(); });
		}
	}
}
