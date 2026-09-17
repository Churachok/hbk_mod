package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.entity.FlyingCarpetEntity;
import dev.kirill.hbk.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void hbk$invertCurrantControls(CallbackInfo ci) {
		var player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (player.hasEffect(ModEffects.DROWSINESS)) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
			this.hbk$sendCarpetInput(player);
			return;
		}
		if (player.hasEffect(ModEffects.HAND_IMMORTALITY)) {
			Input original = this.keyPresses;
			this.keyPresses = new Input(
					original.backward(),
					original.forward(),
					original.right(),
					original.left(),
					original.shift(),
					original.jump(),
					original.sprint()
			);
			this.moveVector = this.moveVector.negated();
		}
		this.hbk$sendCarpetInput(player);
	}

	private void hbk$sendCarpetInput(net.minecraft.client.player.LocalPlayer player) {
		if (!(player.getVehicle() instanceof FlyingCarpetEntity carpet)) {
			return;
		}
		Input input = this.keyPresses;
		carpet.setControls(player, input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.sprint());
		ClientPlayNetworking.send(new ModNetworking.CarpetInputPayload(
				input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.sprint()
		));
	}
}
