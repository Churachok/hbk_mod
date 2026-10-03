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

/** Every colour has a dedicated 64-pixel atlas tile; geometry supplies the details. */
public final class TestKonataModel extends HumanoidModel<GiantBossRenderState> {
	private final ModelPart longHair;

	public TestKonataModel(ModelPart root) {
		super(root);
		hat.visible = false;
		longHair = body.getChild("long_hair");
	}

	@Override
	public void setupAnim(GiantBossRenderState state) {
		super.setupAnim(state);
		longHair.xRot = 0.04f + Mth.sin(state.ageInTicks * 0.07f) * 0.025f;
		longHair.zRot = Mth.sin(state.ageInTicks * 0.045f) * 0.025f;
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = PlayerModel.createMesh(CubeDeformation.NONE, true);
		var root = mesh.getRoot();
		for (String name : new String[]{"hat", "jacket", "left_sleeve", "right_sleeve", "left_pants", "right_pants"}) {
			root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
		}
		var head = part(root, "head", -4, -8, -4, 8, 8, 8, 0, PartPose.ZERO);
		var body = part(root, "body", -3.5f, 0, -2, 7, 10, 4, 3, PartPose.ZERO);
		var rightArm = part(root, "right_arm", -3, -2, -2, 3, 12, 4, 0, PartPose.offset(-4, 2, 0));
		var leftArm = part(root, "left_arm", 0, -2, -2, 3, 12, 4, 0, PartPose.offset(4, 2, 0));
		var rightLeg = part(root, "right_leg", -1.5f, 0, -1.5f, 3, 12, 3, 0, PartPose.offset(-1.65f, 12, 0));
		var leftLeg = part(root, "left_leg", -1.5f, 0, -1.5f, 3, 12, 3, 0, PartPose.offset(1.65f, 12, 0));

		box(head, "cap", -4.4f, -8.7f, -4.4f, 8.8f, 2, 8.8f, 1);
		box(head, "back", -4.3f, -7, 3, 8.6f, 9, 1.7f, 1);
		for (int side : new int[]{-1, 1}) {
			box(head, "temple_" + side, side < 0 ? -4.6f : 3.4f, -7, -3.7f, 1.2f, 9, 7, 1);
			part(head, "side_lock_" + side, -0.7f, 0, -0.6f, 1.4f, 13, 1.3f, 1,
					PartPose.offsetAndRotation(side * 3.7f, -4, -4, 0, 0, -side * 0.09f));
			float x = side < 0 ? -3.2f : 1.0f;
			box(head, "eye_white_" + side, x, -4.8f, -4.08f, 2.2f, 2.6f, 0.12f, 3);
			box(head, "iris_" + side, x + 0.3f, -4.65f, -4.22f, 1.6f, 2.35f, 0.12f, 6);
			box(head, "pupil_" + side, x + 0.75f, -4.6f, -4.35f, 0.65f, 1.6f, 0.1f, 7);
			box(head, "glint_" + side, x + 0.35f, -4.5f, -4.48f, 0.55f, 0.65f, 0.1f, 3);
			box(head, "lash_" + side, x - 0.15f, -5, -4.48f, 2.5f, 0.4f, 0.12f, 7);
		}
		box(head, "beauty_mark", 3.0f, -1.9f, -4.1f, 0.23f, 0.25f, 0.12f, 7);
		box(head, "smile", -0.7f, -1.15f, -4.1f, 1.4f, 0.18f, 0.1f, 7);
		for (int i = 0; i < 4; i++) {
			part(head, "bang_" + i, -0.8f, 0, -0.4f, 1.6f, i == 2 ? 5.8f : 3.3f, 0.9f, i % 2 == 0 ? 1 : 2,
					PartPose.offsetAndRotation(-3 + i * 1.7f, -7.9f, -4.6f, 0, 0, -0.25f));
		}
		part(head, "ahoge_stem", -0.25f, -3.5f, -0.3f, 0.5f, 3.5f, 0.6f, 1,
				PartPose.offsetAndRotation(-1, -8.5f, 0, 0, 0, -0.25f));
		part(head, "ahoge_arch", 0, -0.3f, -0.3f, 3.2f, 0.6f, 0.6f, 2,
				PartPose.offsetAndRotation(-1.8f, -11.8f, 0, 0, 0, 0.12f));
		part(head, "ahoge_tip", 0, 0, -0.25f, 0.5f, 1.9f, 0.5f, 1,
				PartPose.offsetAndRotation(1.3f, -11.4f, 0, 0, 0, -0.5f));

		var hair = body.addOrReplaceChild("long_hair", CubeListBuilder.create(), PartPose.offset(0, -1, 3));
		for (int i = 0; i < 5; i++) {
			float length = 20 + (i % 3) * 1.3f;
			var lock = part(hair, "lock_" + i, -0.9f, 0, 0, 1.8f, length, 1.4f, i % 2 == 0 ? 1 : 2,
					PartPose.offsetAndRotation(-3.6f + i * 1.8f, 0, 0, 0.06f, 0, (2 - i) * 0.025f));
			box(lock, "tip", -0.55f, length, 0.1f, 1.1f, 1.4f, 1.1f, 1);
		}
		// Loose blouse, layered hem and broad sailor collar from the reference.
		box(body, "blouse", -3.75f, 0.1f, -2.3f, 7.5f, 8.6f, 4.6f, 3);
		box(body, "blouse_hem", -3.9f, 8.3f, -2.4f, 7.8f, 0.8f, 4.8f, 3);
		box(body, "sailor_back", -3.8f, -0.05f, 2.32f, 7.6f, 3.1f, 0.25f, 4);
		for (int side : new int[]{-1, 1}) {
			box(body, "shoulder_collar_" + side, side < 0 ? -3.8f : 1.55f,
					-0.15f, -2.4f, 2.25f, 0.3f, 4.9f, 4);
			var collar = part(body, "collar_" + side, -0.85f, 0, 0, 1.7f, 4.8f, 0.3f, 4,
					PartPose.offsetAndRotation(side * 2.3f, 0.05f, -2.65f, 0, 0, side * 0.48f));
			box(collar, "outer_stripe", -0.57f, 0.15f, -0.06f, 0.16f, 4.5f, 0.08f, 3);
			box(collar, "inner_stripe", -0.18f, 0.15f, -0.06f, 0.16f, 4.5f, 0.08f, 3);
			part(body, "tie_" + side, -0.7f, 0, -0.2f, 1.4f, 3.6f, 0.4f, 5,
					PartPose.offsetAndRotation(side * 0.35f, 4.55f, -2.85f, 0, 0, -side * 0.2f));
		}
		box(body, "knot", -0.65f, 3.9f, -3.05f, 1.3f, 1.0f, 0.65f, 5);
		for (int i = 0; i < 8; i++) {
			float angle = i * Mth.PI / 4;
			var pleat = part(body, "pleat_" + i, -1.7f, 0, -0.4f, 3.4f, 5, 0.8f, i % 2 == 0 ? 4 : 10,
					PartPose.offsetAndRotation(Mth.sin(angle) * 3.2f, 9, -Mth.cos(angle) * 2.4f, -0.16f, -angle, 0));
			box(pleat, "stripe_upper", -1.7f, 3.8f, -0.48f, 3.4f, 0.22f, 0.1f, 3);
			box(pleat, "stripe_lower", -1.7f, 4.4f, -0.48f, 3.4f, 0.22f, 0.1f, 3);
		}
		sleeve(rightArm, -3.2f);
		sleeve(leftArm, -0.2f);
		for (var leg : new PartDefinition[]{rightLeg, leftLeg}) {
			box(leg, "sock", -1.55f, 5, -1.55f, 3.1f, 6.8f, 3.1f, 8);
			box(leg, "loafer", -1.7f, 10.3f, -2.15f, 3.4f, 1.7f, 4, 9);
			box(leg, "sole", -1.75f, 11.65f, -2.2f, 3.5f, 0.35f, 4.1f, 7);
		}
		return LayerDefinition.create(mesh, 512, 128);
	}

	private static void sleeve(PartDefinition arm, float x) {
		box(arm, "upper_sleeve", x, -2, -2.15f, 3.4f, 5.2f, 4.3f, 3);
		box(arm, "loose_lower_sleeve", x - 0.2f, 3, -2.3f, 3.8f, 3.8f, 4.6f, 3);
		box(arm, "cuff", x - 0.22f, 6.6f, -2.34f, 3.84f, 1.65f, 4.68f, 4);
		box(arm, "cuff_stripe", x - 0.25f, 6.9f, -2.37f, 3.9f, 0.22f, 4.74f, 3);
		box(arm, "cuff_stripe_lower", x - 0.25f, 7.55f, -2.37f, 3.9f, 0.22f, 4.74f, 3);
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z,
			float w, float h, float d, int colour) {
		part(parent, name, x, y, z, w, h, d, colour, PartPose.ZERO);
	}

	private static PartDefinition part(PartDefinition parent, String name, float x, float y, float z,
			float w, float h, float d, int colour, PartPose pose) {
		return parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(colour % 8 * 64, colour / 8 * 64)
				.addBox(x, y, z, w, h, d), pose);
	}
}
