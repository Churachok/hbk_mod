package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class KirillGlassesRenderer implements ArmorRenderer {
	private static final Identifier WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
	private final KirillGlassesModel frame;
	private final KirillGlassesModel lenses;

	public KirillGlassesRenderer(EntityRendererProvider.Context context) {
		frame = new KirillGlassesModel(context.bakeLayer(ModEntityModelLayers.kirillGlasses(KirillGlassesModel.Material.FRAME)));
		lenses = new KirillGlassesModel(context.bakeLayer(ModEntityModelLayers.kirillGlasses(KirillGlassesModel.Material.LENSES)));
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack,
			HumanoidRenderState state, EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> parent) {
		if (slot != EquipmentSlot.HEAD) {
			return;
		}
		ArmorRenderer.submitTransformCopyingModel(parent, state, frame, state, false,
				collector, poseStack, RenderTypes.armorCutoutNoCull(WHITE_TEXTURE), light,
				OverlayTexture.NO_OVERLAY, 0xFFF6F6F6, null, state.outlineColor, null);
		ArmorRenderer.submitTransformCopyingModel(parent, state, lenses, state, false,
				collector, poseStack, RenderTypes.armorCutoutNoCull(WHITE_TEXTURE), light,
				OverlayTexture.NO_OVERLAY, 0xFFFF2424, null, state.outlineColor, null);
	}
}
