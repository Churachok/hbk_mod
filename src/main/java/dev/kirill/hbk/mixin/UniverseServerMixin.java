package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.UniverseManager;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class UniverseServerMixin {
	@Inject(method = "getAllLevels", at = @At("RETURN"), cancellable = true)
	private void hbk$stableIteration(CallbackInfoReturnable<Iterable<ServerLevel>> cir) {
		cir.setReturnValue(java.util.List.copyOf(((UniverseServerAccess) this).hbk$levels().values()));
	}

	/** Also handles a player logging in/respawning into a saved, currently unloaded universe. */
	@Inject(method = "getLevel", at = @At("RETURN"), cancellable = true)
	private void hbk$loadAddress(ResourceKey<Level> key, CallbackInfoReturnable<ServerLevel> cir) {
		if (cir.getReturnValue() == null && UniverseProfile.of(key) != null) {
			cir.setReturnValue(UniverseManager.load((MinecraftServer) (Object) this, UniverseProfile.of(key).code()));
		}
	}
}
