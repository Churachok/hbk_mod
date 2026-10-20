package dev.kirill.hbk.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AlloyFurnaceAccess {
	@Accessor("litTimeRemaining") int hbk$getHeat();
	@Accessor("litTimeRemaining") void hbk$setHeat(int value);
	@Accessor("litTotalTime") int hbk$getHeatTotal();
	@Accessor("litTotalTime") void hbk$setHeatTotal(int value);
}
