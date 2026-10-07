package dev.kirill.hbk.world.structure;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class GroveStreetPiece extends TemplateStructurePiece {
	public static final int CHEST_COUNT = 12;
	private final int bonusChest;

	public GroveStreetPiece(StructureTemplateManager manager, BlockPos pos, int bonusChest) {
		super(ModStructures.GROVE_STREET_PIECE, 0, manager, HbkMod.id("grove_street"),
				"hbk:grove_street", settings(), pos);
		this.bonusChest = bonusChest;
	}

	public GroveStreetPiece(StructureTemplateManager manager, CompoundTag tag) {
		super(ModStructures.GROVE_STREET_PIECE, tag, manager, id -> settings());
		this.bonusChest = tag.getIntOr("BonusChest", 0);
	}

	private static StructurePlaceSettings settings() {
		return new StructurePlaceSettings().setIgnoreEntities(true);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putInt("BonusChest", this.bonusChest);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		// Write only the current chunk; never load neighbours while generating a 7x7-chunk template.
		fillFoundation(level, this.boundingBox, chunkBox, this.templatePosition.getY());
		super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
		var chests = this.template.filterBlocks(this.templatePosition, settings(), Blocks.CHEST);
		if (chests.size() != CHEST_COUNT) throw new IllegalStateException("Grove Street requires twelve chests");
		BlockPos bonusPos = chests.get(Math.floorMod(this.bonusChest, chests.size())).pos();
		if (chunkBox.isInside(bonusPos) && level.getBlockEntity(bonusPos) instanceof ChestBlockEntity chest) {
			chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, HbkMod.id("chests/grove_street_bonus")), random.nextLong());
		}
	}

	public static void fillFoundation(ServerLevelAccessor level, BoundingBox footprint, BoundingBox chunkBox, int baseY) {
		for (int x = Math.max(footprint.minX(), chunkBox.minX()); x <= Math.min(footprint.maxX(), chunkBox.maxX()); x++) {
			for (int z = Math.max(footprint.minZ(), chunkBox.minZ()); z <= Math.min(footprint.maxZ(), chunkBox.maxZ()); z++) {
				BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, baseY - 1, z);
				while (pos.getY() > level.getMinY()) {
					var state = level.getBlockState(pos);
					if (state.isSolid() && state.getFluidState().isEmpty()
							&& !state.is(net.minecraft.tags.BlockTags.LEAVES) && !state.is(net.minecraft.tags.BlockTags.LOGS)) break;
					level.setBlock(pos, (baseY - pos.getY() <= 3 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2);
					pos.move(0, -1, 0);
				}
			}
		}
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
			RandomSource random, BoundingBox box) {}
}
