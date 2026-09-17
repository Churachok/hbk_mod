package dev.kirill.hbk;

import dev.kirill.hbk.command.ModCommands;
import dev.kirill.hbk.mechanic.ModMechanics;
import dev.kirill.hbk.mechanic.UraniumArmorEffects;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModFeatures;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModSounds;
import dev.kirill.hbk.world.ModWorldEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbkMod implements ModInitializer {
	public static final String MOD_ID = "hbk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModDataComponents.register();
		ModEffects.register();
		ModSounds.register();
		ModBlocks.register();
		ModFeatures.register();
		ModEntityTypes.register();
		ModEntityTypes.registerAttributes();
		ModItems.register();
		ModNetworking.register();
		ModMechanics.register();
		UraniumArmorEffects.register();
		ModWorldEvents.register();
		ModCommands.register();
		LOGGER.info("hbk is ready to walk.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
