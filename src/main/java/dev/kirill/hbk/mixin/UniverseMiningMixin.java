package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.UniverseMining;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class UniverseMiningMixin {
	@Inject(method = "hasCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
	private void hbk$oreTier(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		Player player = (Player) (Object) this;
		if (!UniverseMining.permits(player.level(), state, player.getMainHandItem())) cir.setReturnValue(false);
	}
}
