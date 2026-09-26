package dev.kirill.hbk.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.boat.AbstractBoatModel;

/** A long single-seat boat carrying a blast furnace and an exhaust stack. */
public final class ImprovedBoatModel extends AbstractBoatModel {
	public ImprovedBoatModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("bottom",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-19.0f, -4.0f, -8.0f, 38.0f, 3.0f, 16.0f),
				PartPose.ZERO);
		root.addOrReplaceChild("left_side",
				CubeListBuilder.create().texOffs(0, 24)
						.addBox(-19.0f, -10.0f, -10.0f, 38.0f, 8.0f, 2.0f),
				PartPose.ZERO);
		root.addOrReplaceChild("right_side",
				CubeListBuilder.create().texOffs(0, 44)
						.addBox(-19.0f, -10.0f, 8.0f, 38.0f, 8.0f, 2.0f),
				PartPose.ZERO);
		root.addOrReplaceChild("front",
				CubeListBuilder.create().texOffs(0, 64)
						.addBox(-21.0f, -10.0f, -8.0f, 3.0f, 8.0f, 16.0f),
				PartPose.ZERO);
		root.addOrReplaceChild("back",
				CubeListBuilder.create().texOffs(32, 64)
						.addBox(18.0f, -10.0f, -8.0f, 3.0f, 8.0f, 16.0f),
				PartPose.ZERO);
		root.addOrReplaceChild("seat",
				CubeListBuilder.create().texOffs(0, 82)
						.addBox(4.0f, -7.0f, -8.0f, 6.0f, 2.0f, 16.0f),
				PartPose.ZERO);

		root.addOrReplaceChild("blast_furnace",
				CubeListBuilder.create().texOffs(64, 64)
						.addBox(-18.0f, -19.0f, -8.0f, 16.0f, 16.0f, 16.0f),
				PartPose.ZERO);
		root.addOrReplaceChild("chimney",
				CubeListBuilder.create().texOffs(64, 96)
						.addBox(-12.0f, -31.0f, -2.0f, 4.0f, 12.0f, 4.0f)
						.texOffs(82, 96).addBox(-13.0f, -33.0f, -3.0f, 6.0f, 3.0f, 6.0f),
				PartPose.ZERO);

		root.addOrReplaceChild("left_paddle",
				CubeListBuilder.create().texOffs(0, 100)
						.addBox(-1.0f, 0.0f, -5.0f, 2.0f, 2.0f, 18.0f)
						.addBox(-1.001f, -3.0f, 8.0f, 1.0f, 6.0f, 7.0f),
				PartPose.offsetAndRotation(-4.0f, -5.0f, 10.0f, 0.0f, 0.0f, 0.19634955f));
		root.addOrReplaceChild("right_paddle",
				CubeListBuilder.create().texOffs(32, 100)
						.addBox(-1.0f, 0.0f, -5.0f, 2.0f, 2.0f, 18.0f)
						.addBox(0.001f, -3.0f, 8.0f, 1.0f, 6.0f, 7.0f),
				PartPose.offsetAndRotation(-4.0f, -5.0f, -10.0f, 0.0f, (float) Math.PI, 0.19634955f));

		return LayerDefinition.create(mesh, 128, 128);
	}
}
