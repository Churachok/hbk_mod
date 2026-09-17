package dev.kirill.hbk.entity;

import java.util.UUID;

public interface WorkerMobData {
	UUID hbk$getWorkerOwner();

	void hbk$setWorkerOwner(UUID owner);

	int hbk$getWorkerHunger();

	void hbk$setWorkerHunger(int ticks);
}
