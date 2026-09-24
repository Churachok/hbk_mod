package dev.kirill.hbk.entity;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/** Shared boss-bar, advancement, and restrained terrain-destruction behavior. */
public abstract class HumanoidBossEntity extends Monster {
	private final ServerBossEvent bossEvent;

	protected HumanoidBossEntity(EntityType<? extends HumanoidBossEntity> type, Level level,
			BossEvent.BossBarColor color) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 250;
		this.bossEvent = new ServerBossEvent(this.getUUID(), this.getDisplayName(), color,
				BossEvent.BossBarOverlay.PROGRESS);
		this.bossEvent.setPlayBossMusic(true);
		this.bossEvent.setCreateWorldFog(true);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public void setCustomName(Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		this.bossEvent.removeAllPlayers();
	}

	protected void awardKiller(DamageSource source, String advancementName) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		var advancement = player.level().getServer().getAdvancements().get(HbkMod.id(advancementName));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "kill");
		}
	}

	protected void breakWeakBlocks(ServerLevel level, int horizontalRadius, int verticalRadius,
			float maximumHardness) {
		this.breakWeakBlocks(level, horizontalRadius, -verticalRadius, verticalRadius, maximumHardness);
	}

	protected void breakWeakBlocks(ServerLevel level, int horizontalRadius, int minimumYOffset,
			int maximumYOffset, float maximumHardness) {
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		BlockPos center = this.blockPosition();
		for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
			for (int y = minimumYOffset; y <= maximumYOffset; y++) {
				for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
					BlockPos pos = center.offset(x, y, z);
					BlockState state = level.getBlockState(pos);
					float hardness = state.getDestroySpeed(level, pos);
					if (!state.isAir() && hardness >= 0.0f && hardness <= maximumHardness) {
						level.destroyBlock(pos, true, this, 512);
					}
				}
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.1f, 0.65f);
	}

	@Override
	public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}
}
