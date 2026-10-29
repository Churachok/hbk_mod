package dev.kirill.hbk.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kirill.hbk.entity.SkippingOwlEntity;
import dev.kirill.hbk.network.SkippingOwlScenePayload;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.world.SkippingOwlEncounter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.UUID;

/** A waiting world owl and a real encounter, with rope phases, threatening eyes and baked UV/clearance checks. */
public final class SkippingOwlClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(SkippingOwlClientGameTest::verifyModelAndTexture);
		context.runOnClient(SkippingOwlClientGameTest::verifyCardFramesAndAudioGate);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var camera = context.computeOnClient(client -> client.options.getCameraType());
			boolean hiddenHud = context.computeOnClient(client -> client.gui.hud.isHidden());
			try {
				var server = world.getServer();
				server.runCommand("fill -16 179 -16 16 179 16 minecraft:stone");
				server.runCommand("tp @a 0.5 180 0.5 0 0");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				int idleId = server.computeOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.getInventory().clearContent();
					player.inventoryMenu.broadcastChanges();
					var owl = ModEntityTypes.SKIPPING_OWL.create(player.level(), EntitySpawnReason.COMMAND);
					if (owl == null) throw new AssertionError("The world owl must be natively spawnable without an owner");
					owl.snapTo(6.5, 180, 0.5, 90, 0);
					owl.setYBodyRot(90);
					owl.setYHeadRot(90);
					owl.setOldRot();
					if (!player.level().addFreshEntity(owl) || !owl.isWaitingForPlayer() || !owl.shouldBeSaved()) {
						throw new AssertionError("An unowned world owl must wait and remain saveable");
					}
					return owl.getId();
				});
				context.waitFor(client -> client.level != null && client.level.getEntity(idleId) instanceof SkippingOwlEntity owl
						&& owl.isWaitingForPlayer(), 100);
				context.waitTicks(40);
				context.runOnClient(client -> {
					var owl = (SkippingOwlEntity) client.level.getEntity(idleId);
					if (owl == null || !owl.isWaitingForPlayer() || owl.getPerformanceAge(0) != 0 || SkippingOwlScreenEffect.isActive()
							|| !(client.getEntityRenderDispatcher().getRenderer(owl) instanceof SkippingOwlRenderer renderer)) {
						throw new AssertionError("The real idle owl must remain present and waiting beside a creative player");
					}
					var state = renderer.createRenderState();
					renderer.extractRenderState(owl, state, 0.5f);
					renderer.getModel().setupAnim(state);
					var rig = renderer.getModel().root().getChild("performance");
					if (!state.waitingForPlayer || state.performanceAge != 0
							|| Math.abs(state.idleAge - (owl.level().getGameTime() % 20 + 0.5f)) > 0.001f
							|| rig.getChild("left_brow").visible || rig.getChild("left_pupil").xScale != 1) {
						throw new AssertionError("The idle renderer must use world-time looping and the original wide-eyed expression");
					}
					client.options.setCameraType(CameraType.FIRST_PERSON);
				});
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					if (!(player.level().getEntity(idleId) instanceof SkippingOwlEntity owl)
							|| !owl.isWaitingForPlayer() || owl.getOwnerUUID() != null || SkippingOwlEncounter.isActive(player)) {
						throw new AssertionError("Creative proximity must neither remove the world owl nor claim its encounter");
					}
				});
				context.getInput().lookAt(-90, 8);
				capture(context, "skipping-owl-world-idle-wide-eyes");
				context.getInput().lookAt(0, 0);
				int id = server.computeOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setYRot(0);
					player.setXRot(0);
					var result = SkippingOwlEncounter.start(player);
					var owl = SkippingOwlEncounter.activeOwl(player);
					if (result != SkippingOwlEncounter.StartResult.STARTED || owl == null || owl.getId() == idleId) {
						throw new AssertionError("The real owl encounter must start on the clear dry platform: " + result);
					}
					return owl.getId();
				});
				context.waitFor(client -> client.level != null && client.level.getEntity(id) instanceof SkippingOwlEntity owl
						&& owl.getPerformanceAge(0) > 0 && SkippingOwlScreenEffect.isActive()
						&& SkippingOwlScreenEffect.owlEntityId() == id, 100);
				long sceneStartedAtGameTime = context.computeOnClient(client -> client.level.getGameTime()
						- (long) SkippingOwlScreenEffect.age(0));
				context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
				context.waitFor(client -> {
					float age = SkippingOwlScreenEffect.age(0);
					var frame = SkippingOwlScreenEffect.frameAtAge(age);
					return age > 20 && age < 160 && frame.opacity() > (0.12f + frame.clarity() * 0.8f) * 0.85f;
				}, 100);
				context.takeScreenshot("skipping-owl-card-first-blurred-glimpse");
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					new SkippingOwlScenePayload(UUID.randomUUID(), id, player.level().getGameTime(), false).sendTo(player);
				});
				context.waitTicks(2);
				context.runOnClient(client -> {
					if (!SkippingOwlScreenEffect.isActive() || SkippingOwlScreenEffect.owlEntityId() != id) {
						throw new AssertionError("A stale stop packet for another owl must not clear the real owner-only card scene");
					}
				});
				float[] aim = context.computeOnClient(client -> {
					var owl = (SkippingOwlEntity) client.level.getEntity(id);
					if (!(client.getEntityRenderDispatcher().getRenderer(owl) instanceof SkippingOwlRenderer renderer)
							|| !renderer.getTextureLocation(renderer.createRenderState()).equals(SkippingOwlRenderer.TEXTURE)
							|| !renderer.getModel().root().hasChild("performance")) {
						throw new AssertionError("The real server owl must use its registered model, renderer and actual atlas");
					}
					var state = renderer.createRenderState();
					renderer.extractRenderState(owl, state, 0.5f);
					if (state.waitingForPlayer || Math.abs(state.performanceAge - owl.getPerformanceAge(0.5f)) > 0.01) {
						throw new AssertionError("The renderer must use the synchronized encounter clock rather than tracking-local tickCount");
					}
					client.options.setCameraType(CameraType.FIRST_PERSON);
					double x = owl.getX() - client.player.getX(), z = owl.getZ() - client.player.getZ();
					return new float[] {(float) Math.toDegrees(Math.atan2(-x, z)),
							(float) Math.toDegrees(Math.atan2(client.player.getEyeY() - owl.getY() - 0.7, Math.hypot(x, z)))};
				});
				context.getInput().lookAt(aim[0], aim[1]);
				context.waitFor(client -> client.level.getEntity(id) instanceof SkippingOwlEntity owl
						&& nearPhase(owl, 0, 0.08f), 60);
				capture(context, "skipping-owl-rope-under-feet-hop");
				context.waitFor(client -> client.level.getEntity(id) instanceof SkippingOwlEntity owl
						&& nearPhase(owl, 0.5f, 0.1f), 60);
				capture(context, "skipping-owl-rope-overhead-big-eyes");
				context.waitFor(client -> client.level.getEntity(id) instanceof SkippingOwlEntity owl
						&& owl.getPerformanceAge(0) >= SkippingOwlEncounter.OMEN_TICKS, 300);
				context.runOnClient(client -> {
					var owl = (SkippingOwlEntity) client.level.getEntity(id);
					var renderer = (SkippingOwlRenderer) client.getEntityRenderDispatcher().getRenderer(owl);
					var state = renderer.createRenderState();
					renderer.extractRenderState(owl, state, 0.5f);
					renderer.getModel().setupAnim(state);
					var rig = renderer.getModel().root().getChild("performance");
					if (!rig.getChild("left_brow").visible || rig.getChild("left_brow").zRot < 0.27f
							|| rig.getChild("right_brow").zRot > -0.27f || rig.getChild("left_pupil").xScale > 0.56f
							|| client.player.isDeadOrDying()) {
						throw new AssertionError("The real twelve-second phase must show narrowed pupils and threatening brows while the creative observer remains safe");
					}
				});
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					if (!(player.level().getEntity(idleId) instanceof SkippingOwlEntity owl) || !owl.isWaitingForPlayer()) {
						throw new AssertionError("An independent world owl must still wait throughout a nearby commanded encounter");
					}
				});
				capture(context, "skipping-owl-danger-phase-angry-eyes-fast-rope");
				context.waitFor(client -> {
					float age = SkippingOwlScreenEffect.age(0);
					var frame = SkippingOwlScreenEffect.frameAtAge(age);
					return age >= 310 && !frame.blackout() && frame.clarity() > 0.8f && frame.opacity() > 0.65f;
				}, 120);
				context.takeScreenshot("skipping-owl-card-late-clearer-glimpse");
				context.waitFor(client -> SkippingOwlScreenEffect.isBlackout(), 80);
				context.runOnClient(client -> {
					if (SkippingOwlScreenEffect.isLaughStarted() || SkippingOwlScreenEffect.age(0) >= 395) {
						throw new AssertionError("The opaque final card must begin with its own two-second silent hold, not immediate laughter");
					}
					verifyOnlyFinalCardGui(client);
				});
				context.takeScreenshot("skipping-owl-card-opaque-on-black-silent-hold");
				context.runOnClient(client -> {
					if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
					client.setScreenAndShow(new PauseScreen(true) {
						@Override public boolean isPauseScreen() { return false; }
					});
				});
				context.waitTicks(2);
				context.runOnClient(client -> {
					if (!client.gui.hud.isHidden() || client.gui.screen() == null || !SkippingOwlScreenEffect.isBlackout()) {
						throw new AssertionError("The final card test must actually cover a hidden HUD and an open menu without changing either setting");
					}
					verifyOnlyFinalCardGui(client);
				});
				context.takeScreenshot("skipping-owl-card-only-object-over-menu-and-f1");
				context.runOnClient(client -> client.setScreenAndShow(null));
				context.waitFor(client -> SkippingOwlScreenEffect.isLaughStarted(), 80);
				context.runOnClient(client -> {
					if (SkippingOwlScreenEffect.age(0) < SkippingOwlEncounter.DURATION_TICKS + SkippingOwlEncounter.CARD_HOLD_TICKS
							|| !SkippingOwlScreenEffect.isBlackout() || client.player.isDeadOrDying()) {
						throw new AssertionError("Laughter must begin after the complete card hold while the creative owner is still alive");
					}
				});
				context.waitTicks(30);
				context.runOnClient(client -> {
					if (!SkippingOwlScreenEffect.isActive() || !SkippingOwlScreenEffect.isBlackout() || !SkippingOwlScreenEffect.isLaughStarted()) {
						throw new AssertionError("Repeated server snapshots must not restart or dismiss the card during laughter");
					}
				});
				context.takeScreenshot("skipping-owl-card-during-ominous-laughter");
				context.waitFor(client -> !SkippingOwlScreenEffect.isActive()
						|| client.level.getGameTime() - sceneStartedAtGameTime >= SkippingOwlEncounter.FINALE_TICKS + 60, 220);
				context.runOnClient(client -> {
					long elapsed = client.level.getGameTime() - sceneStartedAtGameTime;
					if (SkippingOwlScreenEffect.isActive() || elapsed < SkippingOwlEncounter.FINALE_TICKS
							|| elapsed >= SkippingOwlEncounter.FINALE_TICKS + 60) {
						throw new AssertionError("The real client completion acknowledgement must finish the scene after laughter and before the 660-tick fallback; elapsed="
								+ elapsed + ", active=" + SkippingOwlScreenEffect.isActive() + ", laughActive=" + SkippingOwlScreenEffect.isLaughActive());
					}
				});
				context.waitFor(client -> client.level.getEntity(id) == null, 40);
				context.runOnClient(client -> {
					if (SkippingOwlScreenEffect.isLaughStarted() || SkippingOwlScreenEffect.isLaughActive()
							|| !client.player.isAlive() || client.level.getEntity(id) != null) {
						throw new AssertionError("The server's single finale must leave the creative owner safe and clear the card, audio and owl");
					}
				});
			} finally {
				context.runOnClient(client -> {
					client.options.setCameraType(camera);
					if (client.gui.hud.isHidden() != hiddenHud) client.gui.hud.toggle();
					client.setScreenAndShow(null);
				});
			}
		}
	}

	private static boolean nearPhase(SkippingOwlEntity owl, float target, float tolerance) {
		float cycles = SkippingOwlEncounter.jumpCycles(owl.getPerformanceAge(0));
		float difference = Math.abs(cycles - (float) Math.floor(cycles) - target);
		return Math.min(difference, 1 - difference) <= tolerance;
	}

	private static void verifyModelAndTexture(Minecraft client) {
		try (var stream = client.getResourceManager().getResourceOrThrow(SkippingOwlRenderer.TEXTURE).open();
				var image = NativeImage.read(stream)) {
			if (image.getWidth() != 128 || image.getHeight() != 128) throw new AssertionError("The owl must load its real four-quadrant 128px atlas");
		} catch (IOException error) {
			throw new UncheckedIOException(error);
		}
		var model = new SkippingOwlModel(SkippingOwlModel.createBodyLayer().bakeRoot());
		int[] cubes = {0};
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			cubes[0]++;
			float minU = 1, maxU = 0, minV = 1, maxV = 0;
			for (var polygon : cube.polygons) {
				for (var vertex : polygon.vertices()) {
					minU = Math.min(minU, vertex.u()); maxU = Math.max(maxU, vertex.u());
					minV = Math.min(minV, vertex.v()); maxV = Math.max(maxV, vertex.v());
				}
			}
			if (minU < 0 || minV < 0 || maxU > 1 || maxV > 1
					|| (int) (minU * 2) != (int) (maxU * 2) || (int) (minV * 2) != (int) (maxV * 2)) {
				throw new AssertionError("Every physical eye, feather, foot and rope cuboid must stay within one atlas quadrant: " + path);
			}
		});
		if (cubes[0] < 50) throw new AssertionError("The owl must have meshed eyes and a segmented rope, not a flat face or billboard");
		var rig = model.root().getChild("performance");
		var rope = rig.getChild("rope");
		for (int segment = 0; segment < 24; segment++) {
			if (!rope.hasChild("segment_" + segment)) throw new AssertionError("The visible curved cord needs all twenty-four segments");
		}
		var state = new SkippingOwlRenderState();
		state.performanceAge = 0;
		model.setupAnim(state);
		if (Math.abs(rig.y + 5.2f) > 0.001 || rope.xRot != 0
				|| SkippingOwlModel.HANDLE_Y + SkippingOwlModel.ROPE_DEPTH + rig.y + 0.18f >= 24) {
			throw new AssertionError("The .325-block hop must clear the underfoot cord above the ground plane");
		}
		state.performanceAge = ageAtCycles(0.5f);
		model.setupAnim(state);
		if (rig.y != 0 || Math.abs(rope.xRot - (float) Math.PI) > 0.001) {
			throw new AssertionError("Half an accelerating rotation later the rope must pass overhead and the owl must be back on its feet");
		}
		state.performanceAge = ageAtCycles(1);
		model.setupAnim(state);
		if (Math.abs(rig.y + 5.2f) > 0.001) throw new AssertionError("The hop must remain locked to each complete accelerating rope rotation");
		float early = SkippingOwlEncounter.jumpCycles(1) - SkippingOwlEncounter.jumpCycles(0);
		float late = SkippingOwlEncounter.jumpCycles(360) - SkippingOwlEncounter.jumpCycles(359);
		if (Math.abs(early * 20 - 1) > 0.01 || Math.abs(late * 20 - 3) > 0.01) {
			throw new AssertionError("The shared rope clock must accelerate continuously from one to three rotations per second");
		}
		state.performanceAge = 0;
		model.setupAnim(state);
		if (rig.getChild("left_brow").visible || rig.getChild("left_pupil").xScale != 1) {
			throw new AssertionError("The performance must begin with the original wide-eyed expression");
		}
		state.performanceAge = 180;
		model.setupAnim(state);
		float intermediateBrow = rig.getChild("left_brow").zRot;
		state.performanceAge = 240;
		model.setupAnim(state);
		if (intermediateBrow <= 0 || intermediateBrow >= rig.getChild("left_brow").zRot
				|| rig.getChild("left_brow").zRot < 0.27f || rig.getChild("right_brow").zRot > -0.27f
				|| rig.getChild("left_pupil").xScale > 0.56f) {
			throw new AssertionError("The ominous brows and narrow pupils must progress smoothly to full intensity by twelve seconds");
		}
		for (float age = 0; age <= 360; age += 0.25f) {
			state.performanceAge = age;
			model.setupAnim(state);
			float below = Math.max(0, (float) Math.cos(rope.xRot)) * SkippingOwlModel.ROPE_DEPTH;
			if (SkippingOwlModel.HANDLE_Y + below + rig.y + 0.18f >= 24) {
				throw new AssertionError("Acceleration must not push the attached cord through the ground at any phase");
			}
		}
		state.waitingForPlayer = true;
		state.performanceAge = 360;
		for (int idleAge : new int[] {0, 10, 20}) {
			state.idleAge = idleAge;
			model.setupAnim(state);
			float expectedLift = idleAge == 10 ? 0 : -SkippingOwlModel.HOP_PIXELS;
			if (Math.abs(rope.xRot - (float) Math.PI * idleAge / 10) > 0.001f
					|| Math.abs(rig.y - expectedLift) > 0.001f
					|| rig.getChild("left_brow").visible || rig.getChild("right_brow").visible
					|| rig.getChild("left_pupil").xScale != 1 || rig.getChild("left_glint").yScale != 1) {
				throw new AssertionError("An idle world owl must loop at one rotation per second with wide eyes, even after rendering the threatening finale");
			}
		}
	}

	private static float ageAtCycles(float cycles) {
		float duration = SkippingOwlEncounter.DURATION_TICKS;
		return ((float) Math.sqrt(duration * duration + 80 * duration * cycles) - duration) / 2;
	}

	private static void verifyCardFramesAndAudioGate(Minecraft client) {
		try (var stream = client.getResourceManager().getResourceOrThrow(SkippingOwlScreenEffect.CARD).open();
				var image = NativeImage.read(stream)) {
			if (image.getWidth() != SkippingOwlScreenEffect.CARD_WIDTH || image.getHeight() != SkippingOwlScreenEffect.CARD_HEIGHT) {
				throw new AssertionError("The screen effect must load the real full-resolution original card, not the owl material atlas");
			}
		} catch (IOException failure) {
			throw new UncheckedIOException(failure);
		}
		float previousOpacity = -1, previousClarity = -1;
		for (int jump = 0; jump < 36; jump++) {
			var frame = SkippingOwlScreenEffect.frameAtAge(ageAtCycles(jump) + 3);
			if (frame.jump() != jump || frame.blackout() || frame.opacity() <= previousOpacity || frame.clarity() <= previousClarity) {
				throw new AssertionError("Each accelerating jump must produce its own increasingly opaque and increasingly clear card flash");
			}
			previousOpacity = frame.opacity();
			previousClarity = frame.clarity();
		}
		var rising = SkippingOwlScreenEffect.frameAtAge(ageAtCycles(12) + 1);
		var peak = SkippingOwlScreenEffect.frameAtAge(ageAtCycles(12) + 3);
		if (Math.abs(rising.clarity() - peak.clarity()) > 0.00001f || rising.opacity() >= peak.opacity()
				|| SkippingOwlScreenEffect.frameAtAge(8).opacity() != 0) {
			throw new AssertionError("The short opacity envelope must not change a jump's blur amount, and early gaps must hide the card completely");
		}
		for (float age : new float[] {360, 399, 400, 559, 560, 659}) {
			var frame = SkippingOwlScreenEffect.frameAtAge(age);
			if (!frame.blackout() || frame.opacity() != 1 || frame.clarity() != 1) {
				throw new AssertionError("The last jump must keep the original card perfectly sharp and opaque on black throughout the hold and laughter");
			}
		}
		if (SkippingOwlScreenEffect.canAcknowledge(559, true, 400, false)
				|| SkippingOwlScreenEffect.canAcknowledge(560, true, 400, true)
				|| SkippingOwlScreenEffect.canAcknowledge(560, false, 400, false)
				|| SkippingOwlScreenEffect.canAcknowledge(560, true, 430, false)
				|| !SkippingOwlScreenEffect.canAcknowledge(560, true, 400, false)
				|| !SkippingOwlScreenEffect.canAcknowledge(590, true, 430, false)) {
			throw new AssertionError("The completion acknowledgement must wait for both the minimum scene time and the full sound, including late or still-active playback");
		}
	}

	private static void verifyOnlyFinalCardGui(Minecraft client) {
		var state = new GuiRenderState();
		var before = new GuiGraphicsExtractor(client, state, 0, 0);
		before.fill(0, 0, before.guiWidth(), before.guiHeight(), 0xFFFF00FF);
		before.text(client.font, "MENU AND HUD MUST DISAPPEAR", 5, 5, 0xFFFFFFFF);
		SkippingOwlScreenEffect.extract(client, state, DeltaTracker.ZERO);
		int[] rectangles = {0}, cards = {0}, others = {0}, text = {0}, items = {0}, pictures = {0};
		var descriptions = new ArrayList<String>();
		state.forEachElement(element -> {
			descriptions.add(element.getClass().getSimpleName() + " bounds=" + element.bounds());
			if (element instanceof ColoredRectangleRenderState rectangle && rectangle.col1() == 0xFF000000
					&& rectangle.col2() == 0xFF000000
					// Native GuiGraphicsExtractor.fill normalizes its stored corners in descending order.
					&& Math.min(rectangle.x0(), rectangle.x1()) == 0 && Math.min(rectangle.y0(), rectangle.y1()) == 0
					&& Math.max(rectangle.x0(), rectangle.x1()) == before.guiWidth()
					&& Math.max(rectangle.y0(), rectangle.y1()) == before.guiHeight()) {
				rectangles[0]++;
			} else if (element instanceof BlitRenderState card && card.color() == 0xFFFFFFFF
					&& card.pipeline().getLocation().equals(dev.kirill.hbk.HbkMod.id("pipeline/skipping_owl_card"))) {
				float aspect = (float) (card.x1() - card.x0()) / (card.y1() - card.y0());
				if (Math.abs(aspect - (float) SkippingOwlScreenEffect.CARD_WIDTH / SkippingOwlScreenEffect.CARD_HEIGHT) > 0.01f) {
					throw new AssertionError("The final card must preserve its original aspect ratio instead of stretching to the viewport");
				}
				cards[0]++;
			} else others[0]++;
		}, GuiRenderState.TraverseRange.ALL);
		state.forEachText(element -> text[0]++);
		state.forEachItem(element -> items[0]++);
		state.forEachPictureInPicture(element -> pictures[0]++);
		if (rectangles[0] != 1 || cards[0] != 1 || others[0] != 0 || text[0] != 0 || items[0] != 0 || pictures[0] != 0) {
			throw new AssertionError("Blackout must replace every extracted HUD/menu element with exactly one solid-black background and one opaque card: black="
					+ rectangles[0] + ", cards=" + cards[0] + ", other=" + others[0] + ", text=" + text[0] + ", items=" + items[0]
					+ ", pictures=" + pictures[0] + ", viewport=" + before.guiWidth() + "x" + before.guiHeight() + ", elements=" + descriptions);
		}
	}

	private static void capture(ClientGameTestContext context, String name) {
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}
}
