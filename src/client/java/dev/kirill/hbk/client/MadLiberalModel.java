package dev.kirill.hbk.client;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class MadLiberalModel extends GiantBossModel {
	private final ModelPart helmet;
	private final ModelPart chestPlate;
	private final ModelPart rightShoulder;
	private final ModelPart leftShoulder;
	private final ModelPart rightBracer;
	private final ModelPart leftBracer;
	private final ModelPart rightGreave;
	private final ModelPart leftGreave;

	public MadLiberalModel(ModelPart root) {
		super(root, true);
		this.helmet = this.head.getChild("armor_helmet");
		this.chestPlate = this.body.getChild("armor_chest");
		this.rightShoulder = this.rightArm.getChild("armor_right_shoulder");
		this.leftShoulder = this.leftArm.getChild("armor_left_shoulder");
		this.rightBracer = this.rightArm.getChild("armor_right_bracer");
		this.leftBracer = this.leftArm.getChild("armor_left_bracer");
		this.rightGreave = this.rightLeg.getChild("armor_right_greave");
		this.leftGreave = this.leftLeg.getChild("armor_left_greave");
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		mesh.getRoot().getChild("head").addOrReplaceChild("armor_helmet",
				CubeListBuilder.create().texOffs(0, 32)
						.addBox(-4.5f, -8.5f, -4.5f, 9.0f, 4.0f, 9.0f, new CubeDeformation(0.1f)),
				PartPose.ZERO);
		mesh.getRoot().getChild("body").addOrReplaceChild("armor_chest",
				CubeListBuilder.create().texOffs(16, 32)
						.addBox(-4.5f, -0.5f, -2.5f, 9.0f, 7.0f, 5.0f, new CubeDeformation(0.12f)),
				PartPose.ZERO);
		addArmArmor(mesh.getRoot().getChild("right_arm"), true);
		addArmArmor(mesh.getRoot().getChild("left_arm"), false);
		addGreave(mesh.getRoot().getChild("right_leg"), true);
		addGreave(mesh.getRoot().getChild("left_leg"), false);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void addArmArmor(net.minecraft.client.model.geom.builders.PartDefinition arm, boolean right) {
		String side = right ? "right" : "left";
		arm.addOrReplaceChild("armor_" + side + "_shoulder",
				CubeListBuilder.create().texOffs(40, 32)
						.addBox(right ? -3.8f : -1.2f, -2.8f, -2.8f, 5.0f, 5.0f, 5.6f,
								new CubeDeformation(0.08f)), PartPose.ZERO);
		arm.addOrReplaceChild("armor_" + side + "_bracer",
				CubeListBuilder.create().texOffs(40, 44)
						.addBox(-2.4f, 5.0f, -2.4f, 4.8f, 6.0f, 4.8f,
								new CubeDeformation(0.06f)), PartPose.ZERO);
	}

	private static void addGreave(net.minecraft.client.model.geom.builders.PartDefinition leg, boolean right) {
		leg.addOrReplaceChild("armor_" + (right ? "right" : "left") + "_greave",
				CubeListBuilder.create().texOffs(0, 45)
						.addBox(-2.35f, 5.0f, -2.35f, 4.7f, 7.0f, 4.7f,
								new CubeDeformation(0.05f)), PartPose.ZERO);
	}

	@Override
	public void setupAnim(GiantBossRenderState state) {
		super.setupAnim(state);
		int stage = state.armorCrackStage;
		this.rightBracer.visible = stage < 1;
		this.leftGreave.visible = stage < 1;
		this.leftBracer.visible = stage < 2;
		this.rightShoulder.visible = stage < 2;
		this.leftShoulder.visible = stage < 3;
		this.rightGreave.visible = stage < 3;
		this.helmet.visible = stage < 4;
		this.chestPlate.visible = stage < 4;
	}
}
