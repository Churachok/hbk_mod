package dev.kirill.hbk.player;

public interface MechanicsPlayerData {
	int hbk$getRationTicks();

	void hbk$setRationTicks(int ticks);

	int hbk$getGooseTicks();

	void hbk$setGooseTicks(int ticks);

	int hbk$getSweetLifeTicks();

	void hbk$setSweetLifeTicks(int ticks);

	long hbk$getLastBuckwheatTick();

	void hbk$setLastBuckwheatTick(long tick);

	long hbk$getLastStewTick();

	void hbk$setLastStewTick(long tick);

	int hbk$getConsecutiveStew();

	void hbk$setConsecutiveStew(int count);

	int hbk$getProgenitorTicks();

	void hbk$setProgenitorTicks(int ticks);

	long hbk$getFoundingBlastReadyTick();

	void hbk$setFoundingBlastReadyTick(long tick);
}
