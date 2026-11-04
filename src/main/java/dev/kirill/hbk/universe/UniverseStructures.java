package dev.kirill.hbk.universe;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dev.kirill.hbk.universe.UniverseNoise.*;

/** Bounded modular plans reconstructed from the address. Never reads or edits a neighbouring chunk. */
public final class UniverseStructures {
	public static final int SPACING = 1024;
	public enum Kind { RESEARCH_COMPLEX, WATCHTOWER, UNDERGROUND_LAB, RUINED_CITY, RING_MACHINE }
	public enum RoomKind { ENTRANCE, DORMITORY, BOTANY, ARCHIVE, REACTOR, WORKSHOP, OBSERVATORY, VAULT }
	public record Room(int x, int y, int z, int radius, RoomKind kind, boolean ruined) {}
	public record Passage(int x0, int y, int z0, int x1, int z1) {}
	public record Treasure(BlockPos pos, boolean secret) {}
	public record Site(Kind kind, BlockPos origin, int rotation, long seed, List<Room> rooms,
			List<Passage> passages, List<Treasure> treasures, List<BlockPos> nests, int surface) {
		public BlockPos world(int x, int y, int z) {
			for (int n = 0; n < rotation; n++) { int old = x; x = -z; z = old; }
			return origin.offset(x, y, z);
		}
		public boolean intersects(int x, int z, int margin) {
			return Math.abs(x - origin.getX()) <= 88 + margin && Math.abs(z - origin.getZ()) <= 88 + margin;
		}
	}
	private final UniverseProfile profile;
	private final UniverseDensity terrain;
	private final Map<Long, Optional<Site>> cache = new ConcurrentHashMap<>();
	public UniverseStructures(UniverseProfile profile) { this.profile = profile; terrain = new UniverseDensity(profile); }
	public Optional<Site> site(int cx, int cz) {
		if (cache.size() > 256) cache.clear();
		return cache.computeIfAbsent(((long) cx << 32) ^ (cz & 0xffffffffL), ignored -> plan(cx, cz));
	}
	private Optional<Site> plan(int cx, int cz) {
		long seed = hash(profile.stream(85), cx, 0, cz);
		Kind kind = Kind.values()[(int) Long.remainderUnsigned(seed, Kind.values().length)];
		for (int attempt = 0; attempt < 5; attempt++) {
			int x = cx * SPACING + 160 + (int) Long.remainderUnsigned(mix(seed + attempt * 31), 704);
			int z = cz * SPACING + 160 + (int) Long.remainderUnsigned(mix(seed + attempt * 71 + 1), 704);
			boolean anomaly = false;
			for (int ax = Math.floorDiv(x - 96, AnomalyZones.SPACING); ax <= Math.floorDiv(x + 96, AnomalyZones.SPACING); ax++)
				for (int az = Math.floorDiv(z - 96, AnomalyZones.SPACING); az <= Math.floorDiv(z + 96, AnomalyZones.SPACING); az++)
					for (var zone : AnomalyZones.zonesInCell(AnomalyZones.layoutSeed(profile.seed()), ax, az))
						anomaly |= zone.intersects(x - 96, z - 96, x + 96, z + 96);
			if (anomaly) continue;
			int surface = 80;
			for (int y = 196; y > -16; y--) {
				if (terrain.sample(x, y, z) > 0 && terrain.sample(x, y + 7, z) < 0) { surface = y + 1; break; }
			}
			int y = Math.clamp(surface, 8, kind == Kind.WATCHTOWER ? 152 : 196);
			if (kind == Kind.UNDERGROUND_LAB) y = Math.max(-28, surface - 30);
			return Optional.of(assemble(kind, new BlockPos(x, y, z), (int) (seed >>> 7 & 3), seed, surface));
		}
		return Optional.empty();
	}
	private Site assemble(Kind kind, BlockPos origin, int rotation, long seed, int surface) {
		var rooms = new ArrayList<Room>(); var passages = new ArrayList<Passage>();
		switch (kind) {
			case RESEARCH_COMPLEX -> {
				rooms.add(new Room(0, 0, -24, 8, RoomKind.ENTRANCE, false));
				rooms.add(new Room(0, 0, 0, 10, RoomKind.REACTOR, false));
				rooms.add(new Room(-24, 0, 0, 8, RoomKind.DORMITORY, true));
				rooms.add(new Room(24, 0, 0, 8, RoomKind.BOTANY, false));
				rooms.add(new Room(0, 0, 24, 8, RoomKind.ARCHIVE, false));
				rooms.add(new Room(24, 0, 24, 6, RoomKind.VAULT, false));
				for (var r : rooms) { passages.add(new Passage(0, 0, 0, r.x, 0)); passages.add(new Passage(r.x, 0, 0, r.x, r.z)); }
			}
			case WATCHTOWER -> {
				for (int floor = 0; floor < 10; floor++) rooms.add(new Room(0, floor * 11, 0, floor == 0 ? 12 : 9,
						floor == 0 ? RoomKind.ENTRANCE : floor == 8 ? RoomKind.ARCHIVE : floor == 9 ? RoomKind.OBSERVATORY : RoomKind.values()[1 + (int) Long.remainderUnsigned(mix(seed + floor), 5)], floor == 5));
				rooms.add(new Room(22, 88, 22, 6, RoomKind.VAULT, false));
				passages.add(new Passage(0,88,0,22,0)); passages.add(new Passage(22,88,0,22,22));
			}
			case UNDERGROUND_LAB -> {
				for (int floor = 0; floor < 2; floor++) for (int wing = -1; wing <= 1; wing++) {
					rooms.add(new Room(wing * 24, floor * 11, 0, 9, floor == 0 && wing == 1 ? RoomKind.VAULT : wing == -1 ? RoomKind.BOTANY : wing == 0 ? RoomKind.REACTOR : RoomKind.ARCHIVE, false));
					passages.add(new Passage(-24, floor * 11, 0, 24, 0));
				}
				rooms.add(new Room(0, surface - origin.getY(), -24, 7, RoomKind.ENTRANCE, false));
				passages.add(new Passage(0, 11, 0, 0, -24));
				passages.add(new Passage(0, 0, 0, 0, -24));
			}
			case RUINED_CITY -> {
				for (int gx = -1; gx <= 1; gx++) for (int gz = -1; gz <= 1; gz++) {
					int floors = 1 + (int) Long.remainderUnsigned(hash(seed, gx, 0, gz), 3);
					for (int floor = 0; floor < floors; floor++) rooms.add(new Room(gx * 38, floor * 11, gz * 38, 11,
							RoomKind.values()[1 + Math.floorMod(gx + gz * 3 + floor, 5)], true));
					passages.add(new Passage(-38, 0, gz * 38, 38, gz * 38));
					passages.add(new Passage(gx * 38, 0, -38, gx * 38, 38));
				}
				rooms.add(new Room(-38,-11,38,7,RoomKind.VAULT,false));
			}
			case RING_MACHINE -> {
				rooms.add(new Room(0, 0, 0, 10, RoomKind.REACTOR, false));
				rooms.add(new Room(0, -11, 0, 7, RoomKind.VAULT, false));
				for (int n = -1; n <= 1; n += 2) {
					rooms.add(new Room(n * 45, 0, 0, 7, RoomKind.WORKSHOP, false));
					passages.add(new Passage(n * 45, 0, 0, 0, 0));
				}
			}
		}
		// Rotation also rotates loot and mob positions, so plans remain consistent across chunk edges.
		var base = new Site(kind, origin, rotation, seed, List.copyOf(rooms), List.copyOf(passages), List.of(), List.of(), surface);
		var chests = new ArrayList<Treasure>(); var nests = new ArrayList<BlockPos>();
		for (int n = 0; n < rooms.size(); n++) {
			Room r = rooms.get(n);
			if (r.kind == RoomKind.VAULT || r.kind == RoomKind.ARCHIVE || r.kind == RoomKind.OBSERVATORY || n == 0)
				chests.add(new Treasure(base.world(r.x + r.radius - 2, r.y + 1, r.z + r.radius - 2), r.kind == RoomKind.VAULT));
			if ((r.kind == RoomKind.BOTANY || r.kind == RoomKind.DORMITORY) && (hash(seed, n, 0, 0) & 1) == 0)
				nests.add(base.world(r.x - r.radius + 2, r.y + 1, r.z + r.radius - 2));
		}
		return new Site(kind, origin, rotation, seed, base.rooms, base.passages, List.copyOf(chests), List.copyOf(nests), surface);
	}
	public List<Site> forChunk(int x, int z) {
		var result = new ArrayList<Site>();
		for (int cx = Math.floorDiv(x - 88, SPACING); cx <= Math.floorDiv(x + 103, SPACING); cx++)
			for (int cz = Math.floorDiv(z - 88, SPACING); cz <= Math.floorDiv(z + 103, SPACING); cz++)
				site(cx, cz).filter(s -> s.intersects(x + 8, z + 8, 8)).ifPresent(result::add);
		return result;
	}
	public Site nearest(BlockPos pos, Kind kind) {
		Site result = null; double distance = Double.MAX_VALUE;
		int cx = Math.floorDiv(pos.getX(), SPACING), cz = Math.floorDiv(pos.getZ(), SPACING);
		for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
			var candidate = site(cx + dx, cz + dz); if (candidate.isEmpty() || candidate.get().kind != kind) continue;
			double d = candidate.get().origin.distSqr(pos); if (d < distance) { result = candidate.get(); distance = d; }
		}
		return result;
	}
	public BlockState material(List<Site> sites, int x, int y, int z) {
		for (var site : sites) {
			if (!site.intersects(x, z, 0)) continue;
			int u = x - site.origin.getX(), h = y - site.origin.getY(), v = z - site.origin.getZ();
			for (int n = 0; n < site.rotation; n++) { int old = u; u = v; v = -old; }
			if (h < -12 || h > 118) continue;
			// Interior access: continuous backed ladders join the tower, lab and service vault.
			int shaftTop = site.kind == Kind.WATCHTOWER ? 100 : site.kind == Kind.UNDERGROUND_LAB ? Math.max(12, site.surface - site.origin.getY() + 2) : site.kind == Kind.RING_MACHINE ? 2 : -99;
			int shaftZ = site.kind == Kind.UNDERGROUND_LAB ? -24 : 0;
			if (h >= (site.kind == Kind.RING_MACHINE ? -10 : 1) && h <= shaftTop && u >= -1 && u <= 1 && v >= shaftZ - 2 && v <= shaftZ + 1) {
				if (v == shaftZ - 2) return Blocks.TUFF_BRICKS.defaultBlockState();
				if (u == 0 && v == shaftZ - 1) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, rotate(Direction.SOUTH, site.rotation));
				return Blocks.CAVE_AIR.defaultBlockState();
			}
			for (var treasure : site.treasures) {
				var pos = treasure.pos;
				if (pos.getX() == x && pos.getY() == y && pos.getZ() == z) return Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, rotate(Direction.NORTH, site.rotation));
			}
			for (var pos : site.nests) if (pos.getX() == x && pos.getY() == y && pos.getZ() == z) return Blocks.SPAWNER.defaultBlockState();
			if (site.kind == Kind.RUINED_CITY) {
				int cx = (int) Math.round(u / 38.0) * 38, cz = (int) Math.round(v / 38.0) * 38;
				int top = 0;
				for (var room : site.rooms) if (room.x == cx && room.z == cz) top = Math.max(top, room.y);
				boolean cellar = cx == -38 && cz == 38;
				if (u == cx && v - cz >= -2 && v - cz <= -1 && h >= (cellar ? -10 : 1) && h <= top + 1) {
					if (v - cz == -2) return Blocks.TUFF_BRICKS.defaultBlockState();
					if (cellar && h == 0) return Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, rotate(Direction.SOUTH, site.rotation));
					return u == cx && v - cz == -1 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, rotate(Direction.SOUTH, site.rotation)) : Blocks.CAVE_AIR.defaultBlockState();
				}
			}
			for (var room : site.rooms) {
				int dx = u - room.x, dz = v - room.z, dy = h - room.y, r = room.radius;
				if (Math.abs(dx) > r || Math.abs(dz) > r || dy < -3 || dy > 8) continue;
				if (dy < 0) return Math.abs(dx) <= r - 1 && Math.abs(dz) <= r - 1 ? Blocks.POLISHED_TUFF.defaultBlockState() : null;
				boolean wall = Math.abs(dx) == r || Math.abs(dz) == r;
				if (dy == 0) return Blocks.TUFF_BRICKS.defaultBlockState();
				if (dy == 8) {
					if (room.ruined && (hash(site.seed, u / 3, h, v / 3) & 7) < 3) return Blocks.CAVE_AIR.defaultBlockState();
					return room.kind == RoomKind.BOTANY && Math.abs(dx) < r - 1 && Math.abs(dz) < r - 1 ? Blocks.GLASS.defaultBlockState() : Blocks.CUT_COPPER.waxed().unaffected().defaultBlockState();
				}
				if (wall) {
					boolean door = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && dy <= 3 && room.kind != RoomKind.VAULT
							&& (site.kind != Kind.WATCHTOWER || room.y == 0 || room.y == 88 && dx == r && Math.abs(dz) <= 1);
					if (door) return Blocks.CAVE_AIR.defaultBlockState();
					if (room.kind == RoomKind.VAULT && dy < 4 && Math.abs(dx) <= 1 && dz == -r) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
					if (room.ruined && dy > 3 && (hash(site.seed + 1, u, h / 3, v) & 7) == 0) return Blocks.CAVE_AIR.defaultBlockState();
					if (dy >= 3 && dy <= 5 && (Math.abs(dx) <= r - 3 || Math.abs(dz) <= r - 3)) return Blocks.GLASS_PANE.defaultBlockState();
					return Math.floorMod(u + v + h, 7) == 0 ? Blocks.CHISELED_TUFF_BRICKS.defaultBlockState() : Blocks.TUFF_BRICKS.defaultBlockState();
				}
				// Functional redstone controls, specimen beds, shelves, workbenches and reactor cores.
				if (dy == 7 && dx == 0 && dz == 0) return Blocks.SEA_LANTERN.defaultBlockState();
				if (room.kind == RoomKind.REACTOR && dy == 1 && dz == 0) {
					if (dx == 3) return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
					if (dx == 4) return Blocks.REDSTONE_WIRE.defaultBlockState();
					if (dx == 5) return Blocks.REDSTONE_LAMP.defaultBlockState();
				}
				if (room.kind == RoomKind.REACTOR && Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy <= 4)
					return dy == 4 ? Blocks.REDSTONE_LAMP.defaultBlockState() : dy == 3 ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.COPPER_BLOCK.waxed().unaffected().defaultBlockState();
				if (room.kind == RoomKind.BOTANY && Math.abs(dz) == r - 2 && Math.abs(dx) < r - 2 && dy <= 2)
					return dy == 1 ? UniverseBotany.block(profile.geometry(), "soil").defaultBlockState() : UniverseBotany.block(profile.geometry(), "plant").defaultBlockState().setValue(UniverseCropBlock.AGE, 3);
				if (room.kind == RoomKind.ARCHIVE && dz == r - 1 && dy <= 3) return Blocks.BOOKSHELF.defaultBlockState();
				if (room.kind == RoomKind.WORKSHOP && dz == r - 2 && dy == 1 && Math.abs(dx) <= 3)
					return dx == 0 ? Blocks.CRAFTING_TABLE.defaultBlockState() : dx == 2 ? Blocks.BLAST_FURNACE.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState();
				if (room.kind == RoomKind.DORMITORY && Math.abs(dx) == r - 2 && dy == 1 && Math.abs(dz) < r - 3) return UniverseBotany.block(profile.geometry(), "slab").defaultBlockState();
				return Blocks.CAVE_AIR.defaultBlockState();
			}
			for (var path : site.passages) {
				boolean alongX = path.z0 == path.z1;
				int along = alongX ? u : v, across = alongX ? v - path.z0 : u - path.x0;
				int from = alongX ? path.x0 : path.z0, to = alongX ? path.x1 : path.z1;
				int dy = h - path.y;
				if (along < Math.min(from, to) || along > Math.max(from, to) || Math.abs(across) > 2 || dy < 0 || dy > 5) continue;
				if (dy == 0 || dy == 5) return Blocks.TUFF_BRICKS.defaultBlockState();
				if (Math.abs(across) == 2 && dy == 1) return Blocks.IRON_BARS.defaultBlockState();
				return Blocks.CAVE_AIR.defaultBlockState();
			}
			if (site.kind == Kind.RING_MACHINE) {
				double r = Math.hypot(u, h - 36);
				if (Math.abs(v) <= 3 && r >= 37 && r <= 44) return (Math.floorMod(u + h, 13) < 2 ? Blocks.SEA_LANTERN : Blocks.COPPER_BLOCK.waxed().oxidized()).defaultBlockState();
				if (Math.abs(u) <= 3 && Math.hypot(v, h - 36) >= 37 && Math.hypot(v, h - 36) <= 44) return Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
			}
		}
		return null;
	}
	private static Direction rotate(Direction facing, int rotation) { for (int n = 0; n < rotation; n++) facing = facing.getClockWise(); return facing; }
	public void decorate(WorldGenLevel level, ChunkAccess chunk) {
		for (var site : forChunk(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ())) {
			for (var treasure : site.treasures) {
				BlockPos pos = treasure.pos; if (!inside(chunk, pos) || !chunk.getBlockState(pos).is(Blocks.CHEST)) continue;
				var tag = new CompoundTag(); tag.putString("id", "minecraft:chest");
				tag.putInt("x", pos.getX()); tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ());
				tag.putString("LootTable", "hbk:chests/universe_" + (treasure.secret ? "secret" : site.kind.name().toLowerCase(Locale.ROOT)) + "_" + UniverseMaterials.FAMILIES.get(profile.geometry()));
				tag.putLong("LootTableSeed", hash(site.seed, pos.getX(), pos.getY(), pos.getZ())); chunk.setBlockEntityNbt(tag);
			}
			for (BlockPos pos : site.nests) {
				if (!inside(chunk, pos) || !chunk.getBlockState(pos).is(Blocks.SPAWNER)) continue;
				var spawner = new SpawnerBlockEntity(pos, chunk.getBlockState(pos));
				spawner.setEntityId(site.kind == Kind.UNDERGROUND_LAB ? EntityTypes.CAVE_SPIDER : site.kind == Kind.WATCHTOWER ? EntityTypes.STRAY : EntityTypes.HUSK, RandomSource.create(site.seed));
				chunk.setBlockEntityNbt(spawner.saveWithFullMetadata(level.registryAccess()));
			}
		}
	}
	private static boolean inside(ChunkAccess chunk, BlockPos pos) { return chunk.getPos().x() == Math.floorDiv(pos.getX(), 16) && chunk.getPos().z() == Math.floorDiv(pos.getZ(), 16); }
}
