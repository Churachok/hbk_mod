package dev.kirill.hbk.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class FlyingCarpetModel extends EntityModel<FlyingCarpetRenderState> {
	public FlyingCarpetModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild(
				"carpet",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-16.0f, -1.5f, -12.0f, 32.0f, 2.0f, 24.0f)
						.texOffs(112, 0).addBox(-18.0f, -1.0f, -10.0f, 2.0f, 1.0f, 2.0f)
						.texOffs(112, 4).addBox(-18.0f, -1.0f, 8.0f, 2.0f, 1.0f, 2.0f)
						.texOffs(120, 0).addBox(16.0f, -1.0f, -10.0f, 2.0f, 1.0f, 2.0f)
						.texOffs(120, 4).addBox(16.0f, -1.0f, 8.0f, 2.0f, 1.0f, 2.0f),
				PartPose.offset(0.0f, 24.0f, 0.0f)
		);
		return LayerDefinition.create(mesh, 128, 32);
	}
}
