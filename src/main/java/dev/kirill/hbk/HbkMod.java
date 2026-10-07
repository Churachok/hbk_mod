package dev.kirill.hbk;

import dev.kirill.hbk.registry.ModStructures;
import dev.kirill.hbk.command.ModCommands;
import dev.kirill.hbk.mechanic.ModMechanics;
import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import dev.kirill.hbk.mechanic.UraniumArmorEffects;
import dev.kirill.hbk.menu.ModMenuTypes;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModFeatures;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModLoot;
import dev.kirill.hbk.registry.ModSounds;
import dev.kirill.hbk.world.ModWorldEvents;
import dev.kirill.hbk.world.ReferenceNpcSpawning;
import dev.kirill.hbk.world.UnknownEncounter;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbkMod implements ModInitializer {
	public static final String MOD_ID = "hbk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModAttachments.register();
		ModDataComponents.register();
		ModEffects.register();
		ModSounds.register();
		ModBlocks.register();
		ModFeatures.register();
		ModStructures.register();
		ModEntityTypes.register();
		ModEntityTypes.registerAttributes();
		ModItems.register();
		ModMenuTypes.register();
		FuelValueEvents.BUILD.register((builder, context) -> builder.add(ModItems.URANIUM_235, 600 * 20));
		ModLoot.register();
		ReferenceNpcSpawning.register();
		ModNetworking.register();
		ModMechanics.register();
		ProgenitorTransformation.register();
		UraniumArmorEffects.register();
		ModWorldEvents.register();
		UnknownEncounter.register();
		ModCommands.register();
		LOGGER.info("hbk is ready to walk.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
