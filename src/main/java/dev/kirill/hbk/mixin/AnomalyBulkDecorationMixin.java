package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyDecorationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** OreFeature treats a missing section as unwritable; it must not bypass the worldgen region guard. */
@Mixin(BulkSectionAccess.class)
public abstract class AnomalyBulkDecorationMixin {
	@Shadow @Final private LevelAccessor level;

	@Inject(method = "getSection", at = @At("HEAD"), cancellable = true)
	private void hbk$protectCopiedOres(BlockPos pos, CallbackInfoReturnable<LevelChunkSection> cir) {
		if (level instanceof WorldGenRegion region && ((AnomalyDecorationGuard) region).hbk$protectAnomalyWrite(pos)) {
			cir.setReturnValue(null);
		}
	}
}
