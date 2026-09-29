package dev.kirill.hbk.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** The Unknown: a silent silhouette used by the fatal encounter. */
public final class KirillSecondEntity extends KirillEntity {
	public static final String ENCOUNTER_TAG_PREFIX = "hbk_unknown_encounter_";
	private static final int MAX_ENCOUNTER_LIFETIME_TICKS = 700;
	private static final EntityDataAccessor<Integer> GRAB_START_TICK = SynchedEntityData.defineId(
			KirillSecondEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> PORTAL_HIDDEN = SynchedEntityData.defineId(
			KirillSecondEntity.class, EntityDataSerializers.BOOLEAN);
	private boolean activeEncounterEntity;

	public KirillSecondEntity(EntityType<? extends KirillSecondEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return KirillEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 200.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(GRAB_START_TICK, -1);
		builder.define(PORTAL_HIDDEN, false);
	}

	public void startGrabbing() {
		if (!this.isGrabbing()) {
			this.entityData.set(GRAB_START_TICK, this.tickCount);
		}
	}

	public boolean isGrabbing() {
		return this.entityData.get(GRAB_START_TICK) >= 0;
	}

	public void stopGrabbing() {
		this.entityData.set(GRAB_START_TICK, -1);
	}

	public float getGrabAnimationProgress(float tickProgress) {
		int startTick = this.entityData.get(GRAB_START_TICK);
		return startTick < 0 ? 0.0f : Mth.clamp((this.tickCount + tickProgress - startTick) / 10.0f, 0.0f, 1.0f);
	}

	public void markEncounterEntity(UUID playerUuid) {
		this.activeEncounterEntity = true;
		this.addTag(encounterTag(playerUuid));
	}

	public static String encounterTag(UUID playerUuid) {
		return ENCOUNTER_TAG_PREFIX + playerUuid;
	}

	public void setPortalHidden(boolean hidden) {
		this.entityData.set(PORTAL_HIDDEN, hidden);
		this.setInvisible(hidden);
	}

	public boolean isPortalHidden() {
		return this.entityData.get(PORTAL_HIDDEN);
	}

	@Override
	public void tick() {
		if (this.level() instanceof ServerLevel) {
			boolean taggedEncounter = this.entityTags().stream()
					.anyMatch(tag -> tag.startsWith(ENCOUNTER_TAG_PREFIX));
			boolean legacyEncounter = !taggedEncounter && this.isNoAi()
					&& this.isInvulnerable() && this.isNoGravity();
			if (!this.activeEncounterEntity && (taggedEncounter || legacyEncounter)
					|| this.activeEncounterEntity && this.tickCount >= MAX_ENCOUNTER_LIFETIME_TICKS) {
				this.discard();
				return;
			}
		}
		if (this.isPortalHidden()) {
			this.setInvisible(true);
		}
		if (this.isGrabbing()) {
			this.setNoGravity(true);
			this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			this.fallDistance = 0.0;
		}
		super.tick();
		if (this.isPortalHidden()) {
			this.setInvisible(true);
		}
		if (this.isGrabbing()) {
			this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			this.fallDistance = 0.0;
		}
	}

	@Override
	public boolean shouldBeSaved() {
		boolean taggedEncounter = this.entityTags().stream()
				.anyMatch(tag -> tag.startsWith(ENCOUNTER_TAG_PREFIX));
		boolean legacyEncounter = !taggedEncounter && this.isNoAi()
				&& this.isInvulnerable() && this.isNoGravity();
		return !this.activeEncounterEntity && !taggedEncounter && !legacyEncounter && super.shouldBeSaved();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SCULK_SHRIEKER_SHRIEK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	protected void playRangedAttackSound(ServerLevel level) {
	}

	@Override
	public void playSound(SoundEvent sound, float volume, float pitch) {
		if (sound == SoundEvents.SCULK_SHRIEKER_SHRIEK) {
			super.playSound(sound, volume, pitch);
		}
	}
}
