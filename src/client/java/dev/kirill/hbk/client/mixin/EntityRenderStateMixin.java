package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.FunnySpinRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements FunnySpinRenderState {
	@Unique private boolean hbk$funnySpinning;

	@Override
	public boolean hbk$isFunnySpinning() {
		return hbk$funnySpinning;
	}

	@Override
	public void hbk$setFunnySpinning(boolean spinning) {
		hbk$funnySpinning = spinning;
	}
}
