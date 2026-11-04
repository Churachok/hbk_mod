package dev.kirill.hbk.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.network.SkippingOwlLaughFinishedPayload;
import dev.kirill.hbk.network.SkippingOwlScenePayload;
import dev.kirill.hbk.registry.ModSounds;
import dev.kirill.hbk.world.SkippingOwlEncounter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.UUID;

/** Owner-only glimpses of the real card, followed by an opaque card and a finite sound-backed finale. */
public final class SkippingOwlScreenEffect {
	public static final Identifier CARD = HbkMod.id("textures/gui/skipping_owl_card.png");
	public static final int CARD_WIDTH = 1862;
	public static final int CARD_HEIGHT = 2048;
	public static final int FLASH_TICKS = 6;
	private static final RenderPipeline CARD_PIPELINE = cardPipeline();
	private static UUID encounterId;
	private static int owlEntityId = -1;
	private static ClientLevel level;
	private static long startedAt;
	private static SimpleSoundInstance laughter;
	private static boolean laughStarted;
	private static float laughStartedAge;
	private static long nextAcknowledgementAt;

	private SkippingOwlScreenEffect() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SkippingOwlScenePayload.TYPE, (payload, context) ->
				context.client().execute(() -> receive(context.client(), payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
		ClientTickEvents.END_CLIENT_TICK.register(SkippingOwlScreenEffect::tick);
	}

	private static void receive(Minecraft client, SkippingOwlScenePayload payload) {
		if (!payload.active()) {
			// A delayed end packet from an older owl must not clear the next encounter.
			if (payload.encounterId().equals(encounterId)) reset(client);
			return;
		}
		if (payload.encounterId().equals(encounterId)) return;
		reset(client);
		if (client.level == null || client.player == null || !client.player.isAlive()) return;
		encounterId = payload.encounterId();
		owlEntityId = payload.owlEntityId();
		level = client.level;
		startedAt = payload.startedAtGameTime();
	}

	private static void tick(Minecraft client) {
		if (!validate(client) || client.isPaused()) return;
		float age = age(0);
		if (!laughStarted && age >= laughStartTick()) {
			laughStarted = true;
			laughStartedAge = age;
			laughter = SimpleSoundInstance.forUI(ModSounds.SKIPPING_OWL_LAUGH, 1.0f, 1.0f);
			client.getSoundManager().play(laughter);
		}
		// A muted/unavailable sound still waits its full duration; a delayed or actually
		// playing sound can never acknowledge while playback is still active.
		if (canAcknowledge(age, laughStarted, laughStartedAge, isLaughActive())
				&& client.level.getGameTime() >= nextAcknowledgementAt
				&& ClientPlayNetworking.canSend(SkippingOwlLaughFinishedPayload.TYPE)) {
			ClientPlayNetworking.send(new SkippingOwlLaughFinishedPayload(encounterId));
			nextAcknowledgementAt = client.level.getGameTime() + 20;
		}
	}

	/** Invoked after the entire GUI has been extracted, including screens, tooltips and hidden-HUD mode. */
	public static void extract(Minecraft client, GuiRenderState guiState, DeltaTracker deltaTracker) {
		if (!validate(client)) return;
		Frame frame = frameAtAge(age(deltaTracker.getGameTimeDeltaPartialTick(false)));
		if (frame.opacity() <= 0) return;
		if (frame.blackout()) {
			// Reset this frame's render state, not any user setting or screen. No HUD,
			// subtitles, menu buttons or crosshair can remain above the final card.
			guiState.reset();
			guiState.isHudHidden = false;
		}
		var graphics = new GuiGraphicsExtractor(client, guiState, 0, 0);
		graphics.nextStratum();
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		if (frame.blackout()) {
			graphics.fill(0, 0, width, height, 0xFF000000);
			graphics.nextStratum();
		}
		float fraction = frame.blackout() ? 0.85f : 0.72f;
		float scale = Math.min(width * 0.9f / CARD_WIDTH, height * fraction / CARD_HEIGHT);
		int cardWidth = Math.max(1, Math.round(CARD_WIDTH * scale));
		int cardHeight = Math.max(1, Math.round(CARD_HEIGHT * scale));
		int x = (width - cardWidth) / 2, y = (height - cardHeight) / 2;
		// R is an independent blur/clarity parameter for this shader, never a tint.
		// Keeping opacity in A means the short flash envelope does not change clarity.
		int color = Math.round(frame.opacity() * 255) << 24 | Math.round(frame.clarity() * 255) << 16 | 0xFFFF;
		graphics.blit(CARD_PIPELINE, CARD, x, y, 0, 0, cardWidth, cardHeight,
				CARD_WIDTH, CARD_HEIGHT, CARD_WIDTH, CARD_HEIGHT, color);
	}

	private static RenderPipeline cardPipeline() {
		var base = RenderPipelines.GUI_TEXTURED;
		var builder = RenderPipeline.builder().withLocation(HbkMod.id("pipeline/skipping_owl_card"))
				.withVertexShader(base.getVertexShader()).withFragmentShader(HbkMod.id("core/skipping_owl_card"))
				.withVertexBinding(0, base.getVertexFormatBinding(0)).withPrimitiveTopology(base.getPrimitiveTopology())
				.withCull(base.isCull()).withPolygonMode(base.getPolygonMode())
				.withDepthStencilState(Optional.ofNullable(base.getDepthStencilState()))
				.withColorTargetState(base.getColorTargetState());
		for (var layout : base.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
		return builder.build();
	}

	private static boolean validate(Minecraft client) {
		if (encounterId == null) return false;
		if (client.level != level || client.player == null || !client.player.isAlive() || client.player.isSpectator()
				|| client.getConnection() == null) {
			reset(client);
			return false;
		}
		return true;
	}

	private static int laughStartTick() {
		return SkippingOwlEncounter.DURATION_TICKS + SkippingOwlEncounter.CARD_HOLD_TICKS;
	}

	public static boolean canAcknowledge(float age, boolean started, float startedAge, boolean soundActive) {
		return started && age >= SkippingOwlEncounter.FINALE_TICKS
				&& age - startedAge >= SkippingOwlEncounter.LAUGH_DURATION_TICKS && !soundActive;
	}

	/** A pure, frame-rate-independent description of each jump's short card flash. */
	public static Frame frameAtAge(float age) {
		float clamped = Math.max(0, age);
		int lastJump = Mth.floor(SkippingOwlEncounter.jumpCycles(SkippingOwlEncounter.DURATION_TICKS));
		if (clamped >= SkippingOwlEncounter.DURATION_TICKS) return new Frame(lastJump, 1, 1, true);
		int jump = Mth.floor(SkippingOwlEncounter.jumpCycles(clamped));
		float duration = SkippingOwlEncounter.DURATION_TICKS;
		float jumpAt = ((float) Math.sqrt(duration * duration + 80 * duration * jump) - duration) / 2;
		float flashAge = clamped - jumpAt;
		float envelope = flashAge >= 0 && flashAge < FLASH_TICKS ? (float) Math.sin(Math.PI * flashAge / FLASH_TICKS) : 0;
		float clarity = Mth.clamp((float) jump / lastJump, 0, 1);
		return new Frame(jump, (0.12f + 0.8f * clarity) * Math.max(0, envelope), clarity, false);
	}

	public static boolean isActive() { return encounterId != null; }
	public static UUID encounterId() { return encounterId; }
	public static int owlEntityId() { return owlEntityId; }
	public static boolean isBlackout() { return isActive() && age(0) >= SkippingOwlEncounter.DURATION_TICKS; }
	public static boolean isLaughStarted() { return laughStarted; }
	public static boolean isLaughActive() {
		return laughter != null && Minecraft.getInstance().getSoundManager().isActive(laughter);
	}
	public static float age(float partialTick) {
		return level == null ? 0 : Math.max(0, level.getGameTime() - startedAt + partialTick);
	}

	private static void reset(Minecraft client) {
		if (laughter != null) client.getSoundManager().stop(laughter);
		encounterId = null;
		owlEntityId = -1;
		level = null;
		startedAt = 0;
		laughter = null;
		laughStarted = false;
		laughStartedAge = 0;
		nextAcknowledgementAt = 0;
	}

	public record Frame(int jump, float opacity, float clarity, boolean blackout) {}
}
