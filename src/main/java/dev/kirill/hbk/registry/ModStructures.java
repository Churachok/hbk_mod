package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.structure.GroveStreetStructure;
import dev.kirill.hbk.world.structure.GroveStreetPiece;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModStructures {
	public static final StructureType<GroveStreetStructure> GROVE_STREET = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, HbkMod.id("grove_street"), () -> GroveStreetStructure.CODEC);
	public static final StructurePieceType GROVE_STREET_PIECE = Registry.register(
			BuiltInRegistries.STRUCTURE_PIECE, HbkMod.id("grove_street"),
			(context, tag) -> new GroveStreetPiece(context.structureTemplateManager(), tag));

	private ModStructures() {}
	public static void register() {}
}
