package dev.kirill.hbk.world.structure;

import com.mojang.serialization.MapCodec;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;

/** Keeps the living-room floor at the surface, not the bottom of its hidden basement. */
public final class KonataHouseStructure extends Structure {
	public static final MapCodec<KonataHouseStructure> CODEC = simpleCodec(KonataHouseStructure::new);
	public static final int HOUSE_FLOOR_Y = 11;
	// Heightmaps return the first free block; terrain adaptation must also use this
	// above-ground level instead of SinglePoolElement's basement-relative delta of 1.
	public static final int GROUND_LEVEL_DELTA = HOUSE_FLOOR_Y + 1;

	public KonataHouseStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var pool = context.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, HbkMod.id("konata_house"))).value();
		var manager = context.structureTemplateManager();
		var rotation = Rotation.getRandom(context.random());
		var element = pool.getRandomTemplate(context.random());
		var relativeBox = element.getBoundingBox(manager, BlockPos.ZERO, rotation);
		// All four rotations get the same 9x9 footprint inside the selected chunk.
		var horizontalOrigin = new BlockPos(context.chunkPos().getMinBlockX() + 4 - relativeBox.minX(), 0,
				context.chunkPos().getMinBlockZ() + 4 - relativeBox.minZ());
		var footprint = element.getBoundingBox(manager, horizontalOrigin, rotation);
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		// Check every column, plus the approach to the porch. A dry centre alone
		// would still allow the rest of the house to hang over a lake or river bank.
		for (int x = footprint.minX() - 1; x <= footprint.maxX() + 1; x++) {
			for (int z = footprint.minZ() - 1; z <= footprint.maxZ() + 1; z++) {
				int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
						context.heightAccessor(), context.randomState());
				int ground = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
						context.heightAccessor(), context.randomState());
				if (surface != ground) return Optional.empty();
				lowest = Math.min(lowest, surface);
				highest = Math.max(highest, surface);
				// A one-block gentle slope needs no hollow stilts or deep excavation.
				if (highest - lowest > 1) return Optional.empty();
			}
		}
		var origin = horizontalOrigin.atY(highest - GROUND_LEVEL_DELTA);
		var box = element.getBoundingBox(manager, origin, rotation);
		if (box.minY() < context.heightAccessor().getMinY()
				|| box.maxY() >= context.heightAccessor().getMaxY()) return Optional.empty();
		var piece = new PoolElementStructurePiece(manager, element, origin, GROUND_LEVEL_DELTA, rotation, box,
				LiquidSettings.IGNORE_WATERLOGGING);
		var surfaceAnchor = new BlockPos((box.minX() + box.maxX()) / 2, highest, (box.minZ() + box.maxZ()) / 2);
		return Optional.of(new GenerationStub(surfaceAnchor, builder -> builder.addPiece(piece)));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.KONATA_HOUSE;
	}
}
