package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Simple cuboids matching the final squirrel concept in the Models chat. */
public final class SquirrelModel extends EntityModel<SquirrelRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart tailMiddle;
	private final ModelPart tailTip;
	private final ModelPart frontLeft;
	private final ModelPart frontRight;
	private final ModelPart hindLeft;
	private final ModelPart hindRight;

	public SquirrelModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.tail = root.getChild("tail");
		this.tailMiddle = this.tail.getChild("middle");
		this.tailTip = this.tailMiddle.getChild("tip");
		this.frontLeft = root.getChild("front_left");
		this.frontRight = root.getChild("front_right");
		this.hindLeft = root.getChild("hind_left");
		this.hindRight = root.getChild("hind_right");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-3, -2.5f, -4.5f, 6, 5, 9), PartPose.offset(0, 18.5f, 1));
		var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0)
				.addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.offset(0, 16.5f, -5));
		head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(24, 24)
				.addBox(-2, 0, -4, 4, 2, 2), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(16, 24)
				.addBox(-1, -3, -0.5f, 2, 3, 1), PartPose.offset(1.5f, -2.5f, 0));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(16, 24)
				.addBox(-1, -3, -0.5f, 2, 3, 1), PartPose.offset(-1.5f, -2.5f, 0));
		for (var name : new String[]{"front_left", "front_right", "hind_left", "hind_right"}) {
			root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 24)
					.addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(
						name.endsWith("left") ? 2 : -2, 20, name.startsWith("front") ? -2 : 4));
		}
		var tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40)
				.addBox(-3, -5, 0, 6, 5, 4), PartPose.offsetAndRotation(0, 20, 5, -0.35f, 0, 0));
		var middle = tail.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 40)
				.addBox(-3, -5, 0, 6, 5, 4), PartPose.offsetAndRotation(0, -5, 0, 0.25f, 0, 0));
		middle.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 40)
				.addBox(-3, -5, 0, 6, 5, 4), PartPose.offsetAndRotation(0, -5, 0, 0.45f, 0, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SquirrelRenderState state) {
		super.setupAnim(state);
		float upright = state.uprightAmount;
		this.body.xRot = -1.25f * upright;
		this.body.y = Mth.lerp(upright, 18.5f, 17.0f);
		this.body.z = Mth.lerp(upright, 1.0f, 2.6f);
		this.head.y = Mth.lerp(upright, 16.5f, 10.5f);
		this.head.z = Mth.lerp(upright, -5.0f, 0.2f);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float stride = Mth.cos(state.walkAnimationPos * 0.9f) * state.walkAnimationSpeed * 0.8f * (1 - upright);
		this.hindRight.xRot = stride;
		this.hindLeft.xRot = -stride;
		this.frontLeft.xRot = stride - upright;
		this.frontRight.xRot = -stride - upright;
		this.frontLeft.y = this.frontRight.y = Mth.lerp(upright, 20, 14.3f);
		this.frontLeft.z = this.frontRight.z = Mth.lerp(upright, -2, -0.5f);
		this.frontLeft.zRot = -0.12f * upright;
		this.frontRight.zRot = 0.12f * upright;
		this.frontRight.xRot = Mth.lerp(state.giveProgress, this.frontRight.xRot, -Mth.HALF_PI);
		this.frontRight.zRot *= 1 - state.giveProgress;
		this.tail.yRot = Mth.sin(state.ageInTicks * 0.12f) * 0.10f;
		this.tail.zRot = Mth.sin(state.ageInTicks * 0.08f) * 0.08f;
		this.tailMiddle.zRot = Mth.sin(state.ageInTicks * 0.08f - 0.5f) * 0.05f;
		this.tailTip.zRot = Mth.sin(state.ageInTicks * 0.08f - 1.0f) * 0.05f;
	}

	public void translateToGiftPaw(PoseStack poseStack) {
		this.frontRight.translateAndRotate(poseStack);
		poseStack.translate(0, 3.7f / 16, 0);
		// Keep the bottle upright while the paw extends horizontally toward the player.
		poseStack.mulPose(Axis.XP.rotation(-this.frontRight.xRot));
		poseStack.translate(0, -0.15f, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees(180));
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		poseStack.scale(0.6f, 0.6f, 0.6f);
	}
}
