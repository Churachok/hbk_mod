package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.feature.RadioactiveCraterFeature;
import dev.kirill.hbk.world.feature.RadioactiveRuinFeature;
import dev.kirill.hbk.world.feature.WastelandSurfaceFeature;
import dev.kirill.hbk.world.feature.GornoslavyanskMonumentFeature;
import dev.kirill.hbk.world.feature.GornoslavyanskRuinFeature;
import dev.kirill.hbk.world.feature.GornoslavyanskSurfaceFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ModFeatures {
	public static final Feature<NoneFeatureConfiguration> WASTELAND_SURFACE = register(
			"wasteland_surface", new WastelandSurfaceFeature());
	public static final Feature<NoneFeatureConfiguration> RADIOACTIVE_CRATER = register(
			"radioactive_crater", new RadioactiveCraterFeature());
	public static final Feature<NoneFeatureConfiguration> RADIOACTIVE_RUIN = register(
			"radioactive_ruin", new RadioactiveRuinFeature());
	public static final Feature<NoneFeatureConfiguration> GORNOSLAVYANSK_SURFACE = register(
			"gornoslavyansk_surface", new GornoslavyanskSurfaceFeature());
	public static final Feature<NoneFeatureConfiguration> GORNOSLAVYANSK_RUIN = register(
			"gornoslavyansk_ruin", new GornoslavyanskRuinFeature());
	public static final Feature<NoneFeatureConfiguration> GORNOSLAVYANSK_MONUMENT = register(
			"gornoslavyansk_monument", new GornoslavyanskMonumentFeature());

	private ModFeatures() {
	}

	private static <T extends Feature<?>> T register(String name, T feature) {
		return Registry.register(BuiltInRegistries.FEATURE, HbkMod.id(name), feature);
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered custom biome features");
	}
}
