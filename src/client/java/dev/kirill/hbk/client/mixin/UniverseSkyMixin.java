package dev.kirill.hbk.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class UniverseSkyMixin {
	@Shadow private int starIndexCount;
	@Shadow private void renderSun(float brightness, PoseStack poses) { throw new AssertionError(); }
	@Shadow private void renderMoon(MoonPhase phase, float brightness, PoseStack poses) { throw new AssertionError(); }
	@Shadow private void renderStars(float brightness, PoseStack poses) { throw new AssertionError(); }
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void hbk$horizon(ClientLevel level, float partial, Camera camera, SkyRenderState state, CallbackInfo ci) {
		var p = UniverseProfile.of(level);
		// The vanilla below-horizon disc obscures the coloured sky around islands and hanging worlds.
		if (p != null) state.shouldRenderDarkDisc = false;
	}
	@Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
	private void hbk$parallelSky(PoseStack poses, float sun, float moon, float star, MoonPhase moonPhase,
			float brightness, float starBrightness, CallbackInfo ci) {
		var level = Minecraft.getInstance().level;
		var p = level == null ? null : UniverseProfile.of(level);
		if (p == null) return;
		ci.cancel();
		if (!p.openSky()) return;
		poses.pushPose();
		poses.mulPose(Axis.YP.rotationDegrees(-90));
		int count = p.get(HiddenParameter.CELESTIAL_COUNT);
		float size = new float[]{.04f, .12f, .3f, .65f, 1, 1.5f, 2.3f, 3.5f, 5, 7}[p.get(HiddenParameter.CELESTIAL_SIZE)];
		float speed = new float[]{0, .01f, .1f, .3f, .7f, 1, 2, 4, 8, 16}[p.get(HiddenParameter.CELESTIAL_SPEED)];
		float orbit = (float) ((level.getGameTime() / 24000.0 * Math.PI * 2 * speed) % (Math.PI * 2));
		for (int i = 0; i < count; i++) {
			poses.pushPose();
			poses.mulPose(Axis.ZP.rotationDegrees(i * 37 + p.geometry() * 11));
			poses.mulPose(Axis.XP.rotation(orbit + i * 2.399963f));
			poses.scale(size, 1, size);
			if (i % 2 == 0) renderSun(brightness, poses); else renderMoon(moonPhase, brightness, poses);
			poses.popPose();
		}
		int stars = p.get(HiddenParameter.STARS), saved = starIndexCount;
		try {
			starIndexCount = stars < 5 ? (saved / 6 * stars * stars / 25) * 6 : saved;
			if (starIndexCount > 0 && starBrightness > 0) for (int layer = 0; layer < (stars < 6 ? 1 : stars - 4); layer++) {
				poses.pushPose();
				poses.mulPose(Axis.XP.rotation(star + layer * .64f));
				poses.mulPose(Axis.ZP.rotation(layer * 1.35f));
				renderStars(starBrightness, poses);
				poses.popPose();
			}
		} finally { starIndexCount = saved; poses.popPose(); }
	}
}
