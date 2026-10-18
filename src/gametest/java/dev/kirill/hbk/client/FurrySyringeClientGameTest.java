package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.mechanic.FurryTransformation;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real syringe use and attachment removal, wolf/player poses, F5 body and both first-person paws. */
public final class FurrySyringeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(FurrySyringeClientGameTest::verifyWolfAssetsAndPoses);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var camera = context.computeOnClient(client -> client.options.getCameraType());
			var mainArm = context.computeOnClient(client -> client.options.mainHand().get());
			try {
				var server = world.getServer();
				server.runCommand("fill -16 179 -16 16 179 16 minecraft:stone");
				server.runCommand("tp @a 0.5 180 0.5 0 0");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setGameMode(GameType.SURVIVAL);
					player.getInventory().clearContent();
					player.getInventory().setSelectedSlot(0);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_SYRINGE, 2));
					player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND_SWORD));
					player.inventoryMenu.broadcastChanges();
				});
				context.waitFor(client -> client.player != null && client.player.getMainHandItem().is(ModItems.FURRY_SYRINGE)
						&& client.player.getOffhandItem().is(Items.DIAMOND_SWORD), 100);
				context.runOnClient(client -> {
					client.options.mainHand().set(HumanoidArm.RIGHT);
					client.options.broadcastOptions();
					verifySyringeModel(client);
				});

				int color = server.computeOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					int wolves = player.level().getEntities(ModEntityTypes.PINK_FURRY_WOLF, entity -> true).size();
					float width = player.getBbWidth(), height = player.getBbHeight(), health = player.getHealth();
					if (!ModItems.FURRY_SYRINGE.use(player.level(), player, InteractionHand.MAIN_HAND).consumesAction()
							|| player.getMainHandItem().getCount() != 1 || player.getBbWidth() != width
							|| player.getBbHeight() != height || player.getHealth() != health
							|| player.level().getEntities(ModEntityTypes.PINK_FURRY_WOLF, entity -> true).size() != wolves) {
						throw new AssertionError("The actual server syringe must transform the existing player and spend one item, not spawn a wolf or change its body");
					}
					player.inventoryMenu.broadcastChanges();
					return FurryTransformation.getColorId(player);
				});
				context.waitFor(client -> FurryTransformation.getColorId(client.player) == color
						&& client.player.getMainHandItem().getCount() == 1, 100);
				context.runOnClient(client -> {
					verifyCurrentAppearance(client, true);
					client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				});
				capture(context, "furry-player-front-held-syringe-and-sword", 10);

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
					player.inventoryMenu.broadcastChanges();
				});
				context.waitFor(client -> client.player.getMainHandItem().isEmpty() && client.player.getOffhandItem().isEmpty(), 100);
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				capture(context, "furry-player-first-person-paw", 40);

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setAttached(ModAttachments.FURRY_EXPIRES_AT, instance.overworld().getGameTime());
					FurryTransformation.tick(player);
					player.inventoryMenu.broadcastChanges();
				});
				context.waitFor(client -> !FurryTransformation.isActive(client.player), 100);
				context.runOnClient(client -> {
					verifyCurrentAppearance(client, false);
					client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				});
				capture(context, "furry-player-expired-human-restored", 10);
			} finally {
				context.runOnClient(client -> {
					client.options.setCameraType(camera);
					client.options.mainHand().set(mainArm);
					client.options.broadcastOptions();
					client.setScreenAndShow(null);
				});
			}
		}
	}

	private static AvatarRenderState extract(Minecraft client) {
		return (AvatarRenderState) client.getEntityRenderDispatcher().extractEntity(client.player, 0.5f);
	}

	private static void verifyCurrentAppearance(Minecraft client, boolean active) {
		var state = extract(client);
		var renderer = client.getEntityRenderDispatcher().getPlayerRenderer(client.player);
		boolean wolfTexture = renderer.getTextureLocation(state).equals(FurryPlayerClientState.texture(
				FurryTransformation.getColorId(client.player)));
		if (FurryPlayerClientState.isActive(state) != active || wolfTexture != active
				|| (active && state.showCape) || renderer.getModel() instanceof FurryPlayerModel) {
			throw new AssertionError("Only an active synced attachment may change the render; the renderer's shared human model must always be restored after submission");
		}
		if (!active) {
			renderer.getModel().setupAnim(state);
			if (renderer.getModel().jacket.visible != state.showJacket
					|| renderer.getModel().leftSleeve.visible != state.showLeftSleeve
					|| renderer.getModel().rightSleeve.visible != state.showRightSleeve) {
				throw new AssertionError("Expiry must restore normal player clothing visibility without requiring a reload");
			}
		}
	}

	private static void verifyWolfAssetsAndPoses(Minecraft client) {
		for (var color : DyeColor.VALUES) {
			var texture = FurryPlayerClientState.texture(color.getId());
			if (client.getResourceManager().getResource(texture).isEmpty()) {
				throw new AssertionError("All sixteen existing furry textures must be available: " + texture);
			}
		}
		var wolf = new FurryPlayerModel();
		var vanilla = new PlayerModel(PinkFurryWolfModel.createBodyLayer().bakeRoot(), false);
		var state = new AvatarRenderState();
		state.speedValue = 1.0f;
		state.walkAnimationPos = 3.0f;
		state.walkAnimationSpeed = 0.7f;
		state.showHat = state.showJacket = state.showLeftSleeve = state.showRightSleeve = true;
		state.showLeftPants = state.showRightPants = true;
		for (int pose = 0; pose < 4; pose++) {
			state.isCrouching = pose == 1;
			state.isPassenger = pose == 2;
			state.swimAmount = pose == 3 ? 1.0f : 0.0f;
			wolf.setupAnim(state);
			vanilla.setupAnim(state);
			if (wolf.hat.visible || wolf.jacket.visible || wolf.leftSleeve.visible || wolf.rightSleeve.visible
					|| wolf.leftPants.visible || wolf.rightPants.visible
					|| wolf.rightArm.xRot != vanilla.rightArm.xRot || wolf.leftArm.xRot != vanilla.leftArm.xRot
					|| wolf.rightLeg.xRot != vanilla.rightLeg.xRot || wolf.leftLeg.xRot != vanilla.leftLeg.xRot
					|| wolf.body.xRot != vanilla.body.xRot) {
				throw new AssertionError("Wolf geometry must preserve vanilla standing/crouching/riding/swimming poses without floating human clothing");
			}
		}
		state.ageInTicks = 0;
		wolf.setupAnim(state);
		float tail = wolf.body.getChild("tail").yRot;
		state.ageInTicks = 12;
		wolf.setupAnim(state);
		if (tail == wolf.body.getChild("tail").yRot
				|| !wolf.firstPersonArm(true).hasChild("right_paw")
				|| !wolf.firstPersonArm(false).hasChild("left_paw")) {
			throw new AssertionError("The tail must wag and both real first-person arm subtrees must contain their paw geometry");
		}
	}

	private static void verifySyringeModel(Minecraft client) {
		for (var display : new ItemDisplayContext[] {ItemDisplayContext.GUI, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
				ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND}) {
			var state = new ItemStackRenderState();
			client.getItemModelResolver().updateForLiving(state, new ItemStack(ModItems.FURRY_SYRINGE), display, client.player);
			var particle = state.pickParticleMaterial(RandomSource.create(0));
			if (state.isEmpty() || particle == null || particle.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())
					|| state.getModelBoundingBox().getXsize() < 0.01 || state.getModelBoundingBox().getZsize() < 0.01) {
				throw new AssertionError("The syringe must resolve a real three-dimensional model in every GUI/hand context: " + display);
			}
		}
	}

	private static void capture(ClientGameTestContext context, String name, int ticks) {
		context.waitTicks(ticks);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}
}
