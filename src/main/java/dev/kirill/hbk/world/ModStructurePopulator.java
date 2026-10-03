package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

/** Adds the one-time inhabitants and special block after a native structure is first visited. */
public final class ModStructurePopulator {
	private static final Identifier KIRILL_HOUSE = HbkMod.id("kirill_house");
	private static final Identifier KIRILL_HOUSE_HBK = HbkMod.id("kirill_house_hbk");
	private static final Identifier DENIS_HOUSE = HbkMod.id("denis_house");
	private static final Identifier KONATA_HOUSE = HbkMod.id("konata_house");
	private static final Identifier STALINKA = HbkMod.id("stalinka");
	private static final Identifier GULAG = HbkMod.id("gulag");
	private static final Identifier GRAVEYARD = HbkMod.id("graveyard");
	private static final Identifier STALIN_DACHA = HbkMod.id("stalin_dacha");
	private static final int VILLAGER_COUNT = 8;
	private static final float VILLAGER_CHANCE = 0.60f;
	private static final float LIZA_CHANCE = 0.60f;
	private static final float DENIS_CHANCE = 0.30f;
	private static final int NKVD_COUNT = 10;

	private ModStructurePopulator() {
	}

	public static void tryPopulateInChunk(ServerLevel level, ChunkAccess chunk) {
		List<StructureStart> starts = level.structureManager().startsForStructure(
				chunk.getPos(),
				structure -> getModStructureId(level, structure) != null
		);
		if (starts.isEmpty()) {
			return;
		}

		ModWorldData data = ModWorldData.get(level);
		for (StructureStart start : starts) {
			if (!start.isValid()) {
				continue;
			}

			Identifier id = getModStructureId(level, start.getStructure());
			if (id == null) {
				continue;
			}

			String populationKey = id + "@" + start.getChunkPos().pack();
			BoundingBox box = start.getBoundingBox();
			if (id.equals(STALIN_DACHA)) {
				populateStalinDacha(level, box, data, populationKey);
				continue;
			}
			if (!level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ())) {
				continue;
			}

			if (!data.isStructurePopulated(populationKey)) {
				boolean populated = true;
				if (id.equals(KIRILL_HOUSE)) {
					spawnNurse(level, box);
				} else if (id.equals(KONATA_HOUSE)) {
					populated = spawnKonata(level, box);
				} else if (id.equals(STALINKA)) {
					if (level.getRandom().nextFloat() < VILLAGER_CHANCE) {
						spawnVillagers(level, box);
					}
				} else if (id.equals(GULAG)) {
					populateGulag(level, box);
				} else if (id.equals(GRAVEYARD)) {
					populated = placeRecipeChest(level, box);
				}
				if (populated) {
					data.markStructurePopulated(populationKey);
				}
			}
			if (id.equals(GULAG)) {
				String suppliesKey = "gulag_supplies@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(suppliesKey)) {
					fillGulagBarrels(level, box);
					data.markStructurePopulated(suppliesKey);
				}
				String compassKey = "gulag_compass@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(compassKey) && placeGulagCompass(level, box)) {
					data.markStructurePopulated(compassKey);
				}
			}

			if (id.equals(KIRILL_HOUSE) || id.equals(KIRILL_HOUSE_HBK)) {
				String kirillPopulationKey = "kirill@" + id + "@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(kirillPopulationKey) && spawnKirillNear(level, box)) {
					data.markStructurePopulated(kirillPopulationKey);
				}
			}
			if (id.equals(KIRILL_HOUSE) || id.equals(KIRILL_HOUSE_HBK)) {
				String npcName = id.equals(KIRILL_HOUSE) ? "lex" : "vlad";
				String npcKey = npcName + "@" + id + "@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(npcKey)) {
					BlockPos pos = findPositionNearStructure(level, box, 120);
					var type = id.equals(KIRILL_HOUSE) ? ModEntityTypes.LEX : ModEntityTypes.VLAD;
					if (pos != null && type.spawn(level, pos, EntitySpawnReason.STRUCTURE) != null) {
						data.markStructurePopulated(npcKey);
					}
				}
			}
			if (id.equals(DENIS_HOUSE)) {
				String denisKey = "denis@" + populationKey;
				String rollKey = denisKey + "@rolled";
				String selectedKey = denisKey + "@selected";
				if (!data.isStructurePopulated(rollKey)) {
					if (level.getRandom().nextFloat() < DENIS_CHANCE) {
						data.markStructurePopulated(selectedKey);
					}
					data.markStructurePopulated(rollKey);
				}
				// Retry only a selected spawn with no safe position; never reroll the 30% chance.
				if (data.isStructurePopulated(selectedKey) && !data.isStructurePopulated(denisKey)) {
					BlockPos pos = findPositionNearStructure(level, box, 120);
					if (pos != null && ModEntityTypes.DENIS.spawn(level, pos, EntitySpawnReason.STRUCTURE) != null) {
						data.markStructurePopulated(denisKey);
					}
				}
			}
			if (id.equals(STALINKA)) {
				String lizaPopulationKey = "liza@" + id + "@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(lizaPopulationKey)) {
					if (level.getRandom().nextFloat() < LIZA_CHANCE) {
						spawnLiza(level, box);
					}
					data.markStructurePopulated(lizaPopulationKey);
				}
			}
		}
	}

	private static Identifier getModStructureId(ServerLevel level, Structure structure) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getResourceKey(structure)
				.map(key -> key.identifier())
				.filter(id -> id.getNamespace().equals(HbkMod.MOD_ID))
				.filter(id -> id.equals(KIRILL_HOUSE) || id.equals(KIRILL_HOUSE_HBK)
						|| id.equals(DENIS_HOUSE) || id.equals(KONATA_HOUSE)
						|| id.equals(STALINKA) || id.equals(GULAG) || id.equals(GRAVEYARD)
						|| id.equals(STALIN_DACHA))
				.orElse(null);
	}

	/** Residents can appear as soon as their part of the large template is loaded. */
	private static void populateStalinDacha(ServerLevel level, BoundingBox box, ModWorldData data, String key) {
		populateStalinDachaResidents(level, box, data, key);
		if (!data.isStructurePopulated(key)
				&& level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ())
				&& fillStalinDachaChests(level, box)) {
			data.markStructurePopulated(key);
		}
	}

	static void populateStalinDachaResidents(ServerLevel level, BoundingBox box, ModWorldData data, String key) {
		for (int i = 0; i < 4; i++) {
			String guardKey = "dacha_guard_" + i + "@" + key;
			if (data.isStructurePopulated(guardKey)) {
				continue;
			}
			BlockPos guardPos = findDachaStandingPosition(level, box, 120);
			if (guardPos != null && ModEntityTypes.NKVD.spawn(level, guardPos, EntitySpawnReason.STRUCTURE) != null) {
				data.markStructurePopulated(guardKey);
			}
		}
		String grishaKey = "dacha_grisha@" + key;
		if (!data.isStructurePopulated(grishaKey)) {
			BlockPos grishaPos = findDachaStandingPosition(level, box, 120);
			if (grishaPos != null && ModEntityTypes.GRISHA.spawn(level, grishaPos, EntitySpawnReason.STRUCTURE) != null) {
				data.markStructurePopulated(grishaKey);
			}
		}
	}

	private static BlockPos findDachaStandingPosition(ServerLevel level, BoundingBox box, int attempts) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < attempts; attempt++) {
			int x = random.nextInt(box.minX() + 1, box.maxX());
			int z = random.nextInt(box.minZ() + 1, box.maxZ());
			if (!level.hasChunkAt(new BlockPos(x, box.minY(), z))) {
				continue;
			}
			for (int y = box.minY() + 1; y < Math.min(box.minY() + 15, box.maxY()); y++) {
				BlockPos pos = new BlockPos(x, y, z);
				if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
						&& level.getBlockState(pos.below()).isSolid()) {
					return pos;
				}
			}
		}
		return null;
	}

	static boolean fillStalinDachaChests(ServerLevel level, BoundingBox box) {
		java.util.ArrayList<ChestBlockEntity> chests = new java.util.ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
				box.maxX(), box.maxY(), box.maxZ())) {
			if (level.getBlockState(pos).is(Blocks.CHEST)
						&& level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
				chests.add(chest);
			}
		}
		if (chests.isEmpty()) {
			return false;
		}
		RandomSource random = level.getRandom();
		for (ChestBlockEntity chest : chests) {
			if (chest.getLootTable() != null) {
				continue;
			}
			int rolls = 2 + random.nextInt(4);
			for (int i = 0; i < rolls; i++) {
				int roll = random.nextInt(100);
				Item item = roll < 10 ? Items.DIAMOND
						: roll < 14 ? Items.NETHERITE_SCRAP
						: roll < 23 ? Items.GOLDEN_APPLE
						: roll < 39 ? ModItems.CONDENSED_MILK
						: roll < 55 ? ModItems.RATION
						: roll < 69 ? ModItems.CURRANT_TINCTURE
						: roll < 84 ? ModItems.BANDAGE
						: roll < 92 ? ModItems.SICKLE_AND_HAMMER
						: ModItems.MUSIC_DISC_USSR_ANTHEM;
				int count = item == Items.DIAMOND || item == ModItems.RATION || item == ModItems.BANDAGE
						? 1 + random.nextInt(3) : 1;
				addToEmptySlot(chest, new ItemStack(item, count), random);
			}
			chest.setChanged();
		}
		return placeUniqueDachaItem(chests, ModItems.STALIN_PIPE, random)
				&& placeUniqueDachaItem(chests, ModItems.STALIN_SPAWN_EGG, random);
	}

	private static boolean placeUniqueDachaItem(List<ChestBlockEntity> chests, Item item, RandomSource random) {
		for (ChestBlockEntity chest : chests) {
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				if (chest.getItem(slot).is(item)) {
					return true;
				}
			}
		}
		int first = random.nextInt(chests.size());
		for (int offset = 0; offset < chests.size(); offset++) {
			if (addToEmptySlot(chests.get((first + offset) % chests.size()), new ItemStack(item), random)) {
				return true;
			}
		}
		return false;
	}

	private static boolean addToEmptySlot(ChestBlockEntity chest, ItemStack stack, RandomSource random) {
		int first = random.nextInt(chest.getContainerSize());
		for (int offset = 0; offset < chest.getContainerSize(); offset++) {
			int slot = (first + offset) % chest.getContainerSize();
			if (chest.getItem(slot).isEmpty()) {
				chest.setItem(slot, stack);
				return true;
			}
		}
		return false;
	}

	private static void spawnNurse(ServerLevel level, BoundingBox box) {
		BlockPos pos = findStandingPosition(level, box, 200);
		if (pos == null || ModEntityTypes.NURSE.spawn(level, pos, EntitySpawnReason.STRUCTURE) == null) {
			HbkMod.LOGGER.warn("Could not spawn the nurse in Kirill's house at {}", box);
		}
	}

	private static boolean spawnKonata(ServerLevel level, BoundingBox box) {
		BlockPos pos = findStandingPosition(level, box, 200);
		var konata = pos == null ? null : ModEntityTypes.KONATA.spawn(level, pos, EntitySpawnReason.STRUCTURE);
		if (konata == null) {
			HbkMod.LOGGER.warn("Could not spawn Konata in her house at {}", box);
			return false;
		}
		konata.setPersistenceRequired();
		return true;
	}

	private static boolean spawnKirillNear(ServerLevel level, BoundingBox box) {
		BlockPos pos = findPositionNearStructure(level, box, 120);
		if (pos == null || ModEntityTypes.KIRILL.spawn(level, pos, EntitySpawnReason.STRUCTURE) == null) {
			HbkMod.LOGGER.warn("Could not spawn Kirill near his house at {}", box);
			return false;
		}
		return true;
	}

	private static void spawnLiza(ServerLevel level, BoundingBox box) {
		BlockPos pos = findStandingPosition(level, box, 200);
		if (pos == null || ModEntityTypes.LIZA.spawn(level, pos, EntitySpawnReason.STRUCTURE) == null) {
			HbkMod.LOGGER.warn("Could not spawn Liza in the Stalinka at {}", box);
		}
	}

	private static BlockPos findPositionNearStructure(ServerLevel level, BoundingBox box, int attempts) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < attempts; attempt++) {
			int x = random.nextInt(box.minX() - 10, box.maxX() + 11);
			int z = random.nextInt(box.minZ() - 10, box.maxZ() + 11);
			if (x >= box.minX() - 2 && x <= box.maxX() + 2
					&& z >= box.minZ() - 2 && z <= box.maxZ() + 2) {
				continue;
			}

			BlockPos column = new BlockPos(x, box.maxY(), z);
			if (!level.hasChunkAt(column)) {
				continue;
			}
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.above()).isAir()
					&& level.getBlockState(pos.below()).isSolid()) {
				return pos;
			}
		}
		return null;
	}

	private static void spawnVillagers(ServerLevel level, BoundingBox box) {
		RandomSource random = level.getRandom();
		int spawned = 0;
		for (int attempt = 0; attempt < VILLAGER_COUNT * 30 && spawned < VILLAGER_COUNT; attempt++) {
			BlockPos spawnPos = findStandingPosition(level, box, 1);
			if (spawnPos == null) {
				continue;
			}
			Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
			if (villager == null) {
				continue;
			}
			villager.snapTo(spawnPos, random.nextFloat() * 360.0f, 0.0f);
			villager.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.STRUCTURE, null);
			villager.setPersistenceRequired();
			if (level.addFreshEntity(villager)) {
				spawned++;
			}
		}
	}

	private static void populateGulag(ServerLevel level, BoundingBox box) {
		BlockPos chestColumn = new BlockPos((box.minX() + box.maxX()) / 2, 0, box.minZ() - 2);
		BlockPos chestPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chestColumn);
		if (level.getBlockState(chestPos).canBeReplaced()) {
			level.setBlockAndUpdate(chestPos, ModBlocks.STRANGE_CHEST.defaultBlockState());
		}

		BlockPos origin = new BlockPos(box.minX(), box.minY(), box.minZ());
		Vec3i size = new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan());
		NkvdSpawning.spawnSquadInArea(level, origin, size, NKVD_COUNT);
	}

	static void fillGulagBarrels(ServerLevel level, BoundingBox box) {
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
				box.maxX(), box.maxY(), box.maxZ())) {
			if (!level.getBlockState(pos).is(Blocks.BARREL)
					|| !(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)
					|| barrel.getLootTable() != null) {
				continue;
			}
			int supplies = 1 + level.getRandom().nextInt(3);
			for (int i = 0; i < supplies; i++) {
				Item item = switch (level.getRandom().nextInt(3)) {
					case 0 -> ModItems.RATION;
					case 1 -> ModItems.STEW;
					default -> ModItems.CONDENSED_MILK;
				};
				addToEmptySlot(barrel, new ItemStack(item, 1 + level.getRandom().nextInt(2)), level.getRandom());
			}
			if (level.getRandom().nextFloat() < 0.03f) {
				addToEmptySlot(barrel, new ItemStack(ModItems.STALIN_SPAWN_EGG), level.getRandom());
			}
			barrel.setChanged();
		}
	}

	/** Places one compass per Gulag, including Gulags generated before this item existed. */
	static boolean placeGulagCompass(ServerLevel level, BoundingBox box) {
		BarrelBlockEntity candidate = null;
		int candidateSlot = -1;
		int candidates = 0;
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
				box.maxX(), box.maxY(), box.maxZ())) {
			if (!level.getBlockState(pos).is(Blocks.BARREL)
					|| !(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)
					|| barrel.getLootTable() != null) {
				continue;
			}
			for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
				if (barrel.getItem(slot).is(ModItems.RESET_COMPASS)) {
					return true;
				}
			}
			for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
				if (barrel.getItem(slot).isEmpty()) {
					if (level.getRandom().nextInt(++candidates) == 0) {
						candidate = barrel;
						candidateSlot = slot;
					}
					break;
				}
			}
		}
		if (candidate == null) {
			return false;
		}
		candidate.setItem(candidateSlot, new ItemStack(ModItems.RESET_COMPASS));
		candidate.setChanged();
		return true;
	}

	private static void addToEmptySlot(BarrelBlockEntity barrel, ItemStack stack, RandomSource random) {
		int first = random.nextInt(barrel.getContainerSize());
		for (int offset = 0; offset < barrel.getContainerSize(); offset++) {
			int slot = (first + offset) % barrel.getContainerSize();
			if (barrel.getItem(slot).isEmpty()) {
				barrel.setItem(slot, stack);
				return;
			}
		}
	}

	static boolean placeRecipeChest(ServerLevel level, BoundingBox box) {
		// Search the perimeter so the book is visible outside the graveyard and existing blocks survive.
		for (int distance = 2; distance <= 4; distance++) {
			for (int x = box.minX() - distance; x <= box.maxX() + distance; x++) {
				for (int z : new int[] {box.minZ() - distance, box.maxZ() + distance}) {
					if (tryPlaceRecipeChest(level, box, x, z)) {
						return true;
					}
				}
			}
			for (int z = box.minZ() - distance + 1; z < box.maxZ() + distance; z++) {
				for (int x : new int[] {box.minX() - distance, box.maxX() + distance}) {
					if (tryPlaceRecipeChest(level, box, x, z)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean tryPlaceRecipeChest(ServerLevel level, BoundingBox box, int x, int z) {
		BlockPos column = new BlockPos(x, box.maxY(), z);
		if (!level.hasChunkAt(column)) {
			return false;
		}
		BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
		if (Math.abs(pos.getY() - box.minY()) > 5 || !level.getBlockState(pos).isAir()
				|| !level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.below()).isSolid()) {
			return false;
		}
		if (!level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState())) {
			return false;
		}
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			chest.setItem(13, GraveyardRecipeBook.create());
			return true;
		}
		return false;
	}

	private static BlockPos findStandingPosition(ServerLevel level, BoundingBox box, int attempts) {
		RandomSource random = level.getRandom();
		int minX = box.minX() + 1;
		int maxX = box.maxX() - 1;
		int minZ = box.minZ() + 1;
		int maxZ = box.maxZ() - 1;
		if (minX > maxX || minZ > maxZ) {
			return null;
		}

		for (int attempt = 0; attempt < attempts; attempt++) {
			int x = random.nextInt(minX, maxX + 1);
			int z = random.nextInt(minZ, maxZ + 1);
			for (int y = box.minY() + 1; y < box.maxY(); y++) {
				BlockPos pos = new BlockPos(x, y, z);
				if (level.getBlockState(pos).isAir()
						&& level.getBlockState(pos.above()).isAir()
						&& level.getBlockState(pos.below()).isSolid()) {
					return pos;
				}
			}
		}
		return null;
	}
}
