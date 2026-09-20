package dev.kirill.hbk.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/** Marks Sasha as hostile for vanilla targeting and the mod's other mechanics. */
public final class SashaEntity extends ReferenceNpcEntity implements Enemy {
	public SashaEntity(EntityType<? extends SashaEntity> type, Level level) {
		super(type, level);
	}
}
