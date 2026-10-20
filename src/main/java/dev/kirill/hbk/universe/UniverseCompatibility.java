package dev.kirill.hbk.universe;

/** Effective rules are derived separately; the original five digits and 36 selections stay intact. */
public record UniverseCompatibility(boolean visibleSky, boolean supportedReservoirs, boolean monolithicAquifers,
		boolean fog, boolean clouds, double minimumJump) {
	public static UniverseCompatibility resolve(UniverseProfile p) {
		return new UniverseCompatibility(p.geometry() != 3 && p.geometry() != 8,
				(p.suspended() || p.hydrology() == 6) && p.hydrology() != 7, p.geology() == 0 && p.hydrology() == 5,
				p.get(HiddenParameter.FOG_DENSITY) > 0, p.get(HiddenParameter.CLOUD_COVER) > 0,
				Math.sqrt(Math.min(1.9, p.gravity() * (1 + p.get(HiddenParameter.LOCAL_GRAVITY) * .045))) * 1.02);
	}
}
