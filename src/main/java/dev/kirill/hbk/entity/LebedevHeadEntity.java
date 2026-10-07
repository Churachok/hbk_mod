package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/** A rigid head using vanilla's hay-filled sulfur cube push and kick physics. */
public final class LebedevHeadEntity extends SulfurCube {
	public static final float HEAD_SIZE = 0.8f;
	private float roll;
	private float oldRoll;

	public LebedevHeadEntity(EntityType<? extends LebedevHeadEntity> type, Level level) {
		super(type, level);
		this.setSize(1, false);
		this.setAgeLocked(true);
		this.setPersistenceRequired();
		this.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.HAY_BLOCK));
		this.setDropChance(EquipmentSlot.BODY, 0.0f);
	}

	@Override
	protected void registerGoals() {
		// An inert trophy: no wandering, jumping, feeding or reproduction goals.
	}

	@Override
	public ParticleOptions getParticleType() {
		return new DustParticleOptions(0xB51B24, 1.0f);
	}

	@Override
	public void tick() {
		double beforeX = this.getX();
		double beforeZ = this.getZ();
		this.oldRoll = this.roll;
		super.tick();
		double dx = this.getX() - beforeX;
		double dz = this.getZ() - beforeZ;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance > 0.001) {
			float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
			this.setYRot(yaw);
			this.setYHeadRot(yaw);
			this.setYBodyRot(yaw);
			this.roll += (float) (distance * 360.0 / (Math.PI * HEAD_SIZE));
		}
	}

	public float getRoll(float partialTick) {
		return Mth.lerp(partialTick, this.oldRoll, this.roll);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit && source.getEntity() instanceof Player) {
			level.sendParticles((DustParticleOptions) this.getParticleType(),
					this.getX(), this.getY() + 0.4, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
		}
		return hit;
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return EntityDimensions.scalable(HEAD_SIZE, HEAD_SIZE);
	}

	@Override
	protected int getSplitCount() {
		return 0;
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean readyForShearing() {
		return false;
	}

	@Override
	public boolean canBePickedUpWithBucket(ItemStack stack) {
		return false;
	}

	@Override
	public boolean equipItem(ItemStack stack) {
		return false;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}
		if (this.level() instanceof ServerLevel level) {
			ItemStack head = new ItemStack(ModItems.LEBEDEV_HEAD);
			if (!player.getInventory().add(head)) {
				player.drop(head, false);
			}
			this.discard();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		this.setDropChance(EquipmentSlot.BODY, 0.0f);
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setAge(0);
		this.setAgeLocked(true);
		this.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.HAY_BLOCK));
		this.setDropChance(EquipmentSlot.BODY, 0.0f);
	}
}
