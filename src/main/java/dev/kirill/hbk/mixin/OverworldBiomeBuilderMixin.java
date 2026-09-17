package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reserves a few rare positive-weirdness climate slots for the two wastelands. */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
	@Shadow @Final private ResourceKey<Biome>[][] MIDDLE_BIOMES_VARIANT;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void hbk$addCustomBiomes(CallbackInfo ci) {
		MIDDLE_BIOMES_VARIANT[4][0] = ModWorldgen.RADIOACTIVE_WASTELAND;

		// These remain variant-only slots, so the biome forms rare islands. Across the
		// complete vanilla parameter map their combined volume is ~10% above all three
		// Badlands variants (the terracotta biomes), matching the intended rarity.
		MIDDLE_BIOMES_VARIANT[3][0] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[4][1] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[0][4] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[2][4] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[4][4] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[1][0] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[0][1] = ModWorldgen.PAST_GORNOSLAVYANSK;
		MIDDLE_BIOMES_VARIANT[0][3] = ModWorldgen.PAST_GORNOSLAVYANSK;
	}
}
