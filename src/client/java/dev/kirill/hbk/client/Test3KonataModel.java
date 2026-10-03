package dev.kirill.hbk.client;

import net.minecraft.client.model.geom.ModelPart;

public final class Test3KonataModel extends Test2KonataModel {
	private static final float ARM_THICKNESS_SCALE = 0.88f;

	public Test3KonataModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(GiantBossRenderState state) {
		super.setupAnim(state);
		thinArm(this.rightArm);
		thinArm(this.leftArm);
	}

	private static void thinArm(ModelPart arm) {
		arm.xScale = ARM_THICKNESS_SCALE;
		arm.yScale = 1.0f;
		arm.zScale = ARM_THICKNESS_SCALE;
	}
}
