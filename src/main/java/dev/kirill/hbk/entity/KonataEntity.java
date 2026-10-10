package dev.kirill.hbk.entity;

import dev.kirill.hbk.player.KonataDailyClaim;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModGameRules;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class KonataEntity extends PathfinderMob {
	private static final float THEME_DISC_CHANCE = 0.001f;
	private static final int GOOD_GESTURE_DURATION_TICKS = 44;
	private static final EntityDataAccessor<Integer> GOOD_GESTURE_START_TICK = SynchedEntityData.defineId(
			KonataEntity.class, EntityDataSerializers.INT);
	private UUID goodGesturePlayerUuid;

	public KonataEntity(EntityType<? extends KonataEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(GOOD_GESTURE_START_TICK, -1);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return false;
	}

	@Override
	public int getAmbientSoundInterval() {
		return this.getType() == ModEntityTypes.KONATA ? 240 : super.getAmbientSoundInterval();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.getType() == ModEntityTypes.KONATA ? ModSounds.KONATA_AMBIENT : null;
	}

	@Override
	protected float getSoundVolume() {
		return this.getType() == ModEntityTypes.KONATA ? 0.8f : super.getSoundVolume();
	}

	public boolean isGoodGestureActive() {
		int startTick = this.entityData.get(GOOD_GESTURE_START_TICK);
		return startTick >= 0 && this.tickCount - startTick < GOOD_GESTURE_DURATION_TICKS;
	}

	public float getGoodGestureProgress(float tickProgress) {
		int startTick = this.entityData.get(GOOD_GESTURE_START_TICK);
		if (startTick < 0) {
			return 0.0f;
		}
		float age = this.tickCount + tickProgress - startTick;
		float fadeIn = Mth.clamp(age / 6.0f, 0.0f, 1.0f);
		float fadeOut = Mth.clamp((GOOD_GESTURE_DURATION_TICKS - age) / 6.0f, 0.0f, 1.0f);
		return Math.min(fadeIn, fadeOut);
	}

	public static ItemStack createGiftForRoll(float roll) {
		return new ItemStack(roll < THEME_DISC_CHANCE
				? ModItems.MUSIC_DISC_KONATA_THEME
				: ModItems.SHPERMA);
	}

	private void startGoodGesture(Player player) {
		this.entityData.set(GOOD_GESTURE_START_TICK, this.tickCount);
		this.goodGesturePlayerUuid = player.getUUID();
		this.getNavigation().stop();
		this.getLookControl().setLookAt(player, 180.0f, 180.0f);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.isGoodGestureActive()) {
			if (this.level() instanceof ServerLevel
					&& this.entityData.get(GOOD_GESTURE_START_TICK) >= 0) {
				this.entityData.set(GOOD_GESTURE_START_TICK, -1);
				this.goodGesturePlayerUuid = null;
			}
			return;
		}
		this.getNavigation().stop();
		var movement = this.getDeltaMovement();
		this.setDeltaMovement(0.0, movement.y, 0.0);
		if (this.level() instanceof ServerLevel serverLevel && this.goodGesturePlayerUuid != null) {
			Player player = serverLevel.getPlayerByUUID(this.goodGesturePlayerUuid);
			if (player != null && player.isAlive()) {
				this.getLookControl().setLookAt(player, 180.0f, 180.0f);
			}
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.getType() != ModEntityTypes.KONATA) {
			return super.mobInteract(player, hand);
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			int dailyGiftLimit = serverLevel.getGameRules().get(ModGameRules.KONATA_SHPERM_LIMIT);
			long currentDay = serverLevel.getOverworldClockTime() / 24_000L;
			KonataDailyClaim savedClaim = player.getAttachedOrCreate(ModAttachments.KONATA_DAILY_CLAIM);
			KonataDailyClaim todayClaim = savedClaim.day() == currentDay
					? savedClaim : new KonataDailyClaim(currentDay, 0);
			if (todayClaim.count() >= dailyGiftLimit) {
				player.sendOverlayMessage(Component.translatable("message.hbk.konata_daily_limit"));
				return InteractionResult.SUCCESS;
			}

			ItemStack gift = createGiftForRoll(serverLevel.getRandom().nextFloat());
			if (this.spawnAtLocation(serverLevel, gift, 0.25f) != null) {
				int newCount = todayClaim.count() + 1;
				player.setAttached(ModAttachments.KONATA_DAILY_CLAIM,
						new KonataDailyClaim(currentDay, newCount));
				if (newCount == dailyGiftLimit) {
					this.startGoodGesture(player);
					this.playSound(ModSounds.KONATA_GOOD, 1.0f, 1.0f);
				} else {
					this.playSound(ModSounds.KONATA_PUPUE, 1.4f, 1.0f);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}
