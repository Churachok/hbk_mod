package dev.kirill.hbk.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Block-built wolf anatomy and clothing layered over the normal humanoid walk cycle. */
public final class PinkFurryWolfModel extends HumanoidModel<PinkFurryWolfRenderState> {
	private static final int PINK_X = 0;
	private static final int LIGHT_PINK_X = 32;
	private static final int WHITE_X = 64;
	private static final int DARK_X = 96;
	private static final int PALETTE_Y = 64;
	private static final int RAINBOW_Y = 96;
	private static final int[] RAINBOW_X = {0, 21, 42, 63, 84, 105};

	private final ModelPart tail;

	public PinkFurryWolfModel(ModelPart root) {
		super(root);
		this.hat.visible = false;
		this.tail = this.body.getChild("tail");
	}

	@Override
	public void setupAnim(PinkFurryWolfRenderState state) {
		super.setupAnim(state);
		// A small idle wag is always present, even while the wolf is standing still.
		this.tail.yRot += Mth.sin(state.ageInTicks * 0.12f) * 0.12f;
		this.tail.xRot += Mth.cos(state.ageInTicks * 0.08f) * 0.03f;
		if (state.fleeing) {
			this.applyFourLeggedFleePose(state);
		}
	}

	private void applyFourLeggedFleePose(PinkFurryWolfRenderState state) {
		float stride = Mth.cos(state.walkAnimationPos * 0.9f)
				* Math.min(0.9f, state.walkAnimationSpeed * 0.9f);

		this.body.xRot = 1.35f;
		this.body.y = 6.0f;
		this.head.y = 6.0f;
		this.head.z = -5.0f;
		this.head.xRot = 0.2f;

		this.rightArm.y = 8.0f;
		this.leftArm.y = 8.0f;
		this.rightArm.z = -4.0f;
		this.leftArm.z = -4.0f;
		this.rightArm.xRot = -0.8f + stride;
		this.leftArm.xRot = -0.8f - stride;

		this.rightLeg.z = 4.0f;
		this.leftLeg.z = 4.0f;
		this.rightLeg.xRot = 0.55f - stride;
		this.leftLeg.xRot = 0.55f + stride;
		this.tail.xRot = -0.2f;
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		PartDefinition body = root.getChild("body");
		PartDefinition rightArm = root.getChild("right_arm");
		PartDefinition leftArm = root.getChild("left_arm");
		PartDefinition rightLeg = root.getChild("right_leg");
		PartDefinition leftLeg = root.getChild("left_leg");

		box(head, "left_ear", -4.5f, -11.0f, -2.0f, 3.0f, 4.0f, 3.0f, PINK_X, PALETTE_Y);
		box(head, "right_ear", 1.5f, -11.0f, -2.0f, 3.0f, 4.0f, 3.0f, PINK_X, PALETTE_Y);
		box(head, "left_inner_ear", -3.75f, -10.1f, -2.35f, 1.5f, 2.5f, 0.5f, LIGHT_PINK_X, PALETTE_Y);
		box(head, "right_inner_ear", 2.25f, -10.1f, -2.35f, 1.5f, 2.5f, 0.5f, LIGHT_PINK_X, PALETTE_Y);
		box(head, "left_cheek", -5.0f, -4.0f, -2.5f, 2.0f, 3.5f, 3.5f, LIGHT_PINK_X, PALETTE_Y);
		box(head, "right_cheek", 3.0f, -4.0f, -2.5f, 2.0f, 3.5f, 3.5f, LIGHT_PINK_X, PALETTE_Y);
		box(head, "snout", -3.0f, -3.6f, -6.6f, 6.0f, 4.0f, 3.0f, WHITE_X, PALETTE_Y);
		box(head, "nose", -1.5f, -3.2f, -7.3f, 3.0f, 1.8f, 1.0f, DARK_X, PALETTE_Y);
		box(head, "left_eye", -3.4f, -6.5f, -4.35f, 2.5f, 3.0f, 0.5f, WHITE_X, PALETTE_Y);
		box(head, "right_eye", 0.9f, -6.5f, -4.35f, 2.5f, 3.0f, 0.5f, WHITE_X, PALETTE_Y);
		box(head, "left_pupil", -2.4f, -5.6f, -4.75f, 1.0f, 2.0f, 0.5f, RAINBOW_X[5], RAINBOW_Y);
		box(head, "right_pupil", 1.4f, -5.6f, -4.75f, 1.0f, 2.0f, 0.5f, RAINBOW_X[5], RAINBOW_Y);
		box(head, "left_brow", -3.5f, -7.25f, -4.55f, 2.7f, 0.6f, 0.6f, DARK_X, PALETTE_Y);
		box(head, "right_brow", 0.8f, -7.25f, -4.55f, 2.7f, 0.6f, 0.6f, DARK_X, PALETTE_Y);
		box(head, "smile", -1.7f, -1.2f, -6.75f, 3.4f, 0.8f, 0.6f, DARK_X, PALETTE_Y);
		box(head, "tongue", -0.8f, -0.55f, -6.9f, 1.6f, 0.8f, 0.6f, LIGHT_PINK_X, PALETTE_Y);

		box(body, "left_jacket_panel", -4.7f, -0.3f, -2.5f, 2.6f, 12.6f, 5.0f, PINK_X, PALETTE_Y);
		box(body, "right_jacket_panel", 2.1f, -0.3f, -2.5f, 2.6f, 12.6f, 5.0f, PINK_X, PALETTE_Y);
		box(body, "hood", -4.0f, -1.6f, 1.7f, 8.0f, 4.0f, 2.5f, LIGHT_PINK_X, PALETTE_Y);
		box(body, "left_collar", -3.7f, -0.7f, -2.9f, 1.5f, 3.0f, 1.0f, LIGHT_PINK_X, PALETTE_Y);
		box(body, "right_collar", 2.2f, -0.7f, -2.9f, 1.5f, 3.0f, 1.0f, LIGHT_PINK_X, PALETTE_Y);
		box(body, "left_drawstring", -2.6f, 1.1f, -2.85f, 0.6f, 4.0f, 0.6f, WHITE_X, PALETTE_Y);
		box(body, "right_drawstring", 2.0f, 1.1f, -2.85f, 0.6f, 4.0f, 0.6f, WHITE_X, PALETTE_Y);

		for (int color = 0; color < RAINBOW_X.length; color++) {
			box(rightArm, "armband_" + color, -3.25f, -0.5f + color, -2.25f,
					4.5f, 1.0f, 4.5f, RAINBOW_X[color], RAINBOW_Y);
		}
		addPawHand(rightArm, "right");
		addPawHand(leftArm, "left");

		addCargoPocket(rightLeg, "right", -2.45f);
		addCargoPocket(leftLeg, "left", 1.25f);
		addFootPaw(rightLeg, "right");
		addFootPaw(leftLeg, "left");

		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(PINK_X, PALETTE_Y)
						.addBox(-2.5f, -2.0f, 0.0f, 5.0f, 6.0f, 8.0f),
				PartPose.offsetAndRotation(0.0f, 8.5f, 2.0f, 0.42f, 0.0f, 0.0f));
		PartDefinition tailMiddle = tail.addOrReplaceChild("middle",
				CubeListBuilder.create().texOffs(LIGHT_PINK_X, PALETTE_Y)
						.addBox(-3.0f, -3.0f, 0.0f, 6.0f, 7.0f, 9.0f),
				PartPose.offsetAndRotation(0.0f, 2.5f, 7.0f, -0.18f, 0.0f, 0.0f));
		tailMiddle.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(WHITE_X, PALETTE_Y)
						.addBox(-3.5f, -3.5f, 0.0f, 7.0f, 7.0f, 8.0f),
				PartPose.offsetAndRotation(0.0f, 0.4f, 8.0f, -0.28f, 0.0f, 0.0f));

		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void addPawHand(PartDefinition arm, String side) {
		box(arm, side + "_paw", -3.15f, 7.8f, -2.15f, 4.3f, 4.3f, 4.3f, WHITE_X, PALETTE_Y);
		for (int claw = 0; claw < 3; claw++) {
			box(arm, side + "_claw_" + claw, -2.8f + claw * 1.25f, 10.7f, -2.65f,
					0.7f, 1.0f, 0.8f, DARK_X, PALETTE_Y);
		}
	}

	private static void addCargoPocket(PartDefinition leg, String side, float x) {
		box(leg, side + "_cargo_pocket", x, 3.5f, -2.3f, 1.2f, 4.0f, 4.6f, WHITE_X, PALETTE_Y);
	}

	private static void addFootPaw(PartDefinition leg, String side) {
		box(leg, side + "_foot_paw", -2.5f, 9.0f, -4.5f, 5.0f, 3.0f, 6.5f, PINK_X, PALETTE_Y);
		box(leg, side + "_toe_cap", -2.5f, 10.0f, -5.0f, 5.0f, 2.0f, 2.0f, WHITE_X, PALETTE_Y);
		for (int claw = 0; claw < 3; claw++) {
			box(leg, side + "_toe_claw_" + claw, -2.1f + claw * 1.6f, 10.8f, -5.45f,
					0.8f, 1.0f, 0.8f, DARK_X, PALETTE_Y);
		}
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z,
			float width, float height, float depth, int textureX, int textureY) {
		parent.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(textureX, textureY).addBox(x, y, z, width, height, depth),
				PartPose.ZERO);
	}
}
