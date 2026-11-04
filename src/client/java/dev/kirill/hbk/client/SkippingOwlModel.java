package dev.kirill.hbk.client;

import dev.kirill.hbk.world.SkippingOwlEncounter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A meshed wide-eyed owl and an articulated U-shaped cord, with no custom vertex pipeline. */
public final class SkippingOwlModel extends EntityModel<SkippingOwlRenderState> {
	public static final int ROPE_SEGMENTS = 24;
	public static final float ROPE_HALF_WIDTH = 9.0f;
	public static final float ROPE_DEPTH = 13.5f;
	public static final float HANDLE_Y = 15.0f;
	public static final float HOP_PIXELS = 5.2f;
	private static final float CORD = 0.35f;
	private final ModelPart performance;
	private final ModelPart rope;
	private final ModelPart leftBrow;
	private final ModelPart rightBrow;
	private final ModelPart leftPupil;
	private final ModelPart rightPupil;
	private final ModelPart leftGlint;
	private final ModelPart rightGlint;

	public SkippingOwlModel(ModelPart root) {
		super(root);
		this.performance = root.getChild("performance");
		this.rope = this.performance.getChild("rope");
		this.leftBrow = this.performance.getChild("left_brow");
		this.rightBrow = this.performance.getChild("right_brow");
		this.leftPupil = this.performance.getChild("left_pupil");
		this.rightPupil = this.performance.getChild("right_pupil");
		this.leftGlint = this.performance.getChild("left_glint");
		this.rightGlint = this.performance.getChild("right_glint");
	}

