package dev.kirill.hbk.mixin;

import dev.kirill.hbk.block.RoflBlock;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRoflBounceMixin {
	@Inject(method = "move", at = @At("TAIL"))
	private void hbk$bounceFromRofl(MoverType type, Vec3 movement, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		// Run after vanilla collision restitution, which otherwise overwrites the upward impulse.
		if (entity.onGround() && !entity.isSuppressingBounce() && !entity.isPassenger()
				&& entity.level().getBlockState(entity.getOnPos()).is(ModBlocks.ROFL_BLOCK)) {
			RoflBlock.launch(entity);
		}
	}

	@Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
	private void hbk$safeRoflLanding(double movementY, boolean grounded, BlockState state, BlockPos pos, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (Boolean.TRUE.equals(entity.getAttached(ModAttachments.ROFL_SAFE_FALL))) {
			entity.resetFallDistance();
			// Remove only upon landing: ceilings and leaving the launch column must remain safe.
			if (grounded || entity.isInWater() || entity.isPassenger()) {
				entity.removeAttached(ModAttachments.ROFL_SAFE_FALL);
			}
			ci.cancel();
		}
	}
}
