package dev.kirill.hbk.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.kirill.hbk.HbkMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.resources.Identifier;

import java.nio.ByteOrder;

/** Two independent glitch rolls plus desaturation during auditory outages. */
public final class AnomalyScreenEffects {
	private static final Identifier SCRATCHES = HbkMod.id("anomaly_scratches");
	private static final Identifier DISTORTION = HbkMod.id("anomaly_distortion");
	private static final Identifier BOTH = HbkMod.id("anomaly_screen_glitch");
	private static final int DURATION = 100;
	private static final int FADE_TICKS = 15;
	private static ClientLevel level;
	private static int secondTicks;
	private static int scratchTicks;
	private static int distortionTicks;
	private static MappableRingBuffer effectUniforms;
	private static boolean rendering;

	private AnomalyScreenEffects() {}

	private static boolean eligible(Minecraft client) {
		return client.level != null && client.player != null && client.player.isAlive() && AnomalyClient.active();
	}

	public static void clear() {
		level = null;
		secondTicks = scratchTicks = distortionTicks = 0;
		if (effectUniforms != null) {
			effectUniforms.close();
			effectUniforms = null;
		}
		rendering = false;
	}

	public static void tick(Minecraft client) {
		if (client.level != level) { clear(); level = client.level; }
		if (!eligible(client)) {
			secondTicks = scratchTicks = distortionTicks = 0;
			// A placed fractured block can keep its lens effect outside the generated zones.
			if (FracturedBlockClient.strength(1) <= 0) clear();
			return;
		}
		if (client.isPaused()) return;
		if (scratchTicks > 0) scratchTicks--;
		if (distortionTicks > 0) distortionTicks--;
		if (++secondTicks < 20) return;
		secondTicks = 0;
		var random = client.level.getRandom();
		boolean scratches = random.nextFloat() < 0.02f;
		boolean distortion = random.nextFloat() < 0.01f;
		// Repeated successes cannot prolong a running episode; different effects can overlap.
		if (scratches && scratchTicks == 0) scratchTicks = DURATION;
		if (distortion && distortionTicks == 0) distortionTicks = DURATION;
	}

	private static float strength(int remaining, float partialTick) {
		if (remaining <= 0) return 0;
		float age = DURATION - remaining + partialTick;
		float ramp = Math.clamp(Math.min(age, DURATION - age) / FADE_TICKS, 0, 1);
		return ramp * ramp * (3 - 2 * ramp);
	}

	/** Only replaces our post-pass configuration while that chain is executing. */
	public static GpuBuffer uniforms(GpuBuffer fallback) {
		return rendering && effectUniforms != null ? effectUniforms.currentBuffer() : fallback;
	}

	public static void render(RenderTarget target, GraphicsResourceAllocator resources, float partialTick) {
		var client = Minecraft.getInstance();
		if (client.level == null || client.player == null || !client.player.isAlive()) return;
		boolean anomaly = client.level == level && eligible(client);
		float effectScale = client.options.screenEffectScale().get().floatValue();
		float scratchStrength = anomaly ? strength(scratchTicks, partialTick) : 0;
		float distortionStrength = anomaly ? strength(distortionTicks, partialTick) * effectScale : 0;
		float desaturation = AnomalyAudio.desaturation(partialTick);
		float lens = FracturedBlockClient.strength(partialTick) * effectScale;
		boolean scratches = scratchStrength > 0;
		boolean distortion = distortionStrength > 0;
		if (!scratches && !distortion && desaturation <= 0 && lens <= 0) return;
		var id = scratches ? distortion ? BOTH : SCRATCHES : distortion ? DISTORTION : BOTH;
		// Use an additional pass so existing potion, glasses and camera post effects retain ownership.
		var chain = client.getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS);
		if (chain == null) return;
		if (effectUniforms == null) {
			effectUniforms = new MappableRingBuffer(() -> "HBK screen effect fades",
					GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, 4 * Float.BYTES);
		}
		// Upload before opening any render pass; rotate buffers so prior GPU frames remain untouched.
		try (var mapped = effectUniforms.currentBuffer().map(false, true)) {
			mapped.data().order(ByteOrder.nativeOrder()).putFloat(scratchStrength).putFloat(distortionStrength).putFloat(desaturation).putFloat(lens);
		}
		rendering = true;
		try {
			chain.process(target, resources);
		} finally {
			rendering = false;
			effectUniforms.rotate();
		}
	}
}
