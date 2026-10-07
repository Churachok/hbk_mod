package dev.kirill.hbk.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Block-built marshrutka, with transparent windows and an actual interior. Coordinates are pixels. */
public final class SovietBusModel extends EntityModel<SovietBusRenderer.State> {
	public enum Material { BODY, GLASS, LIGHTS }
	private final ModelPart[] wheels;

	public SovietBusModel(ModelPart root, Material material) {
		super(root);
		this.wheels = material == Material.BODY ? new ModelPart[]{root.getChild("wheel_0"), root.getChild("wheel_1"),
				root.getChild("wheel_2"), root.getChild("wheel_3")} : new ModelPart[0];
	}

	@Override
	public void setupAnim(SovietBusRenderer.State state) {
		super.setupAnim(state);
		for (ModelPart wheel : wheels) {
			wheel.xRot = state.wheelRotation;
		}
	}

	public static LayerDefinition createLayer(Material material) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		if (material == Material.GLASS) {
			box(root, "windshield", 512, 128, -18, -43.9f, -43.2f, 36, 19.8f, 0.5f);
			box(root, "back_window", 512, 128, -18, -43.9f, 49.2f, 36, 18.8f, 0.5f);
			for (int side : new int[]{-1, 1}) {
				box(root, "glass_" + side, 512, 128, side * 19.6f - 0.25f, -43.9f, -39.9f, 0.5f, 18.8f, 87.8f);
			}
		} else if (material == Material.LIGHTS) {
			for (int side : new int[]{-1, 1}) {
				box(root, "headlight_" + side, 768, 128, side < 0 ? -17 : 10, -21, -52.1f, 7, 6, 1);
				box(root, "front_indicator_" + side, 384, 128, side < 0 ? -20 : 17, -20, -52.1f, 3, 5, 1);
				box(root, "rear_lamp_" + side, 896, 128, side < 0 ? -20 : 17, -29, 50, 3, 10, 1);
				box(root, "rear_indicator_" + side, 384, 128, side < 0 ? -20 : 17, -19, 50, 3, 3, 1);
				box(root, "side_indicator_" + side, 384, 128, side * 21 - 0.5f, -16, 12, 1, 2, 4);
			}
		} else {
			box(root, "chassis", 512, 0, -20, -10, -49, 40, 4, 98);
			box(root, "floor", 128, 128, -18, -13, -47, 36, 3, 94);
			box(root, "roof", 0, 0, -20, -50, -44, 40, 4, 94);
			box(root, "roof_lip", 0, 0, -19, -46, -45, 38, 2, 1);
			box(root, "roof_hatch", 0, 0, -8, -51, -12, 16, 1, 19);
			box(root, "hood", 0, 0, -20, -24, -51, 40, 5, 8);
			box(root, "front", 0, 0, -20, -19, -51, 40, 9, 3);
			box(root, "back", 0, 0, -20, -23, 47, 40, 13, 3);
			box(root, "grille", 512, 0, -10, -21, -51.1f, 20, 9, 1);
			for (int row = 0; row < 3; row++) {
				box(root, "grille_slat_" + row, 0, 128, -9, -20.1f + row * 3, -51.7f, 18, 1, 1);
			}
			box(root, "badge", 0, 128, -1, -20, -52.2f, 2, 6, 0.5f);
			box(root, "front_bumper", 128, 128, -21, -12, -53, 42, 5, 4.8f);
			box(root, "rear_bumper", 128, 128, -21, -12, 48, 42, 5, 5);
			box(root, "front_plate", 128, 320, -16, -11, -53.1f, 32, 8, 1);
			box(root, "rear_plate", 128, 320, -16, -13, 52.1f, 32, 8, 1);
			box(root, "front_route", 128, 256, -15, -34, -44.1f, 14, 10, 1);
			box(root, "back_route", 128, 256, -16, -34, 49.5f, 14, 10, 1);
			box(root, "side_route", 256, 256, 19.7f, -39.2f, 0, 1, 14, 34);
			for (int side : new int[]{-1, 1}) {
				float x = side < 0 ? -20 : 18;
				// Lower panels leave both wheel arches genuinely open.
				box(root, "side_center_" + side, 0, 0, x, -23, -24, 2, 13, 42);
				box(root, "side_front_" + side, 0, 0, x, -19, -48, 2, 9, 4);
				box(root, "side_rear_" + side, 0, 0, x, -23, 38, 2, 13, 9);
				// Separate arch caps, not a second layer over the entire center panel.
				box(root, "front_arch_band_" + side, 0, 0, x, -23, -43, 2, 4, 19);
				box(root, "rear_arch_band_" + side, 0, 0, x, -23, 18, 2, 4, 20);
				box(root, "black_stripe_" + side, 512, 0, side * 20 - 0.5f, -17, -47.9f, 1, 2, 94.8f);
				box(root, "window_top_" + side, 512, 0, x, -46, -43, 2, 2, 91);
				box(root, "window_bottom_" + side, 512, 0, x, -25, -43, 2, 2, 91);
				for (int pillar = 0; pillar < 6; pillar++) {
					box(root, "pillar_" + side + "_" + pillar, 512, 0, x, -44, -43 + pillar * 18,
							2, 19, pillar == 5 ? 1 : 2);
				}
				box(root, "mirror_arm_" + side, 512, 0, side < 0 ? -25 : 20, -34, -43, 5, 2, 2);
				box(root, "mirror_" + side, 128, 128, side < 0 ? -27 : 24, -38, -45, 3, 8, 5);
			}
			box(root, "back_window_top", 512, 0, -20, -46, 48, 40, 2, 2);
			box(root, "back_window_bottom", 512, 0, -20, -25, 48, 40, 2, 2);
			box(root, "back_window_left", 512, 0, -20, -44, 48, 2, 19, 2);
			box(root, "back_window_right", 512, 0, 18, -44, 48, 2, 19, 2);
			box(root, "back_stripe", 512, 0, -16.9f, -17, 50.1f, 33.8f, 2, 0.5f);
			// Front door outline, handle, dashboard, driver's chair and passengers' seats.
			box(root, "door_seam", 128, 128, 20, -23, -25, 0.6f, 13, 1);
			box(root, "door_handle", 512, 0, 20.3f, -26, -29, 1, 2, 4);
			box(root, "door_step", 128, 128, 18, -9, -41, 4, 2, 16);
			box(root, "dashboard", 128, 128, -18, -29, -42, 36, 5, 7);
			box(root, "steering_column", 512, 0, -12, -29, -35, 2, 9, 2);
			box(root, "steering_wheel", 512, 0, -15, -31, -36, 8, 1, 6);
			seat(root, "driver", -15, -31);
			for (int row = 0; row < 4; row++) {
				seat(root, "left_seat_" + row, -17, -16 + row * 15);
				if (row > 0) seat(root, "right_seat_" + row, 5, -16 + row * 15);
			}
			for (int z : new int[]{-23, 11, 40}) {
				box(root, "handrail_" + z, 384, 128, 0, -44, z, 1, 31, 1);
			}
			box(root, "ceiling_rail", 384, 128, -0.25f, -43, -24, 1.5f, 1, 66);
			int wheel = 0;
			for (int side : new int[]{-1, 1}) {
				for (int z : new int[]{-34, 28}) {
					PartDefinition part = root.addOrReplaceChild("wheel_" + wheel++, CubeListBuilder.create(),
							PartPose.offset(side * 20, -8, z));
					// Octagonal tire made of adjacent sections, with no duplicate side faces.
					box(part, "tire_center", 512, 0, -3, -6, -8, 6, 12, 16);
					box(part, "tire_top", 512, 0, -3, -8, -6, 6, 2, 12);
					box(part, "tire_bottom", 512, 0, -3, 6, -6, 6, 2, 12);
					box(part, "rim", 0, 128, -3.1f, -4.9f, -5, 6.2f, 9.8f, 10);
					box(part, "hub", 128, 128, -3.2f, -1.9f, -2, 6.4f, 3.8f, 4);
				}
			}
		}
		return LayerDefinition.create(mesh, 1024, 512);
	}

	private static void seat(PartDefinition root, String name, float x, float z) {
		box(root, name + "_base", 128, 128, x, -19, z, 12, 6, 10);
		box(root, name + "_cushion", 256, 128, x, -23, z, 12, 4, 10);
		box(root, name + "_back", 256, 128, x, -36, z + 10, 12, 14, 3);
	}

	private static void box(PartDefinition parent, String name, int u, int v, float x, float y, float z,
			float width, float height, float depth) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
				.addBox(x, y, z, width, height, depth), PartPose.ZERO);
	}
}
