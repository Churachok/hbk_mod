package dev.kirill.hbk.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent per-player counter for the items received from Konata today. */
public record KonataDailyClaim(long day, int count) {
	public static final KonataDailyClaim EMPTY = new KonataDailyClaim(Long.MIN_VALUE, 0);
	public static final Codec<KonataDailyClaim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("day").forGetter(KonataDailyClaim::day),
			Codec.INT.fieldOf("count").forGetter(KonataDailyClaim::count)
	).apply(instance, KonataDailyClaim::new));
}
