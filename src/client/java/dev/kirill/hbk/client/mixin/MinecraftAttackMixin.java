package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns the normal attack click into a shot while the weapon is in the main hand. */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void hbk$fireAttackingMember(CallbackInfoReturnable<Boolean> cir) {
		Minecraft client = (Minecraft) (Object) this;
		if (client.player != null && ProgenitorTransformation.isActive(client.player)) {
			if (ClientPlayNetworking.canSend(ModNetworking.FoundingPenisAbilityPayload.TYPE)) {
				ClientPlayNetworking.send(new ModNetworking.FoundingPenisAbilityPayload(false));
			}
			cir.setReturnValue(false);
			return;
		}
		if (client.player == null || client.gameMode == null
				|| !ModItems.usesSpecialLeftClick(client.player.getMainHandItem())) {
			return;
		}

		if (!client.player.getCooldowns().isOnCooldown(client.player.getMainHandItem())
				&& ClientPlayNetworking.canSend(ModNetworking.FireAttackingMemberPayload.TYPE)) {
			ClientPlayNetworking.send(ModNetworking.FireAttackingMemberPayload.INSTANCE);
		}
		// Do not punch entities or mine blocks with the weapon: every LMB is a shot.
		cir.setReturnValue(false);
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void hbk$foundingPenisBlast(CallbackInfo ci) {
		Minecraft client = (Minecraft) (Object) this;
		if (client.player != null && ProgenitorTransformation.isActive(client.player)) {
			if (ClientPlayNetworking.canSend(ModNetworking.FoundingPenisAbilityPayload.TYPE)) {
				ClientPlayNetworking.send(new ModNetworking.FoundingPenisAbilityPayload(true));
			}
			ci.cancel();
		}
	}
}
