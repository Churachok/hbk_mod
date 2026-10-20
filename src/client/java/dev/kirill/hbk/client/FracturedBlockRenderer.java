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
import dev.kirill.hbk.block.FracturedBlock;
import dev.kirill.hbk.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

/** GPU-animated visual copies only: no duplicate entities, block states, hitboxes or drops. */
public final class FracturedBlockRenderer {
	private static final RenderPipeline PIPELINE = pipeline();
	private static GpuBuffer vertices;
	private static int indexCount;
	private static long signature = Long.MIN_VALUE;
	private static ClientLevel level;
	private static BlockPos origin = BlockPos.ZERO;
	private FracturedBlockRenderer() {}

	private static RenderPipeline pipeline() {
		var base = RenderPipelines.CELESTIAL;
		var builder = RenderPipeline.builder().withLocation(HbkMod.id("pipeline/fractured_block"))
				.withVertexShader(HbkMod.id("core/fractured_block")).withFragmentShader(HbkMod.id("core/fractured_block"))
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withCull(false).withDepthStencilState(Optional.of(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)))
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
		for (var layout : base.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
		return builder.build();
	}

	public static void render(LevelRenderContext context) {
		var client = Minecraft.getInstance();
		if (client.level == null || client.player == null || !client.player.isAlive()) { clear(); return; }
		long hash = 1;
		for (var pos : FracturedBlockClient.blocks()) {
			if (!client.level.hasChunkAt(pos)) continue;
			var state = client.level.getBlockState(pos);
			if (state.is(ModBlocks.FRACTURED_BLOCK)) hash = hash * 31 + pos.asLong() * 3 + state.getValue(FracturedBlock.PHASE).ordinal();
		}
		if (level != client.level || hash != signature) {
			rebuild(client);
			signature = hash;
			level = client.level;
		}
		if (vertices == null || indexCount == 0) return;
		var camera = context.levelState().cameraRenderState;
		var view = new Matrix4f(camera.viewRotationMatrix).translate((float) (origin.getX() - camera.pos.x),
				(float) (origin.getY() - camera.pos.y), (float) (origin.getZ() - camera.pos.z));
		float time = (context.levelState().gameTime % 72000) / 20.0f;
		var transforms = RenderSystem.getDynamicUniforms().writeTransform(view, new Vector4f(1, 1, 1, time));
		var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		var indexBuffer = indices.getBuffer(indexCount);
		var texture = client.getTextureManager().getTexture(HbkMod.id("textures/block/fractured_block.png"));
		var target = context.gameRenderer().mainRenderTarget();
		try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "HBK fractured block light and spatial echoes", target.getColorTextureView(), Optional.empty(),
				target.getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
			pass.setVertexBuffer(0, vertices.slice());
			pass.setIndexBuffer(indexBuffer, indices.type());
			pass.drawIndexed(indexCount, 1, 0, 0, 0);
		}
	}

	private static void rebuild(Minecraft client) {
		closeBuffer();
		origin = client.player.blockPosition().immutable();
		var format = DefaultVertexFormat.POSITION_TEX_COLOR;
		int faces = 0;
		try (var bytes = ByteBufferBuilder.exactlySized(FracturedBlockClient.MAX_BLOCKS * 3 * 6 * 4 * format.getVertexSize())) {
			var builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
			for (var pos : FracturedBlockClient.blocks()) {
				if (!client.level.hasChunkAt(pos)) continue;
				var state = client.level.getBlockState(pos);
				if (!state.is(ModBlocks.FRACTURED_BLOCK)) continue;
				int phase = state.getValue(FracturedBlock.PHASE).ordinal();
				int seed = pos.hashCode() & 255;
				for (int copy = 0; copy < (phase == 2 ? 3 : 1); copy++) {
					int alpha = copy == 0 ? 190 : copy == 1 ? 80 : 50;
					int color = alpha << 24 | seed << 16 | phase * 127 << 8 | copy * 127;
					for (var direction : Direction.values()) {
						face(builder, pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(), direction, color);
						faces++;
					}
				}
			}
			try (var mesh = builder.build()) {
				if (mesh == null) return;
				vertices = RenderSystem.getDevice().createBuffer(() -> "HBK cached fractured block echoes", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
				indexCount = faces * 6;
			}
		}
	}

	private static void face(BufferBuilder builder, float x, float y, float z, Direction direction, int color) {
		float ox = x, oy = y, oz = z, ux = 0, uy = 0, uz = 0, vx = 0, vy = 0, vz = 0;
		switch (direction) {
			case UP -> { oy++; ux = 1; vz = 1; }
			case DOWN -> { oz++; ux = 1; vz = -1; }
			case NORTH -> { ox++; oy++; ux = -1; vy = -1; }
			case SOUTH -> { oy++; oz++; ux = 1; vy = -1; }
			case WEST -> { oy++; uz = 1; vy = -1; }
			case EAST -> { ox++; oy++; oz++; uz = -1; vy = -1; }
		}
		ox += direction.getStepX() * 0.004f;
		oy += direction.getStepY() * 0.004f;
		oz += direction.getStepZ() * 0.004f;
		for (int corner = 0; corner < 4; corner++) {
			float u = corner == 1 || corner == 2 ? 1 : 0, v = corner >= 2 ? 1 : 0;
			builder.addVertex(ox + ux * u + vx * v, oy + uy * u + vy * v, oz + uz * u + vz * v).setUv(u, v).setColor(color);
		}
	}

	private static void closeBuffer() {
		if (vertices != null) { vertices.close(); vertices = null; }
		indexCount = 0;
	}

	public static void clear() {
		closeBuffer();
		level = null;
		signature = Long.MIN_VALUE;
	}
}
