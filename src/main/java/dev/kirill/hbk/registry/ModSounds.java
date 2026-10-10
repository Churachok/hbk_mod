package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent CANNED_LAUGHTER = register("canned_laughter");
	public static final SoundEvent BUS_HORN = register("bus_horn");
	public static final SoundEvent GOOSE_HONK = register("goose_honk");
	public static final SoundEvent MUSIC_DISC_HBKAU = register("music_disc.hbkau");
	public static final SoundEvent MUSIC_DISC_USSR_ANTHEM = register("music_disc.ussr_anthem");
	public static final SoundEvent STALIN_BOSS_MUSIC = register("music.stalin_boss");
	public static final SoundEvent CJ_BOSS_MUSIC = register("music.cj_boss");
	public static final SoundEvent KIRILL_DOOM_MUSIC = register("music.kirill_doom");
	public static final SoundEvent MAD_LIBERAL_MUSIC = register("music.mad_liberal");
	public static final SoundEvent UNKNOWN_MUSIC = register("music.unknown");
	public static final SoundEvent UNKNOWN_DEATH = register("unknown_death");
	public static final SoundEvent KONATA_AMBIENT = register("entity.konata.ambient");
	public static final SoundEvent KONATA_PUPUE = register("entity.konata.pupue");
	public static final SoundEvent KONATA_GOOD = register("entity.konata.good");
	public static final SoundEvent MUSIC_DISC_KONATA_THEME = register("music_disc.konata_theme");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		var id = HbkMod.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered sounds for {}", HbkMod.MOD_ID);
	}
}
