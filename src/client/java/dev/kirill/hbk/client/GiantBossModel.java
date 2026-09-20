package dev.kirill.hbk.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public class GiantBossModel extends HumanoidModel<GiantBossRenderState> {
	public GiantBossModel(ModelPart root) {
		super(root);
	}

	public GiantBossModel(ModelPart root, boolean showSkinOverlays) {
		this(root);
		// The supplied head overlays contain hair and a nurse cap; their unused
		// pixels are already transparent in the PNG skins.
		this.hat.visible = true;
		this.body.getChild("jacket").visible = showSkinOverlays;
		this.rightArm.getChild("right_sleeve").visible = showSkinOverlays;
		this.leftArm.getChild("left_sleeve").visible = showSkinOverlays;
		this.rightLeg.getChild("right_pants").visible = showSkinOverlays;
		this.leftLeg.getChild("left_pants").visible = showSkinOverlays;
	}

	public static LayerDefinition createBodyLayer() {
		// Use the 64x64 player skin UVs, including separate left limbs and clothing overlays.
		return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
	}
}
