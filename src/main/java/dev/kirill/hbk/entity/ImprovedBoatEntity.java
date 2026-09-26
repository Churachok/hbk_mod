package dev.kirill.hbk.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * A longer, blast-furnace-powered boat. Vanilla boat physics remain responsible
 * for buoyancy, paddles and networking; the authoritative movement is extended
 * by 60 percent after each normal movement step.
 */
public final class ImprovedBoatEntity extends Boat {
	public static final double SPEED_MULTIPLIER = 1.6;

	public ImprovedBoatEntity(
			EntityType<? extends ImprovedBoatEntity> type,
			Level level,
			Supplier<Item> dropItem
	) {
		super(type, level, dropItem);
	}

	@Override
	public void tick() {
		double oldX = this.getX();
		double oldZ = this.getZ();
		super.tick();

		if (!this.isRemoved() && this.hasControllingPassenger() && this.isLocalInstanceAuthoritative()) {
			double bonus = SPEED_MULTIPLIER - 1.0;
			Vec3 extraMovement = new Vec3(
					(this.getX() - oldX) * bonus,
					0.0,
					(this.getZ() - oldZ) * bonus
			);
			if (extraMovement.horizontalDistanceSqr() > 1.0E-8) {
				this.move(MoverType.SELF, extraMovement);
			}
		}
	}

	@Override
	protected int getMaxPassengers() {
		return 1;
	}
}
