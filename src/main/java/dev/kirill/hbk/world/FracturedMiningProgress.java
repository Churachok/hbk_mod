package dev.kirill.hbk.world;

/** A per-attempt accumulator: time spent in non-material phases never becomes retroactive damage. */
public final class FracturedMiningProgress {
	private float progress;
	private int lastTick;

	public void start(int tick, float initial) {
		progress = initial;
		lastTick = tick;
	}

	public float advance(int tick, float rate) {
		if (tick > lastTick) {
			progress += (tick - lastTick) * rate;
			lastTick = tick;
		}
		return progress;
	}
}
