package dev.kirill.hbk.world;

import net.minecraft.core.BlockPos;

/** Shared by ordinary worldgen writes and the ore generator's direct section access. */
public interface AnomalyDecorationGuard {
	boolean hbk$protectAnomalyWrite(BlockPos pos);
}
