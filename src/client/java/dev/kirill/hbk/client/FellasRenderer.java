package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.FellasEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class FellasRenderer extends HumanoidMobRenderer<FellasEntity, GiantBossRenderState, GiantBossModel> {
	public FellasRenderer(EntityRendererProvider.Context context) {
		super(context, new GiantBossModel(context.bakeLayer(ModEntityModelLayers.GIANT_BOSS)), 0.45f);
	}
	@Override
	public GiantBossRenderState createRenderState() { return new GiantBossRenderState(); }
	@Override
	public Identifier getTextureLocation(GiantBossRenderState state) { return HbkMod.id("textures/entity/fellas.png"); }
	@Override
	protected HumanoidModel.ArmPose getArmPose(FellasEntity entity, HumanoidArm arm) {
		if (arm == entity.getMainArm() && entity.getMainHandItem().is(Items.CROSSBOW)) {
			return entity.isChargingCrossbow() ? HumanoidModel.ArmPose.CROSSBOW_CHARGE : HumanoidModel.ArmPose.CROSSBOW_HOLD;
		}
		return super.getArmPose(entity, arm);
	}
}
