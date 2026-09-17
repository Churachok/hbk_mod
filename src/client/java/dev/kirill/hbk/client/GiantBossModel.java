package dev.kirill.hbk.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public class GiantBossModel extends HumanoidModel<GiantBossRenderState> {
	public GiantBossModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 64);
	}
}
