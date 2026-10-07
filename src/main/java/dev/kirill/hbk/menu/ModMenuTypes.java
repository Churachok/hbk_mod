package dev.kirill.hbk.menu;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModMenuTypes {
	public static final ExtendedMenuType<ImprovedBoatFuelMenu, Integer> IMPROVED_BOAT_FUEL = Registry.register(
			BuiltInRegistries.MENU, HbkMod.id("improved_boat_fuel"),
			new ExtendedMenuType<>(ImprovedBoatFuelMenu::new, ByteBufCodecs.VAR_INT));

	private ModMenuTypes() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered fuel menu for {}", HbkMod.MOD_ID);
	}
}
