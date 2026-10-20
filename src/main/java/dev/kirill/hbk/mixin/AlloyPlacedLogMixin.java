package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.AlloyWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class AlloyPlacedLogMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void hbk$rememberBuiltLogs(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> result) {
		if (result.getReturnValue().consumesAction() && context.getLevel() instanceof ServerLevel level
				&& level.getBlockState(context.getClickedPos()).is(BlockTags.LOGS)) AlloyWorldData.get(level).markLog(context.getClickedPos());
	}
}
