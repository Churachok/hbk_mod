package dev.kirill.hbk.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** The rounded black silhouette of the Unknown. */
public final class KirillSecondModel extends HumanoidModel<GiantBossRenderState> {
	private static final int FINGER_COUNT = 5;
	private final ModelPart[] rightFingers = new ModelPart[FINGER_COUNT];
	private final ModelPart[] leftFingers = new ModelPart[FINGER_COUNT];
	private final ModelPart rightForearm;

	public KirillSecondModel(ModelPart root) {
		super(root);
		this.hat.visible = false;
		this.rightForearm = this.rightArm.getChild("forearm");
		collectFingers(this.rightArm, this.rightFingers);
		collectFingers(this.leftArm, this.leftFingers);
	}

	private static void collectFingers(ModelPart arm, ModelPart[] fingers) {
		ModelPart hand = arm.getChild("forearm").getChild("hand");
		for (int i = 0; i < fingers.length; i++) {
			fingers[i] = hand.getChild("finger_" + i);
		}
	}

	@Override
	public void setupAnim(GiantBossRenderState state) {
		super.setupAnim(state);
		float wave = state.ageInTicks * 0.075f;
		for (int i = 0; i < FINGER_COUNT; i++) {
			float phase = i * 0.82f;
			this.rightFingers[i].xRot = 0.10f + Mth.sin(wave + phase) * 0.075f;
			this.leftFingers[i].xRot = 0.10f + Mth.sin(wave + phase + 1.7f) * 0.075f;
			this.rightFingers[i].zRot = Mth.sin(wave * 0.7f + phase) * 0.025f;
			this.leftFingers[i].zRot = Mth.sin(wave * 0.7f + phase + 1.7f) * 0.025f;
		}

		float grab = state.unknownGrabProgress;
		if (grab > 0.0f) {
			this.rightArm.xRot = Mth.lerp(grab, this.rightArm.xRot, -1.43f);
			this.rightArm.yRot = Mth.lerp(grab, this.rightArm.yRot, -0.18f);
			this.rightArm.zRot = Mth.lerp(grab, this.rightArm.zRot, 0.08f);
			this.rightForearm.xRot = Mth.lerp(grab, this.rightForearm.xRot, -0.48f);
			for (ModelPart finger : this.rightFingers) {
				finger.xRot = Mth.lerp(grab, finger.xRot, 1.05f);
			}
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
				black().addBox(-5.0f, -10.0f, -4.25f, 10.0f, 9.5f, 8.5f),
				PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		addRoundedHeadAndFace(head);

		PartDefinition body = root.addOrReplaceChild("body",
				black().addBox(-4.8f, 0.0f, -3.0f, 9.6f, 12.5f, 6.0f)
						.addBox(-5.5f, 3.0f, -3.5f, 11.0f, 10.0f, 7.0f),
				PartPose.ZERO);
		box(body, "left_shoulder", -6.0f, 0.5f, -2.75f, 2.0f, 5.0f, 5.5f, 0, 0);
		box(body, "right_shoulder", 4.0f, 0.5f, -2.75f, 2.0f, 5.0f, 5.5f, 0, 0);

		PartDefinition rightArm = addArm(root, "right_arm", -6.0f, 2.0f, false);
		PartDefinition leftArm = addArm(root, "left_arm", 6.0f, 2.0f, true);
		addHand(rightArm, false);
		addHand(leftArm, true);

		addLeg(root, "right_leg", -2.6f);
		addLeg(root, "left_leg", 2.6f);

		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void addRoundedHeadAndFace(PartDefinition head) {
		box(head, "crown", -4.0f, -11.25f, -3.6f, 8.0f, 1.5f, 7.2f, 0, 0);
		box(head, "left_temple", -6.0f, -8.5f, -3.6f, 1.2f, 6.5f, 7.2f, 0, 0);
		box(head, "right_temple", 4.8f, -8.5f, -3.6f, 1.2f, 6.5f, 7.2f, 0, 0);
		box(head, "lower_head", -4.1f, -1.25f, -3.5f, 8.2f, 1.4f, 7.0f, 0, 0);

		box(head, "left_lens", -4.4f, -7.0f, -4.65f, 3.9f, 2.5f, 0.55f, 0, 64);
		box(head, "right_lens", 0.5f, -7.0f, -4.65f, 3.9f, 2.5f, 0.55f, 0, 64);
		box(head, "glasses_bridge", -0.7f, -6.25f, -4.72f, 1.4f, 0.45f, 0.55f, 96, 64);
		box(head, "mouth_patch", -1.8f, -3.35f, -4.72f, 3.6f, 1.8f, 0.5f, 32, 64);
		box(head, "mouth_line", -0.9f, -2.45f, -5.0f, 1.8f, 0.35f, 0.18f, 64, 64);
	}

	private static PartDefinition addArm(PartDefinition root, String name, float x, float y, boolean mirrored) {
		CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 0);
		if (mirrored) {
			cubes = cubes.mirror();
		}
		PartDefinition arm = root.addOrReplaceChild(name,
				cubes.addBox(-1.8f, -2.0f, -2.0f, 3.6f, 8.0f, 4.0f),
				PartPose.offset(x, y, 0.0f));
		arm.addOrReplaceChild("forearm",
				black().addBox(-1.7f, 0.0f, -1.9f, 3.4f, 8.0f, 3.8f),
				PartPose.offsetAndRotation(0.0f, 5.4f, 0.0f, -0.06f, 0.0f, mirrored ? -0.10f : 0.10f));
		return arm;
	}

	private static void addHand(PartDefinition arm, boolean mirrored) {
		PartDefinition forearm = arm.getChild("forearm");
		PartDefinition hand = forearm.addOrReplaceChild("hand",
				black().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 3.0f, 4.0f),
				PartPose.offset(0.0f, 7.2f, 0.0f));
		float[] lengths = {3.6f, 4.25f, 4.7f, 4.35f, 3.7f};
		for (int i = 0; i < FINGER_COUNT; i++) {
			float x = -2.2f + i * 1.1f;
			hand.addOrReplaceChild("finger_" + i,
					black().addBox(-0.475f, 0.0f, -0.625f, 0.95f, lengths[i], 1.25f),
					PartPose.offsetAndRotation(x, 2.45f, -0.35f, 0.10f, 0.0f,
							mirrored ? -0.025f * (i - 2) : 0.025f * (i - 2)));
		}
	}

	private static void addLeg(PartDefinition root, String name, float x) {
		root.addOrReplaceChild(name,
				black().addBox(-2.25f, 0.0f, -2.5f, 4.5f, 13.0f, 5.0f)
						.addBox(-2.5f, 10.5f, -3.3f, 5.0f, 3.2f, 6.5f),
				PartPose.offset(x, 12.0f, 0.0f));
	}

	private static CubeListBuilder black() {
		return CubeListBuilder.create().texOffs(0, 0);
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z,
			float width, float height, float depth, int textureX, int textureY) {
		parent.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(textureX, textureY)
						.addBox(x, y, z, width, height, depth),
				PartPose.ZERO);
	}
}
