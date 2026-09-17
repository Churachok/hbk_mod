package dev.kirill.hbk.registry;

import com.mojang.serialization.Codec;
import dev.kirill.hbk.HbkMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {
	public static final DataComponentType<Boolean> ATTACK_MODE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			HbkMod.id("attack_mode"),
			DataComponentType.<Boolean>builder()
					.persistent(Codec.BOOL)
					.networkSynchronized(ByteBufCodecs.BOOL.cast())
					.build()
	);

	private ModDataComponents() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered data components for {}", HbkMod.MOD_ID);
	}
}
