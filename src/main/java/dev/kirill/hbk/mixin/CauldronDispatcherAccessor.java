package dev.kirill.hbk.mixin;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Minecraft 26.2 keeps dispatcher registration package-private. */
@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronDispatcherAccessor {
	@Invoker("put")
	void hbk$put(Item item, CauldronInteraction interaction);
}
