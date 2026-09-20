package dev.kirill.hbk.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.Level;

/** The house cat survives every damage source, including the void and /kill. */
public final class LexEntity extends Cat {
	public LexEntity(EntityType<? extends LexEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.setInvulnerable(true);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public void kill(ServerLevel level) {
	}

	@Override
	public void die(DamageSource source) {
	}

	@Override
	public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
		return false;
	}
}
