package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModWorldgen {
	public static final ResourceKey<Biome> RADIOACTIVE_WASTELAND = ResourceKey.create(
			Registries.BIOME, HbkMod.id("radioactive_wasteland"));
	public static final ResourceKey<Biome> PAST_GORNOSLAVYANSK = ResourceKey.create(
			Registries.BIOME, HbkMod.id("past_gornoslavyansk"));

	private ModWorldgen() {
	}
}
