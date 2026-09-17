package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class StalinEntity extends GiantBossEntity {
	public static final float EXPLOSION_POWER = 8.0f;
	public static final double MAX_HEALTH = 400.0;
	public static final double ATTACK_DAMAGE = 16.0;
	private static final float HEALTH_STEP = (float) (MAX_HEALTH * 0.10);
	private static final int NKVD_SQUAD_SIZE = 5;
	private static final int DEATH_NKVD_COUNT = 30;
	private static final float DEATH_EXPLOSION_POWER = 35.0f;
	private static final int SILHOUETTE_RAGE_TICKS = 20 * 30;

	private float nextNkvdHealthThreshold = (float) (MAX_HEALTH - HEALTH_STEP);
	private int silhouetteSummonCooldown;
	private int silhouetteRageTicks;

	public StalinEntity(EntityType<? extends StalinEntity> type, Level level) {
		super(type, level, BossEvent.BossBarColor.RED);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseAttributes(MAX_HEALTH, ATTACK_DAMAGE);
	}

	@Override
	public float getExplosionPower() {
		return EXPLOSION_POWER;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.silhouetteSummonCooldown > 0) {
			this.silhouetteSummonCooldown--;
		}
		if (this.silhouetteRageTicks > 0) {
			this.silhouetteRageTicks--;
		}

		ServerPlayer silhouette = level.players().stream()
				.filter(player -> player.isAlive() && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WESTERN_CHESTPLATE))
				.filter(player -> this.distanceToSqr(player) <= 32.0 * 32.0)
				.filter(player -> this.getSensing().hasLineOfSight(player))
				.min(java.util.Comparator.comparingDouble(this::distanceToSqr))
				.orElse(null);
		if (silhouette == null) {
			if (this.silhouetteRageTicks > 0 && this.tickCount % 20 == 0) {
				this.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false));
				this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1, false, false));
			}
			return;
		}

		this.silhouetteRageTicks = SILHOUETTE_RAGE_TICKS;
		this.setTarget(silhouette);
		if (this.tickCount % 20 == 0) {
			this.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false));
			this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1, false, false));
		}
		if (this.silhouetteSummonCooldown <= 0) {
			NkvdSpawning.spawnSquad(level, this.blockPosition(), NKVD_SQUAD_SIZE);
			this.playSound(SoundEvents.RAID_HORN.value(), 3.0f, 0.65f);
			this.silhouetteSummonCooldown = SILHOUETTE_RAGE_TICKS;
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		float healthBefore = this.getHealth();
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && this.isAlive() && healthBefore > this.getHealth()) {
			this.trySpawnNkvdOnThreshold(level);
		}
		return hurt;
	}

	private void trySpawnNkvdOnThreshold(ServerLevel level) {
		while (this.isAlive() && this.getHealth() <= this.nextNkvdHealthThreshold && this.nextNkvdHealthThreshold > 0.0f) {
			this.nextNkvdHealthThreshold -= HEALTH_STEP;
			NkvdSpawning.spawnSquad(level, this.blockPosition(), NKVD_SQUAD_SIZE);
		}
	}

	@Override
	public void die(DamageSource source) {
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.explode(
					this,
					this.getX(),
					this.getY() + this.getBbHeight() * 0.5,
					this.getZ(),
					DEATH_EXPLOSION_POWER,
					true,
					Level.ExplosionInteraction.TNT
			);
			NkvdSpawning.spawnSquad(serverLevel, this.blockPosition(), DEATH_NKVD_COUNT);
		}
		super.die(source);
	}

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
		boolean killed = super.killedEntity(level, entity, source);
		if (entity instanceof Villager) {
			this.heal(100.0f);
			this.playSound(SoundEvents.PLAYER_LEVELUP, 2.0f, 0.6f);
		}
		return killed;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("nkvd_health_threshold", this.nextNkvdHealthThreshold);
		output.putInt("silhouette_summon_cooldown", this.silhouetteSummonCooldown);
		output.putInt("silhouette_rage_ticks", this.silhouetteRageTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.nextNkvdHealthThreshold = input.getFloatOr("nkvd_health_threshold", (float) (MAX_HEALTH - HEALTH_STEP));
		this.silhouetteSummonCooldown = input.getIntOr("silhouette_summon_cooldown", 0);
		this.silhouetteRageTicks = input.getIntOr("silhouette_rage_ticks", 0);
	}
}
