package dev.kirill.hbk.mixin;

import dev.kirill.hbk.block.FracturedBlock;
import dev.kirill.hbk.world.FracturedMiningProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla multiplies current hardness by elapsed time, which would reset damage on every phase. */
@Mixin(ServerPlayerGameMode.class)
public abstract class FracturedMiningMixin {
	@Shadow protected ServerLevel level;
	@Shadow @Final protected ServerPlayer player;
	@Shadow private int gameTicks;
	@Shadow private int destroyProgressStart;
	@Shadow private BlockPos destroyPos;
	@Shadow private boolean isDestroyingBlock;
	@Shadow private int lastSentState;
	@Unique private final FracturedMiningProgress hbk$mining = new FracturedMiningProgress();
	@Unique private BlockPos hbk$target;
	@Unique private Action hbk$action;

	@Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
	private void hbk$beginAction(BlockPos pos, Action action, Direction direction, int height, int sequence, CallbackInfo ci) {
		hbk$action = action;
		if (action == Action.START_DESTROY_BLOCK || action == Action.ABORT_DESTROY_BLOCK) hbk$target = null;
	}

	@Inject(method = "handleBlockBreakAction", at = @At("TAIL"))
	private void hbk$rememberInitialDamage(BlockPos pos, Action action, Direction direction, int height, int sequence, CallbackInfo ci) {
		if (action != Action.START_DESTROY_BLOCK || !isDestroyingBlock || !pos.equals(destroyPos)) return;
		var state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof FracturedBlock)) return;
		hbk$target = pos.immutable();
		hbk$mining.start(gameTicks, state.getDestroyProgress(player, level, pos));
	}

	@Unique
	private float hbk$progress(BlockState state, BlockPos pos) {
		return hbk$mining.advance(gameTicks, state.getDestroyProgress(player, level, pos));
	}

	@Inject(method = "incrementDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void hbk$accumulateMaterialDamage(BlockState state, BlockPos pos, int start, CallbackInfoReturnable<Float> cir) {
		if (!(state.getBlock() instanceof FracturedBlock) || !pos.equals(hbk$target)) return;
		float progress = hbk$progress(state, pos);
		int stage = (int) (progress * 10);
		if (stage != lastSentState) {
			level.destroyBlockProgress(player.getId(), pos, stage);
			lastSentState = stage;
		}
		// A delayed acknowledgement may arrive as the block phases; complete only when material again.
		cir.setReturnValue(FracturedBlock.material(state) ? progress : Math.min(progress, 0.999f));
	}

	@Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"), require = 2)
	private float hbk$acknowledgeAccumulatedDamage(BlockState state, Player miner, BlockGetter world, BlockPos pos) {
		if (hbk$action == Action.STOP_DESTROY_BLOCK && pos.equals(hbk$target)
				&& state.getBlock() instanceof FracturedBlock) {
			float progress = hbk$progress(state, pos);
			// Vanilla multiplies the returned rate by this interval before checking its completion threshold.
			return FracturedBlock.material(state) ? progress / Math.max(1, gameTicks - destroyProgressStart + 1) : 0;
		}
		return state.getDestroyProgress(miner, world, pos);
	}

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void hbk$requireMaterial(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		var state = level.getBlockState(pos);
		if (!player.getAbilities().instabuild && state.getBlock() instanceof FracturedBlock && (!FracturedBlock.material(state)
				|| !dev.kirill.hbk.universe.UniverseMining.permits(level, state, player.getMainHandItem()))) {
			player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
			cir.setReturnValue(false);
		}
	}
}
