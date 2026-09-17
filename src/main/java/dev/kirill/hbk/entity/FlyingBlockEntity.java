package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class FlyingBlockEntity extends FallingBlockEntity {
	public static final int LIFETIME_TICKS = 20 * 60 * 5;
	private static final EntityDataAccessor<BlockState> FLYING_BLOCK_STATE = SynchedEntityData.defineId(
			FlyingBlockEntity.class,
			EntityDataSerializers.BLOCK_STATE
	);

	private UUID ownerUuid;

	public FlyingBlockEntity(EntityType<? extends FlyingBlockEntity> type, Level level) {
		super(type, level);
		this.disableDrop();
		this.setNoGravity(true);
		this.noPhysics = true;
	}

	public FlyingBlockEntity(ServerLevel level, BlockState state, Player owner, BlockPos sourcePos) {
		this(ModEntityTypes.FLYING_BLOCK, level);
		this.entityData.set(FLYING_BLOCK_STATE, state);
		this.ownerUuid = owner.getUUID();
		this.setStartPos(sourcePos);
		this.setPos(sourcePos.getX() + 0.5, sourcePos.getY() + 0.25, sourcePos.getZ() + 0.5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FLYING_BLOCK_STATE, Blocks.STONE.defaultBlockState());
	}

	@Override
	public BlockState getBlockState() {
		return this.entityData.get(FLYING_BLOCK_STATE);
	}

	@Override
	public void tick() {
		this.baseTick();
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.tickCount >= LIFETIME_TICKS) {
			serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
			this.discard();
			return;
		}

		Entity owner = this.ownerUuid == null ? null : serverLevel.getEntityInAnyDimension(this.ownerUuid);
		if (!(owner instanceof Player player) || !player.isAlive()) {
			return;
		}

		double angle = (this.tickCount * 0.055) + (this.getId() % 12) * 0.52;
		Vec3 target = player.position().add(Math.cos(angle) * 1.6, 1.25 + Math.sin(angle * 0.7) * 0.3, Math.sin(angle) * 1.6);
		Vec3 movement = target.subtract(this.position()).scale(0.18);
		if (movement.lengthSqr() > 1.0) {
			movement = movement.normalize();
		}
		this.setDeltaMovement(movement);
		this.setPos(this.position().add(movement));
		this.setYRot((this.getYRot() + 7.0f) % 360.0f);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("flying_block_state", BlockState.CODEC, this.getBlockState());
		if (this.ownerUuid != null) {
			output.putString("owner_uuid", this.ownerUuid.toString());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(FLYING_BLOCK_STATE, input.read("flying_block_state", BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState()));
		String owner = input.getStringOr("owner_uuid", "");
		try {
			this.ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
		} catch (IllegalArgumentException ignored) {
			this.ownerUuid = null;
		}
	}
}
