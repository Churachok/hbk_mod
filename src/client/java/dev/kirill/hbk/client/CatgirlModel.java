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

/** Catgirl rebuilt from the final front/back concept from the Models task. */
public final class CatgirlModel extends HumanoidModel<GiantBossRenderState> {
	private final ModelPart tailBase;
	private final ModelPart tailMiddle;
	private final ModelPart tailCurve;
	private final ModelPart tailTip;

	public CatgirlModel(ModelPart root) {
		super(root);
		this.hat.visible = false;
		this.tailBase = this.body.getChild("tail_base");
		this.tailMiddle = this.tailBase.getChild("middle");
		this.tailCurve = this.tailMiddle.getChild("curve");
		this.tailTip = this.tailCurve.getChild("tip");
	}

	@Override
	public void setupAnim(GiantBossRenderState state) {
		super.setupAnim(state);
		float wave = state.ageInTicks * 0.11f;
		this.tailBase.xRot = -0.38f + Mth.cos(wave * 0.55f) * 0.035f;
		this.tailBase.yRot = Mth.sin(wave) * 0.13f;
		this.tailMiddle.xRot = -0.34f + Mth.cos(wave * 0.7f - 0.6f) * 0.045f;
		this.tailMiddle.yRot = Mth.sin(wave - 0.65f) * 0.18f;
		this.tailCurve.xRot = 0.18f + Mth.cos(wave * 0.85f - 1.1f) * 0.055f;
		this.tailCurve.yRot = Mth.sin(wave - 1.25f) * 0.22f;
		this.tailTip.xRot = 0.62f + Mth.cos(wave - 1.6f) * 0.065f;
		this.tailTip.yRot = Mth.sin(wave - 1.8f) * 0.25f;
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = PlayerModel.createMesh(CubeDeformation.NONE, true);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 16).addBox(-3.5f, 0.0f, -2.0f, 7.0f, 12.0f, 4.0f),
				PartPose.ZERO);
		PartDefinition rightArm = root.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f),
				PartPose.offset(-4.5f, 2.0f, 0.0f));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(32, 48).addBox(0.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f),
				PartPose.offset(4.5f, 2.0f, 0.0f));
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 16).addBox(-1.75f, 0.0f, -2.0f, 3.5f, 12.0f, 4.0f),
				PartPose.offset(-1.75f, 12.0f, 0.0f));
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(16, 48).addBox(-1.75f, 0.0f, -2.0f, 3.5f, 12.0f, 4.0f),
				PartPose.offset(1.75f, 12.0f, 0.0f));

		addEars(head);
		addBobHair(head);
		addFace(head);
		addUniform(body, rightArm, leftArm);
		addSkirt(body);
		addBackpack(body);
		addLegwear(rightLeg, "right");
		addLegwear(leftLeg, "left");
		addTail(body);

		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void addEars(PartDefinition head) {
		box(head, "left_ear_base", -4.4f, -10.5f, -1.6f, 3.5f, 3.2f, 3.2f, 0, 64);
		box(head, "left_ear_tip", -3.75f, -12.25f, -1.25f, 2.2f, 2.2f, 2.5f, 64, 64);
		box(head, "right_ear_base", 0.9f, -10.5f, -1.6f, 3.5f, 3.2f, 3.2f, 0, 64);
		box(head, "right_ear_tip", 1.55f, -12.25f, -1.25f, 2.2f, 2.2f, 2.5f, 64, 64);
		box(head, "left_inner_ear", -3.65f, -10.55f, -1.95f, 2.0f, 2.2f, 0.45f, 0, 96);
		box(head, "right_inner_ear", 1.65f, -10.55f, -1.95f, 2.0f, 2.2f, 0.45f, 0, 96);
	}

	private static void addBobHair(PartDefinition head) {
		box(head, "hair_cap", -4.6f, -8.8f, -4.6f, 9.2f, 2.0f, 9.2f, 0, 64);
		box(head, "hair_crown", -3.5f, -9.55f, -3.25f, 7.0f, 1.3f, 6.8f, 64, 64);
		box(head, "hair_back", -4.35f, -7.25f, 3.3f, 8.7f, 9.8f, 1.55f, 0, 64);
		box(head, "hair_left", -5.0f, -7.4f, -3.7f, 1.7f, 9.4f, 7.5f, 0, 64);
		box(head, "hair_right", 3.3f, -7.4f, -3.7f, 1.7f, 9.4f, 7.5f, 0, 64);
		box(head, "left_cheek_lock", -4.65f, -3.0f, -4.4f, 1.35f, 5.1f, 1.35f, 64, 64);
		box(head, "right_cheek_lock", 3.3f, -3.0f, -4.4f, 1.35f, 5.1f, 1.35f, 64, 64);
		box(head, "bang_left", -3.7f, -7.9f, -4.9f, 2.25f, 2.6f, 0.85f, 64, 64);
		box(head, "bang_center", -1.55f, -8.25f, -5.0f, 2.5f, 3.0f, 0.9f, 64, 64);
		box(head, "bang_right", 0.85f, -7.85f, -4.9f, 2.8f, 2.55f, 0.85f, 64, 64);
	}

	private static void addFace(PartDefinition head) {
		box(head, "left_eye", -3.15f, -4.95f, -4.65f, 2.25f, 2.7f, 0.28f, 64, 96);
		box(head, "right_eye", 0.9f, -4.95f, -4.65f, 2.25f, 2.7f, 0.28f, 64, 96);
		box(head, "left_lash", -3.4f, -5.2f, -4.82f, 2.8f, 0.55f, 0.25f, 96, 96);
		box(head, "right_lash", 0.65f, -5.2f, -4.82f, 2.8f, 0.55f, 0.25f, 96, 96);
		box(head, "left_pupil", -2.35f, -4.3f, -4.84f, 0.8f, 1.45f, 0.2f, 96, 96);
		box(head, "right_pupil", 1.7f, -4.3f, -4.84f, 0.8f, 1.45f, 0.2f, 96, 96);
		box(head, "mouth", -1.7f, -1.7f, -4.68f, 3.4f, 1.15f, 0.25f, 80, 96);
		box(head, "upper_lip", -1.7f, -1.9f, -4.83f, 3.4f, 0.3f, 0.08f, 80, 96);
		box(head, "fang", -0.75f, -1.68f, -4.94f, 0.5f, 0.6f, 0.02f, 0, 128);
	}

	private static void addUniform(PartDefinition body, PartDefinition rightArm, PartDefinition leftArm) {
		box(body, "blouse", -3.75f, -0.25f, -2.3f, 7.5f, 8.25f, 4.6f, 0, 128);
		box(body, "left_collar", -3.1f, 0.2f, -2.75f, 2.7f, 2.25f, 0.65f, 0, 128);
		box(body, "right_collar", 0.4f, 0.2f, -2.75f, 2.7f, 2.25f, 0.65f, 0, 128);
		box(body, "bow_left", -3.05f, 1.25f, -3.3f, 2.6f, 2.65f, 1.05f, 0, 160);
		box(body, "bow_right", 0.45f, 1.25f, -3.3f, 2.6f, 2.65f, 1.05f, 0, 160);
		box(body, "bow_knot", -0.9f, 1.7f, -3.7f, 1.8f, 1.75f, 1.35f, 32, 160);
		box(body, "bow_left_ribbon", -2.0f, 3.4f, -3.15f, 1.5f, 2.3f, 0.8f, 0, 160);
		box(body, "bow_right_ribbon", 0.5f, 3.4f, -3.15f, 1.5f, 2.3f, 0.8f, 0, 160);
		box(rightArm, "right_sleeve", -3.15f, -0.4f, -2.25f, 3.3f, 4.9f, 4.5f, 0, 128);
		box(leftArm, "left_sleeve", -0.15f, -0.4f, -2.25f, 3.3f, 4.9f, 4.5f, 0, 128);
		box(rightArm, "right_sleeve_cuff", -3.05f, 3.85f, -2.12f, 3.1f, 0.9f, 4.24f, 0, 128);
		box(leftArm, "left_sleeve_cuff", -0.05f, 3.85f, -2.12f, 3.1f, 0.9f, 4.24f, 0, 128);
	}

	private static void addSkirt(PartDefinition body) {
		box(body, "waistband", -3.95f, 7.3f, -2.55f, 7.9f, 1.25f, 5.1f, 64, 160);
		posedBox(body, "skirt_back", -4.25f, 0.0f, 1.85f, 8.5f, 5.8f, 1.1f,
				64, 160, 0.0f, 8.15f, 0.0f, 0.08f, 0.0f, 0.0f);
		posedBox(body, "skirt_left", -4.3f, 0.0f, -2.35f, 1.1f, 5.8f, 4.7f,
				64, 160, 0.0f, 8.15f, 0.0f, 0.0f, 0.0f, 0.08f);
		posedBox(body, "skirt_right", 3.2f, 0.0f, -2.35f, 1.1f, 5.8f, 4.7f,
				64, 160, 0.0f, 8.15f, 0.0f, 0.0f, 0.0f, -0.08f);
		for (int i = 0; i < 5; i++) {
			float x = -3.9f + i * 1.56f;
			float depth = i % 2 == 0 ? 1.15f : 0.85f;
			posedBox(body, "front_pleat_" + i, x, 0.0f, -2.95f, 1.56f, 5.8f, depth,
					64, 160, 0.0f, 8.15f, 0.0f, -0.08f, 0.0f, 0.0f);
		}
	}

	private static void addBackpack(PartDefinition body) {
		box(body, "backpack", -3.7f, 0.9f, 2.0f, 7.4f, 9.55f, 3.7f, 0, 192);
		box(body, "backpack_flap", -3.3f, 1.25f, 5.3f, 6.6f, 3.0f, 0.65f, 64, 192);
		box(body, "backpack_top_band", -3.75f, 3.65f, 5.35f, 7.5f, 1.15f, 0.6f, 0, 128);
		box(body, "backpack_bottom_band", -3.75f, 7.55f, 5.35f, 7.5f, 1.15f, 0.6f, 0, 128);
		box(body, "left_strap", -3.3f, 0.4f, -2.6f, 0.95f, 7.1f, 0.55f, 0, 192);
		box(body, "right_strap", 2.35f, 0.4f, -2.6f, 0.95f, 7.1f, 0.55f, 0, 192);
		box(body, "paw_pad", -0.75f, 5.45f, 5.82f, 1.5f, 1.2f, 0.3f, 0, 128);
		box(body, "paw_toe_left", -1.25f, 4.85f, 5.83f, 0.75f, 0.75f, 0.28f, 0, 128);
		box(body, "paw_toe_middle", -0.35f, 4.55f, 5.83f, 0.7f, 0.75f, 0.28f, 0, 128);
		box(body, "paw_toe_right", 0.5f, 4.85f, 5.83f, 0.75f, 0.75f, 0.28f, 0, 128);
	}

	private static void addLegwear(PartDefinition leg, String side) {
		box(leg, side + "_sock", -1.9f, 4.0f, -2.08f, 3.8f, 6.35f, 4.16f, 0, 224);
		box(leg, side + "_shoe", -2.05f, 9.3f, -2.7f, 4.1f, 2.9f, 5.1f, 64, 224);
		box(leg, side + "_sole", -2.1f, 11.45f, -2.85f, 4.2f, 0.8f, 5.35f, 0, 224);
	}

	private static void addTail(PartDefinition body) {
		PartDefinition base = body.addOrReplaceChild("tail_base",
				CubeListBuilder.create().texOffs(0, 64).addBox(-1.35f, -1.35f, 0.0f, 2.7f, 2.7f, 6.5f),
				PartPose.offsetAndRotation(0.0f, 8.7f, 2.0f, -0.38f, 0.0f, 0.0f));
		PartDefinition middle = base.addOrReplaceChild("middle",
				CubeListBuilder.create().texOffs(32, 64).addBox(-1.45f, -1.45f, 0.0f, 2.9f, 2.9f, 6.3f),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.8f, -0.34f, 0.0f, 0.0f));
		PartDefinition curve = middle.addOrReplaceChild("curve",
				CubeListBuilder.create().texOffs(64, 64).addBox(-1.55f, -1.55f, 0.0f, 3.1f, 3.1f, 5.8f),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.55f, 0.18f, 0.0f, 0.0f));
		curve.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(88, 64).addBox(-1.75f, -1.75f, 0.0f, 3.5f, 3.5f, 4.8f),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.0f, 0.62f, 0.0f, 0.0f));
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z,
			float width, float height, float depth, int textureX, int textureY) {
		parent.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(textureX, textureY).addBox(x, y, z, width, height, depth),
				PartPose.ZERO);
	}

	private static void posedBox(PartDefinition parent, String name, float x, float y, float z,
			float width, float height, float depth, int textureX, int textureY,
			float offsetX, float offsetY, float offsetZ, float xRot, float yRot, float zRot) {
		parent.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(textureX, textureY).addBox(x, y, z, width, height, depth),
				PartPose.offsetAndRotation(offsetX, offsetY, offsetZ, xRot, yRot, zRot));
	}
}
