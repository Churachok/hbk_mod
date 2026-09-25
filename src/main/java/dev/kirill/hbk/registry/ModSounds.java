package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent GOOSE_HONK = register("goose_honk");
	public static final SoundEvent MUSIC_DISC_HBKAU = register("music_disc.hbkau");
	public static final SoundEvent KIRILL_DOOM_MUSIC = register("music.kirill_doom");

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
