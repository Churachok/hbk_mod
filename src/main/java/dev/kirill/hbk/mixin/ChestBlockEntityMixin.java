package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.GroveStreetPopulation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {
	@Inject(method = "startOpen", at = @At("TAIL"))
	private void hbk$witnessChestOpening(ContainerUser user, CallbackInfo ci) {
		ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
		if (!chest.isRemoved() && chest.getLevel() instanceof ServerLevel level
				&& user.getLivingEntity() instanceof Player player) {
			GroveStreetPopulation.onChestOpened(level, chest.getBlockPos(), player);
		}
	}
}
