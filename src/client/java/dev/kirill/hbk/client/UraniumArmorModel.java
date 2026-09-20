package dev.kirill.hbk.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

/** Articulated plated armour: each slot and material has its own immutable mesh. */
public final class UraniumArmorModel extends HumanoidModel<HumanoidRenderState> {
	public enum Material {
		PLATES, EDGES, URANIUM
	}

	public UraniumArmorModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer(EquipmentSlot slot, Material material) {
		var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
		var root = mesh.getRoot();
		// Keep the vanilla joint names/pivots, not its cubes or hat overlay.
		root.clearRecursively();
		switch (slot) {
			case HEAD -> helmet(root.getChild("head"), material);
			case CHEST -> chest(root, material);
			case LEGS -> leggings(root, material);
			case FEET -> boots(root, material);
			default -> throw new IllegalArgumentException("Not a humanoid armour slot: " + slot);
		}
		// Solid, gently textured material tiles rather than a flat painted skin.
		return LayerDefinition.create(mesh, 16, 16);
	}

	private static void helmet(PartDefinition head, Material material) {
		switch (material) {
			case PLATES -> {
				box(head, "crown", -4.6f, -8.8f, -4.6f, 9.2f, 2.0f, 9.2f);
				box(head, "back", -4.6f, -6.8f, 3.6f, 9.2f, 6.8f, 1.0f);
				box(head, "left_cheek", 3.6f, -6.8f, -4.6f, 1.2f, 7.0f, 8.2f);
				box(head, "right_cheek", -4.8f, -6.8f, -4.6f, 1.2f, 7.0f, 8.2f);
				box(head, "brow", -4.6f, -6.8f, -4.8f, 9.2f, 1.5f, 1.2f);
				box(head, "respirator", -1.5f, -3.0f, -5.2f, 3.0f, 3.0f, 1.6f);
				box(head, "forehead_plate", -2.1f, -8.3f, -5.1f, 4.2f, 3.2f, 0.8f);
			}
			case EDGES -> {
				box(head, "crest", -0.8f, -9.5f, -3.6f, 1.6f, 0.7f, 7.2f);
				box(head, "left_rim", 3.2f, -5.5f, -5.0f, 1.8f, 0.6f, 1.0f);
				box(head, "right_rim", -5.0f, -5.5f, -5.0f, 1.8f, 0.6f, 1.0f);
				box(head, "filter", -1.6f, -1.8f, -5.5f, 3.2f, 0.5f, 0.5f);
			}
			case URANIUM -> {
				box(head, "forehead_core", -0.65f, -7.8f, -5.7f, 1.3f, 2.3f, 0.7f);
				box(head, "left_channel", 4.0f, -4.8f, -4.9f, 0.6f, 3.6f, 0.6f);
				box(head, "right_channel", -4.6f, -4.8f, -4.9f, 0.6f, 3.6f, 0.6f);
			}
		}
	}

