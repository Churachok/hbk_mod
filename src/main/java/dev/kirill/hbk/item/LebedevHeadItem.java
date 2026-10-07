package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class LebedevHeadItem extends Item {
	public LebedevHeadItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var player = context.getPlayer();
		if (player == null || player.isSpectator()) {
			return InteractionResult.FAIL;
		}
		var pos = context.getClickedPos().relative(context.getClickedFace());
		if (!player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())
				|| !context.getLevel().mayInteract(player, pos)) {
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		var head = ModEntityTypes.LEBEDEV_HEAD.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (head == null) {
			return InteractionResult.FAIL;
		}
		head.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		head.setYRot(player.getYRot() + 180.0f);
		if (!level.noCollision(head) || !level.addFreshEntity(head)) {
			return InteractionResult.FAIL;
		}
		if (!player.isCreative()) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
