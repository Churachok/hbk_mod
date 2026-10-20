package dev.kirill.hbk.world;

/** Transient marker for the interval before vanilla advances a transformed chunk to FEATURES. */
public interface AnomalyChunkMarker {
	boolean hbk$anomalyDecorated();
	void hbk$markAnomalyDecorated();
}