	private static void chest(PartDefinition root, Material material) {
		var body = root.getChild("body");
		switch (material) {
			case PLATES -> {
				box(body, "cuirass", -4.5f, 0.0f, -2.7f, 9.0f, 11.5f, 5.4f);
				angledBox(body, "left_breastplate", 2.0f, 4.0f, -3.0f, 0.0f, 0.0f, -0.15f, -1.7f, -3.0f, -0.6f, 3.4f, 6.0f, 1.2f);
				angledBox(body, "right_breastplate", -2.0f, 4.0f, -3.0f, 0.0f, 0.0f, 0.15f, -1.7f, -3.0f, -0.6f, 3.4f, 6.0f, 1.2f);
				box(body, "reactor_housing", -1.8f, 3.0f, -4.0f, 3.6f, 5.0f, 1.4f);
				box(body, "backpack", -3.0f, 1.5f, 2.7f, 6.0f, 7.0f, 1.8f);
			}
			case EDGES -> {
				box(body, "collar", -4.7f, -0.3f, -2.9f, 9.4f, 0.7f, 5.8f);
				box(body, "waist_rim", -4.7f, 10.8f, -2.9f, 9.4f, 0.7f, 5.8f);
				box(body, "core_top", -1.9f, 2.7f, -4.2f, 3.8f, 0.6f, 0.6f);
				box(body, "core_bottom", -1.9f, 7.7f, -4.2f, 3.8f, 0.6f, 0.6f);
			}
			case URANIUM -> {
				box(body, "reactor_crystal", -0.9f, 3.6f, -4.6f, 1.8f, 3.8f, 0.8f);
				angledBox(body, "left_conduit", 2.5f, 8.5f, -3.1f, 0.0f, 0.0f, -0.35f, -0.3f, -1.8f, -0.3f, 0.6f, 3.6f, 0.6f);
				angledBox(body, "right_conduit", -2.5f, 8.5f, -3.1f, 0.0f, 0.0f, 0.35f, -0.3f, -1.8f, -0.3f, 0.6f, 3.6f, 0.6f);
				box(body, "rear_core", -0.7f, 2.5f, 4.5f, 1.4f, 4.5f, 0.5f);
			}
		}
		for (String name : new String[]{"right_arm", "left_arm"}) {
			var arm = root.getChild(name);
			// Player models have both 3- and 4-pixel arms; centre the generous plate on either joint.
			float x = name.equals("right_arm") ? -1.0f : 1.0f;
			switch (material) {
				case PLATES -> {
					box(arm, "pauldron", x - 3.0f, -2.8f, -3.2f, 6.0f, 4.8f, 6.4f);
					box(arm, "vambrace", x - 2.4f, 4.2f, -2.5f, 4.8f, 5.5f, 5.0f);
				}
				case EDGES -> {
					box(arm, "shoulder_rim", x - 3.1f, 1.6f, -3.3f, 6.2f, 0.6f, 6.6f);
					box(arm, "wrist_rim", x - 2.5f, 9.3f, -2.6f, 5.0f, 0.6f, 5.2f);
				}
				case URANIUM -> {
					box(arm, "shoulder_core", x - 1.8f, -1.7f, -3.7f, 3.6f, 1.0f, 0.7f);
					box(arm, "forearm_channel", x - 0.35f, 5.0f, -3.0f, 0.7f, 3.5f, 0.7f);
				}
			}
		}
	}

	private static void leggings(PartDefinition root, Material material) {
		var body = root.getChild("body");
		switch (material) {
			case PLATES -> box(body, "belt", -4.6f, 11.5f, -2.8f, 9.2f, 1.5f, 5.6f);
			case EDGES -> box(body, "buckle", -1.6f, 11.6f, -3.3f, 3.2f, 1.3f, 0.7f);
			case URANIUM -> box(body, "belt_core", -0.7f, 11.8f, -3.7f, 1.4f, 0.8f, 0.5f);
		}
		for (String name : new String[]{"right_leg", "left_leg"}) {
			var leg = root.getChild(name);
			switch (material) {
				case PLATES -> {
					box(leg, "thigh_guard", -2.4f, -0.1f, -2.5f, 4.8f, 6.2f, 5.0f);
					box(leg, "knee_guard", -2.6f, 5.0f, -3.3f, 5.2f, 2.6f, 1.7f);
				}
				case EDGES -> box(leg, "knee_rim", -2.7f, 7.1f, -3.4f, 5.4f, 0.6f, 1.8f);
				case URANIUM -> {
					box(leg, "thigh_channel", -0.35f, 0.5f, -2.9f, 0.7f, 3.8f, 0.6f);
					box(leg, "knee_core", -0.8f, 5.4f, -3.8f, 1.6f, 1.4f, 0.6f);
				}
			}
		}
	}

	private static void boots(PartDefinition root, Material material) {
		for (String name : new String[]{"right_leg", "left_leg"}) {
			var leg = root.getChild(name);
			switch (material) {
				case PLATES -> {
					box(leg, "greave", -2.5f, 7.8f, -2.6f, 5.0f, 3.6f, 5.2f);
					box(leg, "toe_cap", -2.6f, 10.4f, -3.6f, 5.2f, 1.7f, 6.2f);
				}
				case EDGES -> box(leg, "sole", -2.7f, 11.5f, -3.7f, 5.4f, 0.7f, 6.4f);
				case URANIUM -> {
					box(leg, "shin_core", -0.5f, 8.2f, -3.0f, 1.0f, 2.3f, 0.6f);
					box(leg, "toe_channel", -1.6f, 10.5f, -4.0f, 3.2f, 0.6f, 0.5f);
				}
			}
		}
	}

	private static void box(PartDefinition parent, String name, float x, float y, float z, float width, float height, float depth) {
		angledBox(parent, name, 0, 0, 0, 0, 0, 0, x, y, z, width, height, depth);
	}

	private static void angledBox(PartDefinition parent, String name, float px, float py, float pz, float rx, float ry, float rz,
			float x, float y, float z, float width, float height, float depth) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, width, height, depth),
				PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
	}
}
