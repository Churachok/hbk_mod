package dev.kirill.hbk.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Simple cuboids matching the final squirrel concept in the Models chat. */
public final class SquirrelModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart frontLeft;
	private final ModelPart frontRight;
	private final ModelPart hindLeft;
	private final ModelPart hindRight;

	public SquirrelModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.tail = root.getChild("tail");
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
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float stride = Mth.cos(state.walkAnimationPos * 0.9f) * state.walkAnimationSpeed * 0.8f;
		this.frontLeft.xRot = this.hindRight.xRot = stride;
		this.frontRight.xRot = this.hindLeft.xRot = -stride;
		this.tail.zRot = Mth.sin(state.ageInTicks * 0.08f) * 0.06f;
	}
}
