package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyChunkMarker;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public abstract class AnomalyChunkMarkerMixin implements AnomalyChunkMarker {
	@Unique private volatile boolean hbk$anomalyDecorated;
	@Override public boolean hbk$anomalyDecorated() { return hbk$anomalyDecorated; }
	@Override public void hbk$markAnomalyDecorated() { hbk$anomalyDecorated = true; }
}
