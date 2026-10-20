package dev.kirill.hbk.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.AnomalyZones;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

/** A world-space, double-sided plane; its UVs cover the whole zone once. */
public final class AnomalyCeilingRenderer {
	private static final RenderPipeline PIPELINE = pipeline();
	private AnomalyCeilingRenderer() {}

	private static RenderPipeline pipeline() {
		var base = RenderPipelines.CELESTIAL;
		var builder = RenderPipeline.builder().withLocation(HbkMod.id("pipeline/anomaly_ceiling"))
				.withVertexShader(base.getVertexShader()).withFragmentShader(base.getFragmentShader())
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withCull(false).withDepthStencilState(Optional.of(new com.mojang.blaze3d.pipeline.DepthStencilState(
						com.mojang.blaze3d.pipeline.DepthStencilState.DEFAULT.depthTest(), false)))
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
		for (var layout : base.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
		return builder.build();
	}

	public static void render(LevelRenderContext context) {
		var client = Minecraft.getInstance();
		Long seed = AnomalyZones.clientSeed();
		if (seed == null || client.level == null || !client.level.dimension().equals(Level.OVERWORLD)) return;
		var camera = context.levelState().cameraRenderState;
		int cellX = Math.floorDiv((int) Math.floor(camera.pos.x), AnomalyZones.SPACING);
		int cellZ = Math.floorDiv((int) Math.floor(camera.pos.z), AnomalyZones.SPACING);
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
			for (var zone : AnomalyZones.zonesInCell(seed, cellX + dx, cellZ + dz)) {
				double centerX = zone.centerX(), centerZ = zone.centerZ();
				if (Math.abs(centerX - camera.pos.x) > 512 || Math.abs(centerZ - camera.pos.z) > 512) continue;
				float x = (float) (zone.minX() - camera.pos.x), z = (float) (zone.minZ() - camera.pos.z);
				float y = (float) (AnomalyZones.CEILING_Y - camera.pos.y), size = zone.size();
				int quads = zone.circular() ? 64 : 1;
				var format = DefaultVertexFormat.POSITION_TEX;
				try (var bytes = ByteBufferBuilder.exactlySized(quads * 4 * format.getVertexSize())) {
					var builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
					if (zone.circular()) {
						for (int i = 0; i < quads; i++) {
							double a = i * Math.PI * 2 / quads, b = (i + 1) * Math.PI * 2 / quads;
							float u0 = 0.5f + (float) Math.cos(a) * 0.5f, v0 = 0.5f + (float) Math.sin(a) * 0.5f;
							float u1 = 0.5f + (float) Math.cos(b) * 0.5f, v1 = 0.5f + (float) Math.sin(b) * 0.5f;
							builder.addVertex(x + size / 2, y, z + size / 2).setUv(0.5f, 0.5f);
							builder.addVertex(x + size * u0, y, z + size * v0).setUv(u0, v0);
							builder.addVertex(x + size * u1, y, z + size * v1).setUv(u1, v1);
							builder.addVertex(x + size / 2, y, z + size / 2).setUv(0.5f, 0.5f);
						}
					} else {
						builder.addVertex(x, y, z).setUv(0, 0);
						builder.addVertex(x + size, y, z).setUv(1, 0);
						builder.addVertex(x + size, y, z + size).setUv(1, 1);
						builder.addVertex(x, y, z + size).setUv(0, 1);
					}
					try (var mesh = builder.buildOrThrow(); var vertices = RenderSystem.getDevice().createBuffer(
							() -> "HBK anomaly ceiling", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())) {
						var transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(camera.viewRotationMatrix), new Vector4f(1, 1, 1, 1));
						var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
						var indexBuffer = indices.getBuffer(quads * 6);
						var texture = client.getTextureManager().getTexture(HbkMod.id("textures/environment/anomaly_641.png"));
						var target = context.gameRenderer().mainRenderTarget();
						try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
								() -> "HBK anomaly ceiling", target.getColorTextureView(), Optional.empty(),
								target.getDepthTextureView(), OptionalDouble.empty())) {
							pass.setPipeline(PIPELINE);
							RenderSystem.bindDefaultUniforms(pass);
							pass.setUniform("DynamicTransforms", transforms);
							pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
							pass.setVertexBuffer(0, vertices.slice());
							pass.setIndexBuffer(indexBuffer, indices.type());
							pass.drawIndexed(quads * 6, 1, 0, 0, 0);
						}
					}
				}
			}
	}
}
