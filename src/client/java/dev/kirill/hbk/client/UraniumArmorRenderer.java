package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;

/** Replaces the vanilla armour skin with posed metal plates and full-bright reactor inserts. */
public final class UraniumArmorRenderer implements ArmorRenderer {
	private static final Identifier MATERIAL_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
	private final EnumMap<EquipmentSlot, EnumMap<UraniumArmorModel.Material, UraniumArmorModel>> models = new EnumMap<>(EquipmentSlot.class);

	public UraniumArmorRenderer(EntityRendererProvider.Context context) {
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			var materials = new EnumMap<UraniumArmorModel.Material, UraniumArmorModel>(UraniumArmorModel.Material.class);
			for (var material : UraniumArmorModel.Material.values()) {
				materials.put(material, new UraniumArmorModel(context.bakeLayer(ModEntityModelLayers.uraniumArmor(slot, material))));
			}
			models.put(slot, materials);
		}
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack,
			HumanoidRenderState state, EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> parent) {
		var materials = models.get(slot);
		if (materials == null) {
			return;
		}
		for (var material : UraniumArmorModel.Material.values()) {
			int color = switch (material) {
				case PLATES -> 0xFF344444;
				case EDGES -> 0xFF91A498;
				case URANIUM -> 0xFF9AFF26;
			};
			int materialLight = material == UraniumArmorModel.Material.URANIUM ? LightCoordsUtil.FULL_BRIGHT : light;
			// Copy joints during deferred rendering, not here: otherwise another entity's pose can leak in.
			ArmorRenderer.submitTransformCopyingModel(parent, state, materials.get(material), state, false,
					collector, poseStack, RenderTypes.armorCutoutNoCull(MATERIAL_TEXTURE), materialLight,
					OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
			if (stack.hasFoil() && material != UraniumArmorModel.Material.URANIUM) {
				ArmorRenderer.submitTransformCopyingModel(parent, state, materials.get(material), state, false,
						collector, poseStack, RenderTypes.armorEntityGlint(), light,
						OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
			}
		}
	}
}
