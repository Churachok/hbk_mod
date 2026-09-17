package dev.kirill.hbk.mixin;

import dev.kirill.hbk.player.MechanicsPlayerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPersistentDataMixin implements MechanicsPlayerData {
	@Unique
	private int hbk$rationTicks;
	@Unique
	private int hbk$gooseTicks;
	@Unique
	private int hbk$sweetLifeTicks;
	@Unique
	private long hbk$lastBuckwheatTick = Long.MIN_VALUE;
	@Unique
	private long hbk$lastStewTick = Long.MIN_VALUE;
	@Unique
	private int hbk$consecutiveStew;

	@Override
	public int hbk$getRationTicks() {
		return this.hbk$rationTicks;
	}

	@Override
	public void hbk$setRationTicks(int ticks) {
		this.hbk$rationTicks = ticks;
	}

	@Override
	public int hbk$getGooseTicks() {
		return this.hbk$gooseTicks;
	}

	@Override
	public void hbk$setGooseTicks(int ticks) {
		this.hbk$gooseTicks = ticks;
	}

	@Override
	public int hbk$getSweetLifeTicks() {
		return this.hbk$sweetLifeTicks;
	}

	@Override
	public void hbk$setSweetLifeTicks(int ticks) {
		this.hbk$sweetLifeTicks = ticks;
	}

	@Override
	public long hbk$getLastBuckwheatTick() {
		return this.hbk$lastBuckwheatTick;
	}

	@Override
	public void hbk$setLastBuckwheatTick(long tick) {
		this.hbk$lastBuckwheatTick = tick;
	}

	@Override
	public long hbk$getLastStewTick() {
		return this.hbk$lastStewTick;
	}

	@Override
	public void hbk$setLastStewTick(long tick) {
		this.hbk$lastStewTick = tick;
	}

	@Override
	public int hbk$getConsecutiveStew() {
		return this.hbk$consecutiveStew;
	}

	@Override
	public void hbk$setConsecutiveStew(int count) {
		this.hbk$consecutiveStew = count;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void hbk$saveMechanicData(ValueOutput output, CallbackInfo ci) {
		output.putInt("hbk_ration_ticks", this.hbk$rationTicks);
		output.putInt("hbk_goose_ticks", this.hbk$gooseTicks);
		output.putInt("hbk_sweet_life_ticks", this.hbk$sweetLifeTicks);
		output.putLong("hbk_last_buckwheat_tick", this.hbk$lastBuckwheatTick);
		output.putLong("hbk_last_stew_tick", this.hbk$lastStewTick);
		output.putInt("hbk_consecutive_stew", this.hbk$consecutiveStew);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void hbk$loadMechanicData(ValueInput input, CallbackInfo ci) {
		this.hbk$rationTicks = input.getIntOr("hbk_ration_ticks", 0);
		this.hbk$gooseTicks = input.getIntOr("hbk_goose_ticks", 0);
		this.hbk$sweetLifeTicks = input.getIntOr("hbk_sweet_life_ticks", 0);
		this.hbk$lastBuckwheatTick = input.getLongOr("hbk_last_buckwheat_tick", Long.MIN_VALUE);
		this.hbk$lastStewTick = input.getLongOr("hbk_last_stew_tick", Long.MIN_VALUE);
		this.hbk$consecutiveStew = input.getIntOr("hbk_consecutive_stew", 0);
	}
}
