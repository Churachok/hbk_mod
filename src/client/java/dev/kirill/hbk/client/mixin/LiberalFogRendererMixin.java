package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.YoungLiberalClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class LiberalFogRendererMixin {
	@Inject(method = "computeFogColor", at = @At("RETURN"))
	private void hbk$matchSkyHorizon(Camera camera, float partialTick, ClientLevel level,
			int renderDistance, float darkness, Vector4f color, CallbackInfo ci) {
		if (YoungLiberalClient.isActive() && level.dimensionType().skybox() == DimensionType.Skybox.OVERWORLD
				&& camera.getFluidInCamera() == FogType.NONE) {
			float angle = camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partialTick);
			color.set(ARGB.vector4fFromARGB32(YoungLiberalClient.skyColor((float) Math.toRadians(angle))));
		}
	}
}
