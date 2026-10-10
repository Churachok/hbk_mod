package dev.kirill.hbk.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class SquirrelRenderState extends LivingEntityRenderState {
	public float uprightAmount;
	public float giveProgress;
	public final ItemStackRenderState beer = new ItemStackRenderState();
}
