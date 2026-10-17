package dev.kirill.hbk.item;

import dev.kirill.hbk.block.JagermeisterCauldronBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A plain, stackable empty bottle; unlike empty beer bottles, it is not a weapon. */
public final class EmptyJagermeisterBottleItem extends Item {
	public EmptyJagermeisterBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return JagermeisterCauldronBlock.tryFillBottle(context);
	}
}
