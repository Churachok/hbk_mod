package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class RoflMushroomGeneration {
	public static final ResourceKey<PlacedFeature> PATCH = ResourceKey.create(
			Registries.PLACED_FEATURE, HbkMod.id("patch_rofl_mushroom"));

	private RoflMushroomGeneration() {}

	public static void register() {
		BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD).and(context ->
				!context.getBiomeKey().equals(ModWorldgen.RADIOACTIVE_WASTELAND)
				&& !context.getBiomeKey().equals(ModWorldgen.PAST_GORNOSLAVYANSK)),
				GenerationStep.Decoration.VEGETAL_DECORATION, PATCH);
	}
}
