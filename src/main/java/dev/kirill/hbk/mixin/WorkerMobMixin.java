package dev.kirill.hbk.mixin;

import dev.kirill.hbk.entity.WorkerMobData;
import dev.kirill.hbk.entity.WorkerMobLogic;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class WorkerMobMixin implements WorkerMobData {
	@Unique
	private UUID hbk$workerOwner;
	@Unique
	private int hbk$workerHunger;

	@Override
	public UUID hbk$getWorkerOwner() {
		return this.hbk$workerOwner;
	}

	@Override
	public void hbk$setWorkerOwner(UUID owner) {
		this.hbk$workerOwner = owner;
	}

	@Override
	public int hbk$getWorkerHunger() {
		return this.hbk$workerHunger;
	}

	@Override
	public void hbk$setWorkerHunger(int ticks) {
		this.hbk$workerHunger = ticks;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void hbk$tickWorker(CallbackInfo ci) {
		WorkerMobLogic.tick((Mob) (Object) this);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void hbk$saveWorker(ValueOutput output, CallbackInfo ci) {
		if (this.hbk$workerOwner != null) {
			output.putString("hbk_worker_owner", this.hbk$workerOwner.toString());
			output.putInt("hbk_worker_hunger", this.hbk$workerHunger);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void hbk$loadWorker(ValueInput input, CallbackInfo ci) {
		String owner = input.getStringOr("hbk_worker_owner", "");
		try {
			this.hbk$workerOwner = owner.isEmpty() ? null : UUID.fromString(owner);
		} catch (IllegalArgumentException ignored) {
			this.hbk$workerOwner = null;
		}
		this.hbk$workerHunger = input.getIntOr("hbk_worker_hunger", 0);
	}
}
