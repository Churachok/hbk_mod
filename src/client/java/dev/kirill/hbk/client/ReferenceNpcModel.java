package dev.kirill.hbk.client;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class ReferenceNpcModel {
	private ReferenceNpcModel() {
	}

	public static LayerDefinition createAntonBodyLayer() {
		var mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		mesh.getRoot().getChild("head").addOrReplaceChild("hair_bun",
				CubeListBuilder.create().texOffs(8, 0).addBox(-1, -10, -1, 2, 2, 2), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}
}
