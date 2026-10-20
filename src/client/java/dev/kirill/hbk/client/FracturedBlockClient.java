package dev.kirill.hbk.client;

import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Palette-filtered, bounded search shared by the lens effect and the decorative echo renderer. */
public final class FracturedBlockClient {
	public static final int RADIUS = 24;
	public static final int MAX_BLOCKS = 96;
	private static ClientLevel level;
	private static List<BlockPos> blocks = List.of();
	private static BlockPos origin = BlockPos.ZERO;
	private static int scanTicks;
	private static float strength;
	private static float previousStrength;
	private FracturedBlockClient() {}

	public static List<BlockPos> blocks() { return blocks; }

	public static void clear() {
		level = null;
		blocks = List.of();
		scanTicks = 0;
		strength = previousStrength = 0;
		FracturedBlockRenderer.clear();
	}

	public static float strength(float partialTick) {
		var client = Minecraft.getInstance();
		if (client.level != level || client.player == null || !client.player.isAlive()) return 0;
		return previousStrength + (strength - previousStrength) * Math.clamp(partialTick, 0, 1);
	}

	public static void tick(Minecraft client) {
		if (client.level != level) { clear(); level = client.level; }
		if (client.level == null || client.player == null || !client.player.isAlive()) { clear(); return; }
		if (client.isPaused()) return;
		var center = client.player.blockPosition();
		if (scanTicks-- <= 0 || origin.distSqr(center) > 16) {
			scan(client, center);
			scanTicks = 9;
			origin = center.immutable();
		}
		var eye = client.player.getEyePosition();
		double distance = 12;
		int particles = 0;
		for (var pos : blocks) {
			if (!client.level.hasChunkAt(pos) || !client.level.getBlockState(pos).is(ModBlocks.FRACTURED_BLOCK)) continue;
			double dx = Math.max(0, Math.max(pos.getX() - eye.x, eye.x - pos.getX() - 1));
			double dy = Math.max(0, Math.max(pos.getY() - eye.y, eye.y - pos.getY() - 1));
			double dz = Math.max(0, Math.max(pos.getZ() - eye.z, eye.z - pos.getZ() - 1));
			distance = Math.min(distance, Math.sqrt(dx * dx + dy * dy + dz * dz));
			if (scanTicks % 4 == 0 && particles++ < 6) {
				var random = client.level.getRandom();
				client.level.addParticle(new DustParticleOptions(0x4ADFFF, 0.65f),
						pos.getX() + random.nextDouble(), pos.getY() + 1 + random.nextDouble() * 0.5,
						pos.getZ() + random.nextDouble(), 0, 0.015, 0);
			}
		}
		float progress = (float) (distance / 12);
		float target = 1 - progress * progress * (3 - 2 * progress);
		previousStrength = strength;
		strength += (target - strength) * 0.25f;
		if (target == 0 && strength < 0.001f) strength = 0;
	}

	private static void scan(Minecraft client, BlockPos center) {
		Comparator<BlockPos> byDistance = Comparator.comparingDouble(p -> p.distSqr(center));
		var nearest = new PriorityQueue<>(MAX_BLOCKS, byDistance.reversed());
		var world = client.level;
		int minX = center.getX() - RADIUS, maxX = center.getX() + RADIUS;
		int minZ = center.getZ() - RADIUS, maxZ = center.getZ() + RADIUS;
		int minY = Math.max(world.getMinY(), center.getY() - RADIUS);
		int maxY = Math.min(world.getMaxY() - 1, center.getY() + RADIUS);
		for (int cx = minX >> 4; cx <= maxX >> 4; cx++) for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
			var chunk = world.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
			if (chunk == null) continue;
			for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
				var section = chunk.getSection(world.getSectionIndexFromSectionY(sy));
				if (!section.maybeHas(state -> state.is(ModBlocks.FRACTURED_BLOCK))) continue;
				for (int x = Math.max(minX, cx * 16); x <= Math.min(maxX, cx * 16 + 15); x++)
					for (int z = Math.max(minZ, cz * 16); z <= Math.min(maxZ, cz * 16 + 15); z++)
						for (int y = Math.max(minY, sy * 16); y <= Math.min(maxY, sy * 16 + 15); y++) {
							if (!section.getBlockState(x & 15, y & 15, z & 15).is(ModBlocks.FRACTURED_BLOCK)) continue;
							var pos = new BlockPos(x, y, z);
							if (pos.distSqr(center) > RADIUS * RADIUS) continue;
							if (nearest.size() < MAX_BLOCKS) nearest.add(pos);
							else if (byDistance.compare(pos, nearest.peek()) < 0) { nearest.poll(); nearest.add(pos); }
						}
			}
		}
		blocks = nearest.stream().sorted(byDistance).toList();
	}
}
