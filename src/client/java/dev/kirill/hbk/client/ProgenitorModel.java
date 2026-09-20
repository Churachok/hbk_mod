package dev.kirill.hbk.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** The body is thirty blocks tall when rendered at the transformation's 16x player scale. */
public final class ProgenitorModel extends EntityModel<AvatarRenderState> {
	public static final ProgenitorModel INSTANCE = new ProgenitorModel(createBodyLayer().bakeRoot());
	private static final int BONE = 0;
	private static final int ORANGE = 80;
	private static final int MAGENTA = 104;

	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart frontMember;

	private ProgenitorModel(ModelPart root) {
		super(root);
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.frontMember = root.getChild("front_member");
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		this.resetPose();
		float stride = Mth.cos(state.walkAnimationPos * 0.55f) * Math.min(0.18f, state.walkAnimationSpeed * 0.16f);
		this.leftArm.xRot = stride;
		this.rightArm.xRot = -stride;
		this.leftLeg.xRot = -stride;
		this.rightLeg.xRot = stride;
		this.frontMember.zRot = Mth.sin(state.ageInTicks * 0.08f) * 0.035f;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Vertebrae rise into one high arch. The topmost dorsal ridge is y=-8,
		// while all limbs end near y=24: 32 model pixels * 16/16 * .9375 = 30 blocks.
		for (int i = 0; i < 25; i++) {
			float t = i / 24.0f;
			float z = -26.0f + 56.0f * t;
			float arch = Mth.sin((float) Math.PI * t);
			float y = 15.0f - 18.0f * arch;
			float breadth = 2.4f + 0.7f * arch;
			box(root, "vertebra_" + i, -breadth / 2, y - 1.4f, z - 1.55f,
					breadth, 2.8f, 3.1f, BONE);
			float ridgeTop = Math.max(-8.0f, y - 4.8f);
			box(root, "ridge_" + i, -0.65f, ridgeTop, z - 0.55f,
					1.3f, y - 1.3f - ridgeTop, 1.1f, BONE);
		}

		// Ribs curve out and down in three articulated pieces; lengths vary subtly.
		for (int i = 3; i < 22; i++) {
			float t = i / 24.0f;
			float z = -26.0f + 56.0f * t;
			float arch = Mth.sin((float) Math.PI * t);
			float spineY = 15.0f - 18.0f * arch;
			float startY = spineY + 1.6f;
			float reach = 3.6f + 6.3f * arch;
			float endY = Math.min(21.0f, startY + 11.5f + 7.0f * arch + (i % 3 - 1) * 0.7f);
			for (int side : new int[]{-1, 1}) {
				String name = "rib_" + i + "_" + (side < 0 ? "left" : "right");
				rodXY(root, name + "_upper", side * 1.1f, startY, z,
						side * reach * 0.52f, startY + 3.2f, 1.35f, BONE);
				rodXY(root, name + "_middle", side * reach * 0.52f, startY + 3.0f, z,
						side * reach, startY + (endY - startY) * 0.56f, 1.25f, BONE);
				rodXY(root, name + "_tip", side * reach, startY + (endY - startY) * 0.54f, z,
						side * (reach + 0.35f), endY, 0.95f, BONE);
			}
		}

		// Compact skull, front lure and orange accents.
		box(root, "skull", -4.4f, 10.0f, -33.0f, 8.8f, 4.2f, 8.0f, BONE);
		box(root, "snout", -2.5f, 12.0f, -35.0f, 5.0f, 2.5f, 3.5f, BONE);
		box(root, "left_horn", -5.5f, 9.4f, -31.5f, 2.0f, 2.3f, 2.3f, ORANGE);
		box(root, "right_horn", 3.5f, 9.4f, -31.5f, 2.0f, 2.3f, 2.3f, ORANGE);
		box(root, "left_eye", -4.5f, 12.0f, -33.2f, 1.3f, 1.3f, 0.6f, ORANGE);
		box(root, "right_eye", 3.2f, 12.0f, -33.2f, 1.3f, 1.3f, 0.6f, ORANGE);
		PartDefinition frontMember = root.addOrReplaceChild("front_member",
				CubeListBuilder.create().texOffs(0, MAGENTA)
						.addBox(-1.4f, 0.0f, -1.4f, 2.8f, 7.8f, 2.8f),
				PartPose.offset(0, 14.1f, -33.0f));
		box(frontMember, "front_tip", -1.05f, 6.8f, -1.05f, 2.1f, 2.0f, 2.1f, MAGENTA);

		// Each short arm has a shoulder, upper arm, orange elbow and blunt forearm.
		for (int side : new int[]{-1, 1}) {
			String armName = side < 0 ? "left_arm" : "right_arm";
			float x = side * 5.2f;
			PartDefinition arm = root.addOrReplaceChild(armName,
					CubeListBuilder.create().texOffs(0, BONE)
							.addBox(-1.25f, 0, -1.25f, 2.5f, 5.5f, 2.5f),
					PartPose.offset(x, 11.0f, -27.5f));
			box(arm, "shoulder", -1.75f, -0.7f, -1.75f, 3.5f, 1.8f, 3.5f, ORANGE);
			PartDefinition forearm = arm.addOrReplaceChild("forearm",
					CubeListBuilder.create().texOffs(0, BONE)
							.addBox(-1.0f, 0, -1.0f, 2.0f, 6.0f, 2.0f),
					PartPose.offset(0, 5.4f, 0));
			box(forearm, "elbow", -1.55f, -0.8f, -1.55f, 3.1f, 1.8f, 3.1f, ORANGE);
		}

		// Pelvis, two simple hind legs and exactly two members hanging down under it.
		box(root, "pelvis", -4.0f, 15.0f, 27.0f, 8.0f, 4.0f, 6.0f, BONE);
		for (int side : new int[]{-1, 1}) {
			String legName = side < 0 ? "left_leg" : "right_leg";
			PartDefinition leg = root.addOrReplaceChild(legName,
					CubeListBuilder.create().texOffs(0, BONE)
							.addBox(-1.1f, 0, -1.2f, 2.2f, 4.0f, 2.4f),
					PartPose.offset(side * 3.2f, 17.0f, 30.0f));
			box(leg, "hip", -1.6f, -0.5f, -1.6f, 3.2f, 1.5f, 3.2f, ORANGE);
			box(leg, "lower_leg", -0.9f, 3.8f, -1.0f, 1.8f, 3.2f, 2.0f, BONE);
			box(leg, "knee", -1.35f, 3.4f, -1.4f, 2.7f, 1.5f, 2.8f, ORANGE);
			box(root, "pelvic_member_shaft_" + side,
					side * 1.45f - 0.6f, 18.5f, 29.4f, 1.2f, 2.8f, 1.2f, BONE);
			box(root, "pelvic_member_tip_" + side,
					side * 1.45f - 0.8f, 21.2f, 29.2f, 1.6f, 1.7f, 1.6f, MAGENTA);
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z,
			float width, float height, float depth, int textureY) {
		parent.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(0, textureY).addBox(x, y, z, width, height, depth),
				PartPose.ZERO);
	}

	private static void rodXY(PartDefinition parent, String name,
			float x1, float y1, float z, float x2, float y2, float thickness, int textureY) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = Mth.sqrt(dx * dx + dy * dy);
		float rotation = (float) Math.atan2(-dx, dy);
		parent.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(0, textureY)
						.addBox(-thickness / 2, 0, -thickness / 2, thickness, length, thickness),
				PartPose.offsetAndRotation(x1, y1, z, 0, 0, rotation));
	}
}
