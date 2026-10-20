package dev.kirill.hbk.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.AnomalyZones;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

/** Animated surface corruption on ordinary blocks: one bounded mesh, no world edits or entities. */
public final class AnomalyGlitchRenderer {
	private static final int RADIUS = 32;
	private static final int MAX_FACES = 480;
	private static final int REFRESH_TICKS = 8;
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final RenderPipeline PIPELINE = pipeline();
	private static GpuBuffer vertices;
	private static int indexCount;
	private static ClientLevel cachedLevel;
	private static Object cachedModels;
	private static long cachedSeed;
	private static long builtAt = Long.MIN_VALUE;
	private static BlockPos origin = BlockPos.ZERO;

	private AnomalyGlitchRenderer() {}

	private static RenderPipeline pipeline() {
		var base = RenderPipelines.CELESTIAL;
		var builder = RenderPipeline.builder().withLocation(HbkMod.id("pipeline/anomaly_glitch"))
				.withVertexShader(HbkMod.id("core/anomaly_glitch"))
				.withFragmentShader(HbkMod.id("core/anomaly_glitch"))
				.withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withCull(false).withDepthStencilState(Optional.of(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)))
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
		for (var layout : base.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
		return builder.build();
	}

	public static void render(LevelRenderContext context) {
		var client = Minecraft.getInstance();
		var level = client.level;
		Long seed = AnomalyZones.clientSeed();
		var camera = context.levelState().cameraRenderState;
		if (level == null || seed == null || !level.dimension().equals(Level.OVERWORLD)
				|| !nearZone(seed, camera.pos)) {
			clear();
			return;
		}
		long tick = context.levelState().gameTime;
		var models = client.getModelManager().getBlockStateModelSet();
		if (level != cachedLevel || models != cachedModels || seed != cachedSeed
				|| tick < builtAt || tick - builtAt >= REFRESH_TICKS
				|| origin.distToCenterSqr(camera.pos) > 16) {
			rebuild(client, seed, camera.blockPos);
			cachedLevel = level;
			cachedModels = models;
			cachedSeed = seed;
			builtAt = tick;
		}
		if (vertices == null || indexCount == 0) return;

		var view = new Matrix4f(camera.viewRotationMatrix).translate((float) (origin.getX() - camera.pos.x),
				(float) (origin.getY() - camera.pos.y), (float) (origin.getZ() - camera.pos.z));
		// The shader uses the fourth component as its animation clock; it never multiplies texture alpha by it.
		var transforms = RenderSystem.getDynamicUniforms().writeTransform(view, new Vector4f(1, 1, 1, (tick % 72000) / 20.0f));
		var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		var indexBuffer = indices.getBuffer(indexCount);
		// AtlasManager expects the definition ID; sprite.atlasLocation() below uses the texture path.
		var atlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		var target = context.gameRenderer().mainRenderTarget();
		try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "HBK animated block glitches", target.getColorTextureView(), Optional.empty(),
				target.getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
			pass.setVertexBuffer(0, vertices.slice());
			pass.setIndexBuffer(indexBuffer, indices.type());
			pass.drawIndexed(indexCount, 1, 0, 0, 0);
		}
	}

	private static boolean nearZone(long seed, Vec3 camera) {
		int cellX = Math.floorDiv((int) Math.floor(camera.x), AnomalyZones.SPACING);
		int cellZ = Math.floorDiv((int) Math.floor(camera.z), AnomalyZones.SPACING);
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
			for (var zone : AnomalyZones.zonesInCell(seed, cellX + dx, cellZ + dz)) {
				if (zone.horizontalDistance(camera.x, camera.z) <= RADIUS) return true;
			}
		return false;
	}

	private static void rebuild(Minecraft client, long seed, BlockPos center) {
		closeBuffer();
		origin = center.immutable();
		var level = client.level;
		var models = client.getModelManager().getBlockStateModelSet();
		var format = DefaultVertexFormat.ENTITY;
		var pos = new BlockPos.MutableBlockPos();
		var neighbor = new BlockPos.MutableBlockPos();
		int gridX = Math.floorDiv(center.getX(), 2), gridY = Math.floorDiv(center.getY(), 2), gridZ = Math.floorDiv(center.getZ(), 2);
		int faces = 0;
		try (var bytes = ByteBufferBuilder.exactlySized(MAX_FACES * 4 * format.getVertexSize())) {
			var builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
			// Visit nearby cells first, sampling one stable position per 2x2x2 cell. Never load a chunk.
			search: for (int shell = 0; shell <= RADIUS / 2; shell++) {
				for (int gx = -shell; gx <= shell; gx++) for (int gy = -shell; gy <= shell; gy++) for (int gz = -shell; gz <= shell; gz++) {
					if (Math.max(Math.abs(gx), Math.max(Math.abs(gy), Math.abs(gz))) != shell) continue;
					int cx = gridX + gx, cy = gridY + gy, cz = gridZ + gz;
					long hash = hash(seed ^ (long) cx * 341873128712L ^ (long) cy * 42317861L ^ (long) cz * 132897987541L);
					if ((hash & 3) != 0) continue;
					int x = cx * 2 + (int) ((hash >>> 2) & 1), y = cy * 2 + (int) ((hash >>> 3) & 1), z = cz * 2 + (int) ((hash >>> 4) & 1);
					pos.set(x, y, z);
					if (pos.distSqr(center) > RADIUS * RADIUS || level.isOutsideBuildHeight(y)
							|| !level.hasChunkAt(pos) || AnomalyZones.at(seed, x, z) == null) continue;
					var state = level.getBlockState(pos);
					if (!state.isSolidRender() || state.hasBlockEntity() || state.is(Blocks.BEDROCK)) continue;
					var sprite = models.getParticleMaterial(state).sprite();
					if (!sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) continue;
					for (var direction : DIRECTIONS) {
						neighbor.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
						if (!level.hasChunkAt(neighbor) || level.getBlockState(neighbor).isSolidRender()) continue;
						face(builder, x - center.getX(), y - center.getY(), z - center.getZ(), direction, sprite, (int) hash);
						if (++faces == MAX_FACES) break search;
					}
				}
			}
			try (var mesh = builder.build()) {
				if (mesh == null) return;
				vertices = RenderSystem.getDevice().createBuffer(() -> "HBK cached glitch faces", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
				indexCount = faces * 6;
			}
		}
	}

	private static void face(BufferBuilder builder, int x, int y, int z, Direction direction, TextureAtlasSprite sprite, int seed) {
		float ox = x, oy = y, oz = z, ux = 0, uy = 0, uz = 0, vx = 0, vy = 0, vz = 0;
		switch (direction) {
			case UP -> { oy++; ux = 1; vz = 1; }
			case DOWN -> { oz++; ux = 1; vz = -1; }
			case NORTH -> { ox++; oy++; ux = -1; vy = -1; }
			case SOUTH -> { oy++; oz++; ux = 1; vy = -1; }
			case WEST -> { oy++; uz = 1; vy = -1; }
			case EAST -> { ox++; oy++; oz++; uz = -1; vy = -1; }
		}
		ox += direction.getStepX() * 0.003f;
		oy += direction.getStepY() * 0.003f;
		oz += direction.getStepZ() * 0.003f;
		int minU = Math.round(sprite.getU0() * 32767), minV = Math.round(sprite.getV0() * 32767);
		int maxU = Math.round(sprite.getU1() * 32767), maxV = Math.round(sprite.getV1() * 32767);
		for (int corner = 0; corner < 4; corner++) {
			float u = corner == 1 || corner == 2 ? 1 : 0, v = corner >= 2 ? 1 : 0;
			builder.addVertex(ox + ux * u + vx * v, oy + uy * u + vy * v, oz + uz * u + vz * v)
					.setColor(0xFF000000 | seed & 0xFFFFFF).setUv(u, v).setUv1(minU, minV).setUv2(maxU, maxV)
					.setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
		}
	}

	private static long hash(long value) {
		value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
		value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
		return value ^ value >>> 31;
	}

	private static void closeBuffer() {
		if (vertices != null) { vertices.close(); vertices = null; }
		indexCount = 0;
	}

	public static void clear() {
		closeBuffer();
		cachedLevel = null;
		cachedModels = null;
		builtAt = Long.MIN_VALUE;
	}
}
