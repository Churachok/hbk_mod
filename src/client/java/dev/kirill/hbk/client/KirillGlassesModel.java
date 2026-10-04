package dev.kirill.hbk.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Slim spectacles attached to the animated head joint. */
public final class KirillGlassesModel extends HumanoidModel<HumanoidRenderState> {
	public enum Material { FRAME, LENSES }

	public KirillGlassesModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer(Material material) {
		var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
		var root = mesh.getRoot();
		root.clearRecursively();
		PartDefinition head = root.getChild("head");
		if (material == Material.FRAME) {
			box(head, "left_top", -3.9f, -4.6f, -5.05f, 3.3f, 0.55f, 0.55f);
			box(head, "right_top", 0.6f, -4.6f, -5.05f, 3.3f, 0.55f, 0.55f);
			box(head, "left_bottom", -3.9f, -2.0f, -5.05f, 3.3f, 0.55f, 0.55f);
			box(head, "right_bottom", 0.6f, -2.0f, -5.05f, 3.3f, 0.55f, 0.55f);
			box(head, "left_outer", -3.9f, -4.1f, -5.05f, 0.55f, 2.2f, 0.55f);
			box(head, "left_inner", -1.15f, -4.1f, -5.05f, 0.55f, 2.2f, 0.55f);
			box(head, "right_inner", 0.6f, -4.1f, -5.05f, 0.55f, 2.2f, 0.55f);
			box(head, "right_outer", 3.35f, -4.1f, -5.05f, 0.55f, 2.2f, 0.55f);
			box(head, "bridge", -0.65f, -3.7f, -5.05f, 1.3f, 0.45f, 0.55f);
			box(head, "left_temple", -4.3f, -4.25f, -4.65f, 0.55f, 0.55f, 7.8f);
			box(head, "right_temple", 3.75f, -4.25f, -4.65f, 0.55f, 0.55f, 7.8f);
		} else {
			box(head, "left_lens", -3.35f, -4.05f, -5.0f, 2.2f, 2.05f, 0.16f);
			box(head, "right_lens", 1.15f, -4.05f, -5.0f, 2.2f, 2.05f, 0.16f);
		}
		return LayerDefinition.create(mesh, 16, 16);
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z,
			float width, float height, float depth) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
				.addBox(x, y, z, width, height, depth), PartPose.ZERO);
	}
}
