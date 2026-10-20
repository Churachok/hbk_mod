package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.mechanic.FurryBreeding;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Apply the common voluntary-motion constraint exactly once, before native local collision handling. */
@Mixin(LocalPlayer.class)
public abstract class FurryLoveMovementMixin {
	@ModifyArg(method = "move", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), index = 1)
	private Vec3 hbk$constrainLoveMotion(MoverType type, Vec3 requested) {
		var level = Minecraft.getInstance().level;
		return type == MoverType.SELF && level != null
				? FurryBreeding.constrainLoveMovement((LocalPlayer) (Object) this, requested, level.entitiesForRendering()) : requested;
	}
}
