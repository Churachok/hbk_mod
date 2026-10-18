package dev.kirill.hbk.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** The existing wolf anatomy, animated with the actual player's complete humanoid state. */
public final class FurryPlayerModel extends PlayerModel {
	private final ModelPart tail;

	public FurryPlayerModel() {
		super(PinkFurryWolfModel.createBodyLayer().bakeRoot(), false);
		this.tail = this.body.getChild("tail");
		this.hideHumanClothing();
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		// Keep vanilla held-item, swimming, riding, crouching and attack animations.
		super.setupAnim(state);
		this.hideHumanClothing();
		this.tail.yRot += Mth.sin(state.ageInTicks * 0.12f) * 0.12f;
		this.tail.xRot += Mth.cos(state.ageInTicks * 0.08f) * 0.03f;
	}

	private void hideHumanClothing() {
		this.hat.visible = false;
		this.jacket.visible = false;
		this.leftSleeve.visible = false;
		this.rightSleeve.visible = false;
		this.leftPants.visible = false;
		this.rightPants.visible = false;
	}

	public ModelPart firstPersonArm(boolean right) {
		this.resetPose();
		this.hideHumanClothing();
		ModelPart arm = right ? this.rightArm : this.leftArm;
		arm.visible = true;
		arm.zRot = right ? 0.1f : -0.1f;
		return arm;
	}
}
