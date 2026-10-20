package dev.kirill.hbk.universe;

import com.mojang.serialization.Codec;
import dev.kirill.hbk.HbkMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The generator version and all selected values are saved alongside each universe's chunks. */
public final class UniverseState extends SavedData {
	public static final Codec<UniverseState> CODEC = UniverseProfile.CODEC.xmap(UniverseState::new, state -> state.profile);
	public final UniverseProfile profile;
	public UniverseState(UniverseProfile profile) { this.profile = profile; }
	public static SavedDataType<UniverseState> type(UniverseProfile profile) {
		return new SavedDataType<>(HbkMod.id("universe_profile"), () -> new UniverseState(profile), CODEC, DataFixTypes.SAVED_DATA_RAIDS);
	}
}
