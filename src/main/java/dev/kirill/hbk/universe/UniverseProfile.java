package dev.kirill.hbk.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record UniverseProfile(UniverseCode code, int version, long seed, List<Integer> hidden) {
	public static final int VERSION = 1;
	public static final Codec<UniverseCode> CODE_CODEC = Codec.STRING.comapFlatMap(value -> UniverseCode.valid(value)
			? DataResult.success(new UniverseCode(value)) : DataResult.error(() -> "Invalid universe address"), UniverseCode::value);
	public static final Codec<UniverseProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
			CODE_CODEC.fieldOf("code").forGetter(UniverseProfile::code), Codec.intRange(1, VERSION).fieldOf("version").forGetter(UniverseProfile::version),
			Codec.LONG.fieldOf("seed").forGetter(UniverseProfile::seed),
			Codec.intRange(0, 9).listOf().validate(list -> list.size() == 36 ? DataResult.success(list)
					: DataResult.error(() -> "Expected 36 hidden parameters")).fieldOf("hidden").forGetter(UniverseProfile::hidden)
	).apply(i, UniverseProfile::new));
	private static final Map<UniverseCode, UniverseProfile> CACHE = new LinkedHashMap<>(64, .75f, true);
	public UniverseProfile {
		if (version != VERSION || hidden.size() != 36 || hidden.stream().anyMatch(v -> v < 0 || v > 9)) {
			throw new IllegalArgumentException("Unsupported universe profile");
		}
		hidden = List.copyOf(hidden);
	}
	public static UniverseProfile create(UniverseCode code) {
		long seed = code.seed();
		List<Integer> values = new ArrayList<>(36);
		for (int i = 0; i < 36; i++) values.add((int) Long.remainderUnsigned(UniverseNoise.mix(seed + 0x9e3779b97f4a7c15L * (i + 1)), 10));
		return new UniverseProfile(code, VERSION, seed, values);
	}
	public static synchronized UniverseProfile of(UniverseCode code) {
		UniverseProfile profile = CACHE.computeIfAbsent(code, UniverseProfile::create);
		if (CACHE.size() > 256) CACHE.remove(CACHE.keySet().iterator().next());
		return profile;
	}
	public static UniverseProfile of(Level level) { return of(level.dimension()); }
	public static UniverseProfile of(ResourceKey<Level> key) {
		var id = key.identifier();
		String path = id.getPath();
		return id.getNamespace().equals("hbk") && path.startsWith("universe_") && UniverseCode.valid(path.substring(9))
				? of(new UniverseCode(path.substring(9))) : null;
	}
	public int geometry() { return code.pattern(0); }
	public int relief() { return code.pattern(1); }
	public int geology() { return code.pattern(2); }
	public int distribution() { return code.pattern(3); }
	public int hydrology() { return code.pattern(4); }
	public int get(HiddenParameter parameter) { return hidden.get(parameter.ordinal()); }
	public long stream(long channel) { return UniverseNoise.mix(seed ^ channel); }
	public double scale() { return 100 + 80 * UniverseNoise.unit(stream(101)); }
	public double amplitude() { return 12 + 25 * UniverseNoise.unit(stream(102)); }
	public boolean openSky() { return geometry() != 3 && geometry() != 8; }
	public boolean suspended() { return geometry() == 1 || geometry() == 7 || geometry() == 8; }
	public double gravity() { return new double[]{.4, .55, .7, .85, 1, 1.1, 1.2, 1.3, 1.4, 1.5}[get(HiddenParameter.GRAVITY)]; }
	public double jump() {
		double selected = new double[]{.75, .8, .85, .9, .95, 1, 1.05, 1.1, 1.15, 1.25}[get(HiddenParameter.JUMP)];
		// A one-block step remains possible, even in a heavy-gravity universe.
		return Math.max(selected, UniverseCompatibility.resolve(this).minimumJump());
	}
}
