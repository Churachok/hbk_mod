package dev.kirill.hbk.universe;

import java.util.Locale;

/** An address, never a lossy numeric UI value. */
public record UniverseCode(String value) {
	public UniverseCode {
		if (!valid(value)) throw new IllegalArgumentException("Universe address must contain five ASCII digits");
	}

	public static boolean valid(String value) {
		return value != null && value.length() == 5 && value.chars().allMatch(c -> c >= '0' && c <= '9');
	}

	public static UniverseCode of(int value) {
		if (value < 0 || value > 99999) throw new IllegalArgumentException("Address outside 00000..99999");
		return new UniverseCode(String.format(Locale.ROOT, "%05d", value));
	}

	public int pattern(int position) {
		if (position < 0 || position >= 5) throw new IndexOutOfBoundsException(position);
		return value.charAt(position) - '0';
	}

	public long seed() { return UniverseNoise.mix(0x48424b5f554e4956L ^ Integer.parseInt(value)); }
	@Override public String toString() { return value; }
}
