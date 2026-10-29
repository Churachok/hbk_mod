package dev.kirill.hbk.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class SkippingOwlRenderState extends LivingEntityRenderState {
	public boolean waitingForPlayer;
	public float idleAge;
	public float performanceAge;
}
