package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.FellasEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class GroveStreetPopulation {
	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, HbkMod.id("grove_street"));
	private static final Map<ServerLevel, Map<Long, Long>> NEXT_GROUP = new WeakHashMap<>();
	private GroveStreetPopulation() {}

	public static void tick(ServerLevel level, ChunkAccess chunk) {
		if (!level.getGameRules().get(GameRules.SPAWN_MOBS)) return;
		for (var start : level.structureManager().startsForStructure(chunk.getPos(), structure ->
				level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structure).filter(STRUCTURE::equals).isPresent())) {
			if (!start.isValid()) continue;
			BoundingBox box = start.getPieces().getFirst().getBoundingBox();
			// Count all residents only when their territory and its immediate surroundings are loaded.
			if (!level.hasChunksAt(box.minX() - 8, box.minZ() - 8, box.maxX() + 8, box.maxZ() + 8)) continue;
			var cooldowns = NEXT_GROUP.computeIfAbsent(level, ignored -> new HashMap<>());
			long key = start.getChunkPos().pack();
			if (level.getGameTime() < cooldowns.getOrDefault(key, 0L)) continue;
			int spawned = spawnGroup(level, box);
			cooldowns.put(key, level.getGameTime() + (spawned == 4 ? 1200L : 80L));
		}
	}

	/** An entire group is prepared before adding any entity, so partial groups cannot exceed the cap. */
	static int spawnGroup(ServerLevel level, BoundingBox box) {
		if (!level.getGameRules().get(GameRules.SPAWN_MOBS)) return 0;
		int count = 0;
		for (var entity : level.getAllEntities()) {
			if (entity instanceof FellasEntity fella && fella.isAlive()
					&& (fella.belongsTo(box) || box.isInside(fella.blockPosition()))) count++;
		}
		if (count > 8) return 0;
		var group = new ArrayList<FellasEntity>();
		var random = level.getRandom();
		for (int attempt = 0; attempt < 400 && group.size() < 4; attempt++) {
			int anchorX = group.isEmpty() ? (box.minX() + box.maxX()) / 2 : group.getFirst().blockPosition().getX();
			int anchorZ = group.isEmpty() ? (box.minZ() + box.maxZ()) / 2 : group.getFirst().blockPosition().getZ();
			int radius = group.isEmpty() ? Math.max(box.getXSpan(), box.getZSpan()) : 6;
			int x = random.nextInt(Math.max(box.minX() + 1, anchorX - radius), Math.min(box.maxX(), anchorX + radius + 1));
			int z = random.nextInt(Math.max(box.minZ() + 1, anchorZ - radius), Math.min(box.maxZ(), anchorZ + radius + 1));
			if (!level.hasChunkAt(new BlockPos(x, box.minY(), z))) continue;
			for (int y = box.minY() + 1; y <= Math.min(box.minY() + 4, box.maxY() - 1); y++) {
				BlockPos pos = new BlockPos(x, y, z);
				var ground = level.getBlockState(pos.below());
				if (!(ground.is(Blocks.ANDESITE) || ground.is(Blocks.DYED_TERRACOTTA.cyan())
						|| ground.is(Blocks.CONCRETE.gray()) || ground.is(Blocks.GRASS_BLOCK))) continue;
				if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
				boolean tooClose = level.players().stream().anyMatch(player -> !player.isSpectator()
						&& player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 24 * 24);
				if (tooClose) continue;
				FellasEntity fella = ModEntityTypes.FELLAS.create(level, EntitySpawnReason.STRUCTURE);
				if (fella == null) return 0;
				fella.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
				if (!level.noCollision(fella) || group.stream().anyMatch(other -> other.getBoundingBox().intersects(fella.getBoundingBox()))) continue;
				fella.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
				fella.setTerritory(box);
				group.add(fella);
				break;
			}
		}
		if (group.size() != 4) return 0;
		for (FellasEntity fella : group) level.addFreshEntity(fella);
		return 4;
	}

	/** Called after a real chest menu opens, including both halves of double chests. */
	public static void onChestOpened(ServerLevel level, BlockPos chest, Player player) {
		if (player.isCreative() || player.isSpectator()) return;
		for (FellasEntity fella : level.getEntitiesOfClass(FellasEntity.class, new AABB(chest).inflate(32))) {
			if (fella.isAlive() && fella.hasLineOfSight(player) && fella.distanceToSqr(player) <= 32 * 32) {
				fella.provoke(player);
			}
		}
	}

	public static void alertToAttack(ServerLevel level, FellasEntity victim, LivingEntity offender) {
		for (FellasEntity fella : level.getEntitiesOfClass(FellasEntity.class, victim.getBoundingBox().inflate(32))) {
			if (fella.isAlive() && fella.hasLineOfSight(offender)) fella.provoke(offender);
		}
	}
}
