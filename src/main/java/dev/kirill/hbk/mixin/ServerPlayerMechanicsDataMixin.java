package dev.kirill.hbk.mixin;

import dev.kirill.hbk.player.MechanicsPlayerData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMechanicsDataMixin {
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void hbk$restoreMechanicsData(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		MechanicsPlayerData oldData = (MechanicsPlayerData) oldPlayer;
		MechanicsPlayerData newData = (MechanicsPlayerData) this;
		newData.hbk$setRationTicks(oldData.hbk$getRationTicks());
		newData.hbk$setGooseTicks(oldData.hbk$getGooseTicks());
		newData.hbk$setSweetLifeTicks(oldData.hbk$getSweetLifeTicks());
		newData.hbk$setLastBuckwheatTick(oldData.hbk$getLastBuckwheatTick());
		newData.hbk$setLastStewTick(oldData.hbk$getLastStewTick());
		newData.hbk$setConsecutiveStew(oldData.hbk$getConsecutiveStew());
		newData.hbk$setProgenitorTicks(alive ? oldData.hbk$getProgenitorTicks() : 0);
		newData.hbk$setFoundingBlastReadyTick(oldData.hbk$getFoundingBlastReadyTick());
	}
}
