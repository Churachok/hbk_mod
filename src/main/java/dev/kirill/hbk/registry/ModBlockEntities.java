package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.block.entity.JagermeisterCauldronBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<JagermeisterCauldronBlockEntity> JAGERMEISTER_CAULDRON = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, HbkMod.id("jagermeister_cauldron"),
			FabricBlockEntityTypeBuilder.create(JagermeisterCauldronBlockEntity::new,
					ModBlocks.JAGERMEISTER_CAULDRON).build());

	private ModBlockEntities() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered block entities for {}", HbkMod.MOD_ID);
	}
}
