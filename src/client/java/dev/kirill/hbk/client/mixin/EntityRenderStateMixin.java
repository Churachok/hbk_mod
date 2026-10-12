package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.FunnySpinRenderState;
import dev.kirill.hbk.client.FlyingCarpetPassengerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements FunnySpinRenderState, FlyingCarpetPassengerRenderState {
	@Unique private boolean hbk$funnySpinning;
	@Unique private boolean hbk$onFlyingCarpet;

	@Override
	public boolean hbk$isOnFlyingCarpet() {
		return this.hbk$onFlyingCarpet;
	}

	@Override
	public void hbk$setOnFlyingCarpet(boolean onCarpet) {
		this.hbk$onFlyingCarpet = onCarpet;
	}

	@Override
	public boolean hbk$isFunnySpinning() {
		return hbk$funnySpinning;
	}

	@Override
	public void hbk$setFunnySpinning(boolean spinning) {
		hbk$funnySpinning = spinning;
	}
}
