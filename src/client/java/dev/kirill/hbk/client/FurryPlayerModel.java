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
		if (((FurryPlayerRenderState) state).hbk$isFurrySprinting()
				&& !state.isPassenger && !state.isVisuallySwimming && state.swimAmount <= 0
				&& !state.isFallFlying && !state.isCrouching) {
			this.applyFourLeggedSprintPose(state);
		}
	}

	private void applyFourLeggedSprintPose(AvatarRenderState state) {
		float stride = Mth.cos(state.walkAnimationPos * 0.9f) * Math.min(0.9f, state.walkAnimationSpeed * 0.9f);
		this.body.xRot = 1.35f;
		this.body.y = this.head.y = 6.0f;
		this.head.z = -5.0f;
		this.head.xRot = 0.2f;
		this.rightArm.y = this.leftArm.y = 8.0f;
		this.rightArm.z = this.leftArm.z = -4.0f;
		this.rightArm.xRot = -0.8f + stride;
		this.leftArm.xRot = -0.8f - stride;
		this.rightLeg.z = this.leftLeg.z = 4.0f;
		this.rightLeg.xRot = 0.55f - stride;
		this.leftLeg.xRot = 0.55f + stride;
		this.tail.xRot = -0.2f;
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