	@Override
	public void setupAnim(SkippingOwlRenderState state) {
		this.resetPose();
		float angle = Mth.TWO_PI * (state.waitingForPlayer ? state.idleAge / 20
				: SkippingOwlEncounter.jumpCycles(state.performanceAge));
		float lift = Math.max(0, Mth.cos(angle));
		// One shared visual rig keeps feet, wings, handles and cord attached.
		// At the lowest cord phase the owl rises .325 blocks; physics never hops.
		this.performance.y = -HOP_PIXELS * lift * lift;
		this.rope.xRot = angle;
		float threat = state.waitingForPlayer ? 0 : Mth.clamp((state.performanceAge - SkippingOwlEncounter.OMEN_TICKS * 0.5f)
				/ (SkippingOwlEncounter.OMEN_TICKS * 0.5f), 0, 1);
		threat = threat * threat * (3 - 2 * threat);
		this.leftBrow.visible = this.rightBrow.visible = threat > 0.001f;
		this.leftBrow.yScale = this.rightBrow.yScale = 0.05f + threat * 0.95f;
		this.leftBrow.zRot = threat * 0.28f;
		this.rightBrow.zRot = -threat * 0.28f;
		this.leftPupil.xScale = this.rightPupil.xScale = 1 - threat * 0.45f;
		// These cubes use global rig coordinates, so compensate scale about each
		// eye's own center instead of pulling both pupils toward the beak.
		this.leftPupil.x = -2.65f * threat * 0.45f;
		this.rightPupil.x = 2.65f * threat * 0.45f;
		this.leftGlint.yScale = this.rightGlint.yScale = 1 - threat * 0.55f;
		this.leftGlint.y = this.rightGlint.y = 9.2f * threat * 0.55f;
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var rig = mesh.getRoot().addOrReplaceChild("performance", CubeListBuilder.create(), PartPose.ZERO);
		box(rig, "body", -4.5f, 13, -3.5f, 9, 10, 7, Material.PURPLE);
		box(rig, "breast", -3.3f, 14.1f, -4.05f, 6.6f, 7.5f, 1.1f, Material.PALE);
		box(rig, "breast_tip", -2.5f, 21.3f, -3.9f, 5, 1.5f, 0.8f, Material.PALE);
		box(rig, "head", -5.5f, 6.5f, -4, 11, 8, 8, Material.PURPLE);
		box(rig, "left_tuft", -4.8f, 4, -0.8f, 2.2f, 4, 2.5f, Material.PURPLE);
		box(rig, "right_tuft", 2.6f, 4, -0.8f, 2.2f, 4, 2.5f, Material.PURPLE);
		box(rig, "left_tuft_tip", -4.9f, 4.1f, -0.6f, 1.3f, 2.3f, 1.8f, Material.PURPLE);
		box(rig, "right_tuft_tip", 3.6f, 4.1f, -0.6f, 1.3f, 2.3f, 1.8f, Material.PURPLE);
		for (int side : new int[] {-1, 1}) {
			String name = side < 0 ? "left" : "right";
			float center = side * 2.65f;
			// Physical stepped discs, irises, pupils and glints; the atlas has no drawn face.
			box(rig, name + "_eye_rim", center - 2.6f, 7.1f, -4.35f, 5.2f, 6.0f, 0.7f, Material.DARK);
			box(rig, name + "_eye_disc", center - 2.4f, 7.7f, -4.9f, 4.8f, 4.8f, 0.8f, Material.PALE);
			box(rig, name + "_eye_disc_vertical", center - 1.8f, 7.25f, -4.85f, 3.6f, 5.7f, 0.7f, Material.PALE);
			box(rig, name + "_iris", center - 1.6f, 8.25f, -5.3f, 3.2f, 4.0f, 0.7f, Material.GOLD);
			box(rig, name + "_pupil", center - 0.9f, 8.55f, -5.65f, 1.8f, 3.3f, 0.65f, Material.DARK);
			box(rig, name + "_glint", center - 0.65f, 8.75f, -5.85f, 0.7f, 0.9f, 0.3f, Material.PALE);
			rig.addOrReplaceChild(name + "_brow", cube(Material.DARK, -2.5f, -0.45f, -0.22f, 5, 0.9f, 0.5f),
					PartPose.offset(center, 8.1f, -5.93f));
			box(rig, name + "_foot", side * 2.55f - 1.5f, 23, -3, 3, 1, 4.2f, Material.GOLD);
			for (int toe = 0; toe < 3; toe++) {
				box(rig, name + "_toe_" + toe, side * 2.55f - 1.25f + toe * 0.9f,
						23.3f, -3.65f, 0.65f, 0.7f, 1.1f, Material.GOLD);
			}
			var wing = rig.addOrReplaceChild(name + "_wing", CubeListBuilder.create(), PartPose.ZERO);
			box(wing, "upper", side < 0 ? -7.0f : 4.0f, 13.8f, -0.6f, 3, 3.7f, 3, Material.PURPLE);
			box(wing, "tip", side < 0 ? -9.4f : 6.4f, 14.1f, -0.7f, 3, 2.6f, 2.7f, Material.PURPLE);
			box(rig, name + "_handle", side * ROPE_HALF_WIDTH - 0.6f, HANDLE_Y - 0.7f, -1.5f,
					1.2f, 1.4f, 3, Material.GOLD);
		}
		box(rig, "beak", -1.0f, 12.1f, -5.8f, 2, 1.9f, 1.6f, Material.GOLD);
		box(rig, "beak_tip", -0.5f, 13.4f, -6.1f, 1, 1.2f, 1.1f, Material.GOLD);
		box(rig, "tail", -2.8f, 19.5f, 3.1f, 5.6f, 3.5f, 2.8f, Material.PURPLE);

		var rope = rig.addOrReplaceChild("rope", CubeListBuilder.create(), PartPose.offset(0, HANDLE_Y, 0));
		for (int index = 0; index < ROPE_SEGMENTS; index++) {
			float a = index * Mth.PI / ROPE_SEGMENTS;
			float b = (index + 1) * Mth.PI / ROPE_SEGMENTS;
			float x = -ROPE_HALF_WIDTH * Mth.cos(a), y = ROPE_DEPTH * Mth.sin(a);
			float dx = -ROPE_HALF_WIDTH * Mth.cos(b) - x, dy = ROPE_DEPTH * Mth.sin(b) - y;
			float length = Mth.sqrt(dx * dx + dy * dy);
			rope.addOrReplaceChild("segment_" + index, cube(Material.DARK, -CORD / 2, 0, -CORD / 2, CORD, length, CORD),
					PartPose.offsetAndRotation(x, y, 0, 0, 0, (float) Math.atan2(-dx, dy)));
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static CubeListBuilder cube(Material material, float x, float y, float z, float w, float h, float d) {
		// Native cuboid unfolding consumes 2*(w+d) by h+d pixels, all within one 64px tile.
		if (2 * (w + d) > 60 || h + d > 60) throw new IllegalArgumentException("Owl UV crosses its atlas quadrant");
		return CubeListBuilder.create().texOffs(material.u + 2, material.v + 2).addBox(x, y, z, w, h, d);
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z, float w, float h, float d, Material material) {
		parent.addOrReplaceChild(name, cube(material, x, y, z, w, h, d), PartPose.ZERO);
	}

	private enum Material {
		PURPLE(0, 0), PALE(64, 0), GOLD(0, 64), DARK(64, 64);
		private final int u, v;
		Material(int u, int v) { this.u = u; this.v = v; }
	}
}
