package dev.kirill.hbk.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** A temporary animal sent only to the player whose encounter created it. */
public final class SquirrelEntity extends PathfinderMob {
	public static final int LIFETIME_TICKS = 20 * 60;
	private UUID ownerUuid;

	public SquirrelEntity(EntityType<? extends SquirrelEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setSilent(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	public void setEncounterOwner(ServerPlayer player) {
		this.ownerUuid = player.getUUID();
	}

	@Override
	public boolean broadcastToPlayer(ServerPlayer player) {
		return player.getUUID().equals(this.ownerUuid) && super.broadcastToPlayer(player);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		if (this.level() instanceof ServerLevel level) {
			ServerPlayer owner = this.ownerUuid == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerUuid);
			if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level
					|| this.distanceToSqr(owner) > 32.0 * 32.0 || this.tickCount >= LIFETIME_TICKS) {
				this.discard();
				return;
			}
		}
		super.tick();
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
		// An invisible encounter must not move other players or animals.
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && !attacker.getUUID().equals(this.ownerUuid)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}
}
