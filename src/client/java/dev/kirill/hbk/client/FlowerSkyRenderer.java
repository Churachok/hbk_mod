package dev.kirill.hbk.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import dev.kirill.hbk.HbkMod;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

/** Textured celestial quads using the actual vanilla flowers, including resource-pack replacements. */
public final class FlowerSkyRenderer implements AutoCloseable {
	private static final String[] FLOWERS = {"poppy", "dandelion", "oxeye_daisy", "blue_orchid"};
	private static final int FLOWERS_PER_TYPE = 40;
	private static final int INDEX_COUNT = FLOWERS_PER_TYPE * 6;
	private static final RenderPipeline PIPELINE = flowerPipeline();
	private final GpuBuffer[] buffers = new GpuBuffer[FLOWERS.length];

	private static RenderPipeline flowerPipeline() {
		// Sun/moon use additive blending; flowers need alpha blending to retain their real colors.
		var celestial = RenderPipelines.CELESTIAL;
		var builder = RenderPipeline.builder().withLocation(HbkMod.id("pipeline/flower_sky"))
				.withVertexShader(celestial.getVertexShader()).withFragmentShader(celestial.getFragmentShader())
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withCull(false).withDepthStencilState(Optional.ofNullable(celestial.getDepthStencilState()))
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
		for (var layout : celestial.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
		return builder.build();
	}

	private GpuBuffer build(int type) {
		var format = DefaultVertexFormat.POSITION_TEX;
		try (var bytes = ByteBufferBuilder.exactlySized(FLOWERS_PER_TYPE * 4 * format.getVertexSize())) {
			var builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
			var random = new Random(10867L + type);
			for (int i = 0; i < FLOWERS_PER_TYPE; i++) {
				Vector3f normal;
				do {
					normal = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1,
							random.nextFloat() * 2 - 1);
				} while (normal.lengthSquared() < 0.01f || normal.lengthSquared() > 1);
				normal.normalize();
				var reference = Math.abs(normal.y) > 0.98f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
				var right = new Vector3f(normal).cross(reference).normalize();
				var up = new Vector3f(right).cross(normal).normalize();
				float angle = random.nextFloat() * (float) (Math.PI * 2);
				var rotatedRight = new Vector3f(right).mul((float) Math.cos(angle))
						.add(new Vector3f(up).mul((float) Math.sin(angle)));
				var rotatedUp = new Vector3f(up).mul((float) Math.cos(angle))
						.sub(new Vector3f(right).mul((float) Math.sin(angle)));
				float size = 4.0f + random.nextFloat() * 1.5f;
				var center = new Vector3f(normal).mul(100);
				for (int corner = 0; corner < 4; corner++) {
					float x = corner == 0 || corner == 3 ? -1 : 1;
					float y = corner < 2 ? -1 : 1;
					var point = new Vector3f(center).add(new Vector3f(rotatedRight).mul(x * size))
							.add(new Vector3f(rotatedUp).mul(y * size));
					builder.addVertex(point.x, point.y, point.z).setUv((x + 1) / 2, (1 - y) / 2);
				}
			}
			try (var mesh = builder.buildOrThrow()) {
				return RenderSystem.getDevice().createBuffer(() -> "HBK flower sky " + FLOWERS[type],
						GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
			}
		}
	}

	public void render(RenderTarget target, float brightness, PoseStack poseStack) {
		var modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		try {
			modelView.mul(poseStack.last().pose());
			var transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelView),
					new Vector4f(1, 1, 1, brightness));
			var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
			var indexBuffer = indices.getBuffer(INDEX_COUNT);
			for (int type = 0; type < FLOWERS.length; type++) {
				if (this.buffers[type] == null) this.buffers[type] = this.build(type);
				var texture = Minecraft.getInstance().getTextureManager().getTexture(
						Identifier.withDefaultNamespace("textures/block/" + FLOWERS[type] + ".png"));
				try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
						() -> "HBK night flowers", target.getColorTextureView(), Optional.empty(),
						target.getDepthTextureView(), OptionalDouble.empty())) {
					pass.setPipeline(PIPELINE);
					RenderSystem.bindDefaultUniforms(pass);
					pass.setUniform("DynamicTransforms", transforms);
					pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
					pass.setVertexBuffer(0, this.buffers[type].slice());
					pass.setIndexBuffer(indexBuffer, indices.type());
					pass.drawIndexed(INDEX_COUNT, 1, 0, 0, 0);
				}
			}
		} finally {
			modelView.popMatrix();
		}
	}

	@Override
	public void close() {
		for (int i = 0; i < this.buffers.length; i++) {
			if (this.buffers[i] != null) {
				this.buffers[i].close();
				this.buffers[i] = null;
			}
		}
	}
}
