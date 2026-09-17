package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class FlyingCarpetEntity extends Entity {
	private boolean returnsItem = true;
	private int emptyTicks;
	private boolean forward;
	private boolean backward;
	private boolean left;
	private boolean right;
	private boolean jump;
	private boolean sprint;

	public FlyingCarpetEntity(EntityType<? extends FlyingCarpetEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.blocksBuilding = true;
	}

	public FlyingCarpetEntity(ServerLevel level, Player owner, boolean returnsItem) {
		this(ModEntityTypes.FLYING_CARPET, level);
		this.returnsItem = returnsItem;
		this.snapTo(owner.getX(), owner.getY() - 0.2, owner.getZ(), owner.getYRot(), 0.0f);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		Entity passenger = this.getFirstPassenger();
		if (!(passenger instanceof Player player)) {
			this.clearControls();
			if (this.level() instanceof ServerLevel level && ++this.emptyTicks > 20) {
				this.returnItem(level);
			}
			return;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			Input input = serverPlayer.getLastClientInput();
			this.updateControls(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.sprint());
		}
		this.emptyTicks = 0;
		this.setYRot(player.getYRot());
		player.fallDistance = 0.0;
		double forwardInput = (this.forward ? 1.0 : 0.0) - (this.backward ? 1.0 : 0.0);
		double sideInput = (this.right ? 1.0 : 0.0) - (this.left ? 1.0 : 0.0);
		double speed = this.sprint ? 0.62 : 0.42;
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0, look.z);
		if (forward.lengthSqr() < 0.0001) {
			forward = new Vec3(0.0, 0.0, 1.0);
		} else {
			forward = forward.normalize();
		}
		Vec3 movement = forward.scale(forwardInput * speed)
				.add(-forward.z * sideInput * speed, this.jump ? 0.36 : forwardInput * look.y * 0.34, forward.x * sideInput * speed);
		this.setDeltaMovement(movement);
		this.move(MoverType.SELF, movement);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	public void setControls(Player player, boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint) {
		if (this.getFirstPassenger() != player) {
			return;
		}
		this.updateControls(forward, backward, left, right, jump, sprint);
	}

	private void updateControls(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint) {
		this.forward = forward;
		this.backward = backward;
		this.left = left;
		this.right = right;
		this.jump = jump;
		this.sprint = sprint;
	}

	private void clearControls() {
		this.forward = false;
		this.backward = false;
		this.left = false;
		this.right = false;
		this.jump = false;
		this.sprint = false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty() && passenger instanceof Player;
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return this.position().add(0.0, 0.28, 0.0);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
		if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
			player.startRiding(this, true, false);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.isRemoved()) {
			return false;
		}
		this.ejectPassengers();
		this.returnItem(level);
		return true;
	}

	private void returnItem(ServerLevel level) {
		if (this.returnsItem) {
			this.spawnAtLocation(level, new ItemStack(ModItems.FLYING_CARPET));
			this.returnsItem = false;
		}
		this.discard();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.returnsItem = input.getBooleanOr("returns_item", true);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean("returns_item", this.returnsItem);
	}
}
