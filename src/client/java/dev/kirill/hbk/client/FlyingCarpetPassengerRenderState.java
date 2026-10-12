package dev.kirill.hbk.client;

/** The passenger's pose is extracted per entity, including players observed by other clients. */
public interface FlyingCarpetPassengerRenderState {
	boolean hbk$isOnFlyingCarpet();
	void hbk$setOnFlyingCarpet(boolean onCarpet);
}
