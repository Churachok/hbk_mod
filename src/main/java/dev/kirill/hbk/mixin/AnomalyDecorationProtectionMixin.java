package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyChunkMarker;
import dev.kirill.hbk.world.AnomalyDecorationGuard;
import dev.kirill.hbk.world.AnomalyZones;
import dev.kirill.hbk.world.RepeatedAnomaly;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Neighbouring decoration must not plant old-height trees in a void or alter an already copied tile. */
@Mixin(WorldGenRegion.class)
public abstract class AnomalyDecorationProtectionMixin implements AnomalyDecorationGuard {
	@Unique private long hbk$lastChunk = Long.MIN_VALUE;
	@Unique private AnomalyZones.Zone hbk$zone;

	@Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
	private void hbk$protectTransformedTerrain(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
		if (hbk$protectAnomalyWrite(pos)) cir.setReturnValue(false);
	}

	@Override
	public boolean hbk$protectAnomalyWrite(BlockPos pos) {
		var region = (WorldGenRegion) (Object) this;
		if (!region.getLevel().dimension().equals(Level.OVERWORLD)) return false;
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		long key = net.minecraft.world.level.ChunkPos.pack(cx, cz);
		if (key != hbk$lastChunk) {
			hbk$lastChunk = key;
			hbk$zone = AnomalyZones.forChunk(AnomalyZones.layoutSeed(region.getSeed()), cx * 16, cz * 16);
		}
		if (hbk$zone == null || hbk$zone.kind() < 5 || !hbk$zone.contains(pos.getX() + 0.5, pos.getZ() + 0.5)) return false;
		if (hbk$zone.kind() == 6) return !RepeatedAnomaly.capturing();
		if (!region.hasChunk(cx, cz)) return false;
		var chunk = region.getChunk(cx, cz, ChunkStatus.EMPTY, false);
		return ((AnomalyChunkMarker) chunk).hbk$anomalyDecorated() || chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES);
	}
}
