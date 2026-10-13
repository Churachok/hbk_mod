package dev.kirill.hbk;

import dev.kirill.hbk.client.YoungLiberalClient;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.world.GraveyardRecipeBook;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

/** Actual shader, daytime/nighttime sky, flowers and cleanup on a connected client. */
public final class LiberalBloodClientGameTest implements FabricClientGameTest {
	private static SkyRenderState sky(Minecraft client) {
		var state = new SkyRenderState();
		client.levelRenderer.skyRenderer().extractRenderState(client.level, 0, client.gameRenderer.mainCamera(), state);
		return state;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("fill -12 79 -12 12 79 12 minecraft:stone");
			server.runCommand("tp @a 0.5 80 0.5 0 -25");
			server.runCommand("time set noon");
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("hbk-liberal-before");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				var level = player.level();
				var head = ModEntityTypes.LEBEDEV_HEAD.spawn(level, new BlockPos(0, 80, 3), EntitySpawnReason.COMMAND);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
				head.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
				if (head.isRemoved() || !player.getMainHandItem().is(ModItems.LIBERAL_BLOOD_BUCKET)) {
					throw new AssertionError("RMB must fill the bucket and keep the head");
				}
				player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
				player.setItemInHand(InteractionHand.MAIN_HAND,
						player.getMainHandItem().finishUsingItem(level, player));
				player.getInventory().setItem(1, new ItemStack(ModItems.LIBERAL_BLOOD_BUCKET));
			});
			context.waitFor(client -> YoungLiberalClient.isActive()
					&& YoungLiberalClient.POST_EFFECT.equals(client.gameRenderer.currentPostEffect())
					&& sky(client).skyColor == YoungLiberalClient.DAY_SKY_COLOR, 100);
			context.runOnClient(client -> {
				if (client.player.hasEffect(MobEffects.POISON) || !client.player.getMainHandItem().is(Items.BUCKET)) {
					throw new AssertionError("Effect cleanup and returned bucket must synchronize to the client");
				}
			});
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
			context.takeScreenshot("hbk-liberal-day");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(3);
			context.takeScreenshot("hbk-liberal-bucket-and-flag");
			context.setScreen(() -> null);
			server.runCommand("time set midnight");
			server.runCommand("tp @a 0.5 80 0.5 0 -40");
			context.waitFor(client -> sky(client).skyColor == YoungLiberalClient.NIGHT_SKY_COLOR
					&& sky(client).starBrightness == 1.0f, 100);
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
			context.takeScreenshot("hbk-liberal-night-flowers");
			var guide = context.computeOnClient(client -> new BookViewScreen(BookViewScreen.BookAccess.fromItem(GraveyardRecipeBook.create())));
			context.setScreen(() -> guide);
			context.runOnClient(client -> guide.setPage(1 + GraveyardRecipeBook.RECIPES.size()
					+ GraveyardRecipeBook.INFO_PAGES.indexOf("liberal_blood")));
			context.waitTicks(3);
			context.takeScreenshot("hbk-liberal-guide");
			context.setScreen(() -> null);
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.removeEffect(ModEffects.YOUNG_LIBERAL);
				player.removeEffect(MobEffects.REGENERATION);
				player.removeEffect(MobEffects.HASTE);
			});
			context.waitFor(client -> !YoungLiberalClient.isActive()
					&& !YoungLiberalClient.POST_EFFECT.equals(client.gameRenderer.currentPostEffect())
					&& sky(client).skyColor != YoungLiberalClient.NIGHT_SKY_COLOR, 100);
			context.waitTicks(3);
			context.takeScreenshot("hbk-liberal-restored");
		}
	}
}
