package dev.kirill.hbk.client;

/** Snapshots the real avatar's sprint state for deferred model submission, including remote players. */
public interface FurryPlayerRenderState {
	boolean hbk$isFurrySprinting();

	void hbk$setFurrySprinting(boolean sprinting);
}
