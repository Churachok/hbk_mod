package dev.kirill.hbk.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** A network anchor in the owner's loaded chunk, like the bus's distant visual.
 * The authoritative world position belongs to the saved GodEncounter, not this anchor. */
public final class GodEntity extends Entity {
	private static final EntityDataAccessor<Vector3fc> OFFSET = SynchedEntityData.defineId(GodEntity.class, EntityDataSerializers.VECTOR3);
	public GodEntity(EntityType<? extends GodEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}
	@Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(OFFSET, new Vector3f()); }
	public void updatePursuit(Vec3 anchor, Vec3 position) {
		setPos(anchor);
		Vec3 offset = position.subtract(anchor);
		entityData.set(OFFSET, new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
	}
	public Vec3 visualOffset() {
		var offset = entityData.get(OFFSET);
		return new Vec3(offset.x(), offset.y(), offset.z());
	}
	@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
	@Override public boolean isPickable() { return false; }
	@Override public boolean shouldBeSaved() { return false; }
	@Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
	@Override protected void addAdditionalSaveData(ValueOutput output) {}
}
