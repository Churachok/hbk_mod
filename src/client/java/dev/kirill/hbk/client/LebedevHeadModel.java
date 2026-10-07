package dev.kirill.hbk.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class LebedevHeadModel extends EntityModel<EntityRenderState> {
	public LebedevHeadModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 16);
	}
}
