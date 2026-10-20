package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.block.JagermeisterCauldronBlock;
import dev.kirill.hbk.block.entity.JagermeisterCauldronBlockEntity;
import dev.kirill.hbk.item.JagermeisterBottleItem;
import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/** Real bottle rendering, ingredient packets/checklist, brewing tint and seven-sip consumption. */
public final class JagermeisterClientGameTest implements FabricClientGameTest {
	private static final BlockPos CAULDRON = new BlockPos(0, 180, 2);
	private static final ItemDisplayContext[] DISPLAYS = {
			ItemDisplayContext.GUI, ItemDisplayContext.FIXED,
			ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
			ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (JagermeisterIngredients.TOTAL_INGREDIENTS != 20 || JagermeisterIngredients.items().getFirst() != Items.GOLDEN_DANDELION
				|| JagermeisterIngredients.items().contains(Items.DANDELION)
				|| JagermeisterIngredients.items().contains(Items.CLOSED_EYEBLOSSOM)) {
			throw new AssertionError("The brewing checklist must contain twenty flowers, retaining golden dandelion but neither removed flower");
		}
		context.runOnClient(JagermeisterClientGameTest::verifyCauldronModels);
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
				server.runCommand("fill -8 179 -8 8 179 8 minecraft:stone");
				server.runCommand("tp @a 0.5 180.25 0.5 0 0");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setGameMode(GameType.SURVIVAL);
					// Keep genuine survival interactions, without random ground-only encounters.
					player.setNoGravity(true);
					player.setOnGround(false);
					player.removeAllEffects();
					player.getInventory().clearContent();
					player.getInventory().setSelectedSlot(0);
					player.getInventory().setItem(1, new ItemStack(ModItems.JAGERMEISTER_BOTTLE));
					player.getInventory().setItem(2, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE));
					player.inventoryMenu.broadcastChanges();
				});
				server.runCommand("tp @a 0.5 180.25 0.5 0 0");
				context.waitFor(client -> client.player != null
						&& client.player.getInventory().getItem(1).is(ModItems.JAGERMEISTER_BOTTLE)
						&& client.player.getInventory().getItem(2).is(ModItems.EMPTY_JAGERMEISTER_BOTTLE), 100);
				context.runOnClient(client -> {
					client.options.mainHand().set(HumanoidArm.RIGHT);
					verifyBottleModels(client);
				});

				Item[] bottles = {ModItems.JAGERMEISTER_BOTTLE, ModItems.EMPTY_JAGERMEISTER_BOTTLE};
				String[] names = {"full", "empty"};
				for (int i = 0; i < bottles.length; i++) {
					Item item = bottles[i];
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst();
						setHand(player, new ItemStack(item));
						player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(item));
						player.inventoryMenu.broadcastChanges();
					});
					context.waitFor(client -> client.player != null
							&& client.player.getMainHandItem().is(item) && client.player.getOffhandItem().is(item), 100);
					context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
					capture(context, "jagermeister-" + names[i] + "-first-person-both-hands-gui");
					context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
					capture(context, "jagermeister-" + names[i] + "-third-person-both-hands");
				}

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
					player.level().setBlockAndUpdate(CAULDRON,
							Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
					rejectRemovedFlowers(player);
					setHand(player, new ItemStack(JagermeisterIngredients.items().getFirst()));
					interact(player);
					rejectRemovedFlowers(player);
				});
				server.runCommand("tp @a 0.5 180.25 0.5 0 27");
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				waitForBrew(context, JagermeisterIngredients.ingredientBit(0), 3, false);
				context.runOnClient(client -> verifyBrewPanelAndTint(client, JagermeisterIngredients.ingredientBit(0), 3, false));
				capture(context, "jagermeister-cauldron-first-ingredient-hud");

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					for (int ingredient = 1; ingredient < JagermeisterIngredients.items().size(); ingredient++) {
						setHand(player, new ItemStack(JagermeisterIngredients.items().get(ingredient)));
						interact(player);
					}
				});
				waitForBrew(context, JagermeisterIngredients.ALL_MASK, 3, true);
				context.runOnClient(client -> verifyBrewPanelAndTint(client, JagermeisterIngredients.ALL_MASK, 3, true));
				capture(context, "jagermeister-cauldron-all-" + JagermeisterIngredients.TOTAL_INGREDIENTS + "-ready-brown-hud");

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					setHand(player, new ItemStack(ModItems.EMPTY_JAGERMEISTER_BOTTLE));
					interact(player);
				});
				waitForBrew(context, JagermeisterIngredients.ALL_MASK, 2, true);
				context.waitFor(client -> client.player != null
						&& client.player.getMainHandItem().is(ModItems.JAGERMEISTER_BOTTLE)
						&& JagermeisterBottleItem.getSips(client.player.getMainHandItem()) == JagermeisterBottleItem.MAX_SIPS, 100);
				context.runOnClient(client -> {
					verifyBrewPanelAndTint(client, JagermeisterIngredients.ALL_MASK, 2, true);
					if (ModItems.JAGERMEISTER_BOTTLE.isBarVisible(client.player.getMainHandItem())) {
						throw new AssertionError("A newly filled seven-sip bottle must not show a partially-empty bar");
					}
				});
				capture(context, "jagermeister-cauldron-full-seven-sip-bottle-two-layers-hud");

				server.runOnServer(instance -> drinkOneSip(instance.getPlayerList().getPlayers().getFirst()));
				context.waitFor(client -> client.player != null
						&& JagermeisterBottleItem.getSips(client.player.getMainHandItem()) == 6
						&& client.player.hasEffect(ModEffects.HUNTERS_INSTINCT) && client.player.hasEffect(MobEffects.NAUSEA), 100);
				context.runOnClient(client -> {
					var bottle = client.player.getMainHandItem();
					if (!ModItems.JAGERMEISTER_BOTTLE.isBarVisible(bottle)
							|| ModItems.JAGERMEISTER_BOTTLE.getBarWidth(bottle) != Math.round(13.0f * 6 / 7)
							|| client.player.getActiveEffects().size() != 2
							|| client.player.hasEffect(MobEffects.NIGHT_VISION)
							|| !HuntersInstinctClient.hasNightVision(client.player)) {
						throw new AssertionError("The first sip must synchronize six charges, Hunter's Instinct and nausea without a separate night-vision effect");
					}
				});
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					for (int sip = 1; sip < JagermeisterBottleItem.MAX_SIPS; sip++) drinkOneSip(player);
					if (!player.getMainHandItem().is(ModItems.EMPTY_JAGERMEISTER_BOTTLE)
							|| player.getMainHandItem().getCount() != 1 || player.getActiveEffects().size() != 2
							|| !player.hasEffect(MobEffects.NAUSEA)
							|| player.getEffect(ModEffects.HUNTERS_INSTINCT) == null
							|| player.getEffect(ModEffects.HUNTERS_INSTINCT).getDuration() < 7 * HuntersInstinctEffect.DURATION_TICKS - 200) {
						throw new AssertionError("Seven survival sips must leave one reusable empty bottle and accumulate twenty-one minutes of one Hunter's Instinct effect");
					}
				});
				context.waitFor(client -> client.player != null
						&& client.player.getMainHandItem().is(ModItems.EMPTY_JAGERMEISTER_BOTTLE)
						&& client.player.hasEffect(ModEffects.HUNTERS_INSTINCT) && client.player.hasEffect(MobEffects.NAUSEA)
						&& client.player.getEffect(ModEffects.HUNTERS_INSTINCT).getDuration() >= 7 * HuntersInstinctEffect.DURATION_TICKS - 200, 100);
			} finally {
				context.runOnClient(client -> {
					client.setScreenAndShow(null);
					client.options.setCameraType(originalCamera);
					client.options.mainHand().set(originalMainHand);
				});
			}
		}
	}

	private static void verifyBottleModels(Minecraft client) {
		double fullHeight = 0;
		for (var item : new Item[]{ModItems.JAGERMEISTER_BOTTLE, ModItems.EMPTY_JAGERMEISTER_BOTTLE}) {
			for (var display : DISPLAYS) {
				var state = new ItemStackRenderState();
				client.getItemModelResolver().updateForLiving(state, new ItemStack(item), display, client.player);
				if (state.isEmpty()) throw new AssertionError("Both Jagermeister models must load in " + display);
				var particle = state.pickParticleMaterial(RandomSource.create(0));
				if (particle == null || particle.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())
						|| !particle.sprite().contents().name().equals(HbkMod.id("item/jagermeister_bottle"))) {
					throw new AssertionError("Bottles must use their real green/orange/stag atlas, not the missing model");
				}
				var box = state.getModelBoundingBox();
				if (box.getYsize() < 0.3 || box.getXsize() < 0.08 || box.getZsize() < 0.08) {
					throw new AssertionError("The squat flat bottle must remain visibly three-dimensional in " + display);
				}
				if (display == ItemDisplayContext.FIXED) {
					if (item == ModItems.JAGERMEISTER_BOTTLE) fullHeight = box.getYsize();
					else if (fullHeight - box.getYsize() < 0.1) {
						throw new AssertionError("The uncapped empty bottle must be visibly different from the sealed full bottle");
					}
				}
			}
		}
	}

	private static void verifyCauldronModels(Minecraft client) {
		var models = client.getModelManager().getBlockStateModelSet();
		for (var state : ModBlocks.JAGERMEISTER_CAULDRON.getStateDefinition().getPossibleStates()) {
			var model = models.get(state);
			if (model == models.missingModel()) throw new AssertionError("Every herbal cauldron state must have a loaded model");
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
						throw new AssertionError("Brewing must preserve the animated vanilla surface at its actual fill height");
					}
				}
			}
			if (!surface || client.getBlockColors().getTintSource(state, 0).color(state) != JagermeisterCauldronColors.surfaceColor(state)) {
				throw new AssertionError("Only the actual herbal liquid surface must receive the registered brewing tint");
			}
		}
		var brewing = ModBlocks.JAGERMEISTER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
				.setValue(JagermeisterCauldronBlock.READY, false);
		if (!client.getModelManager().requiresRender(brewing, brewing.setValue(JagermeisterCauldronBlock.READY, true))) {
			throw new AssertionError("Completing the recipe must invalidate the surface's cached render color");
		}
	}

	private static void waitForBrew(ClientGameTestContext context, int mask, int bottles, boolean ready) {
		context.waitFor(client -> client.level != null
				&& client.level.getBlockState(CAULDRON).is(ModBlocks.JAGERMEISTER_CAULDRON)
				&& client.level.getBlockEntity(CAULDRON) instanceof JagermeisterCauldronBlockEntity brew
				&& brew.getIngredientsMask() == mask
				&& JagermeisterBrewingHud.currentPanel().filter(panel -> panel.pos().equals(CAULDRON)
						&& panel.ingredientsMask() == mask && panel.bottles() == bottles && panel.ready() == ready).isPresent(), 120);
	}

	private static void verifyBrewPanelAndTint(Minecraft client, int mask, int bottles, boolean ready) {
		var panel = JagermeisterBrewingHud.currentPanel().orElseThrow(() -> new AssertionError("Aiming at the actual brew must show its checklist"));
		if (!panel.pos().equals(CAULDRON) || panel.ingredientsMask() != mask || panel.bottles() != bottles || panel.ready() != ready) {
			throw new AssertionError("The HUD must display the synchronized ingredient mask, readiness and remaining bottles");
		}
		var state = client.level.getBlockState(CAULDRON);
		int actual = client.getBlockColors().getTintSource(state, 0).colorInWorld(state, client.level, CAULDRON);
		int expected = ready ? JagermeisterCauldronColors.READY_COLOR : JagermeisterCauldronColors.BREWING_COLOR;
		if (actual != expected || state.getValue(LayeredCauldronBlock.LEVEL) != bottles
				|| state.getValue(JagermeisterCauldronBlock.READY) != ready) {
			throw new AssertionError("The real client liquid must transition from herbal brewing tint to ready brown");
		}
	}

	private static void setHand(ServerPlayer player, ItemStack stack) {
		player.setItemSlot(EquipmentSlot.MAINHAND, stack);
		player.inventoryMenu.broadcastChanges();
	}

	private static void interact(ServerPlayer player) {
		var hit = new BlockHitResult(Vec3.atCenterOf(CAULDRON), Direction.NORTH, CAULDRON, false);
		if (!player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction()) {
			throw new AssertionError("Each real ingredient/empty bottle must interact with the ordinary targeted cauldron");
		}
		player.inventoryMenu.broadcastChanges();
	}

	private static void rejectRemovedFlowers(ServerPlayer player) {
		var previous = player.level().getBlockState(CAULDRON);
		var previousHand = player.getMainHandItem().copy();
		int mask = player.level().getBlockEntity(CAULDRON) instanceof JagermeisterCauldronBlockEntity brew
				? brew.getIngredientsMask() : 0;
		var hit = new BlockHitResult(Vec3.atCenterOf(CAULDRON), Direction.NORTH, CAULDRON, false);
		for (Item removed : new Item[] {Items.DANDELION, Items.CLOSED_EYEBLOSSOM}) {
			setHand(player, new ItemStack(removed, 2));
			if (player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction()
					|| player.getMainHandItem().getCount() != 2 || !player.level().getBlockState(CAULDRON).equals(previous)
					|| player.level().getBlockEntity(CAULDRON) instanceof JagermeisterCauldronBlockEntity brew
							&& brew.getIngredientsMask() != mask) {
				throw new AssertionError("Ordinary dandelion and closed eyeblossom must be rejected by real interactions with fresh water and unfinished brew");
			}
		}
		setHand(player, previousHand);
	}

	private static void drinkOneSip(ServerPlayer player) {
		if (!player.getMainHandItem().is(ModItems.JAGERMEISTER_BOTTLE)) {
			throw new AssertionError("Every remaining sip must still come from the same filled bottle");
		}
		setHand(player, player.getMainHandItem().finishUsingItem(player.level(), player));
	}

	private static void capture(ClientGameTestContext context, String name) {
		context.waitTicks(40);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}
}
