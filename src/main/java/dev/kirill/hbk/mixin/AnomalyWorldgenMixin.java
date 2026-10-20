package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyTerrain;
import dev.kirill.hbk.world.RepeatedAnomaly;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class AnomalyWorldgenMixin {
	@Inject(method = "applyBiomeDecoration", at = @At("HEAD"), cancellable = true)
	private void hbk$repeatAnomalyChunk(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
		if (RepeatedAnomaly.decorate((ChunkGenerator) (Object) this, level, chunk, structures)) ci.cancel();
	}

	@Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
	private void hbk$foldAnomalyTerrain(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
		AnomalyTerrain.decorate(level, chunk);
	}
}
