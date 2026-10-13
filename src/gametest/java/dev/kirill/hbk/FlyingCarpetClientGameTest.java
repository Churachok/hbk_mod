package dev.kirill.hbk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.authlib.GameProfile;
import com.mojang.math.Axis;
import dev.kirill.hbk.client.FlyingCarpetPassengerRenderState;
import dev.kirill.hbk.entity.FlyingCarpetEntity;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Vector3f;

/** Mount through the real item/network path and capture the seated player above the carpet. */
public final class FlyingCarpetClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("tp @a 0.5 85 0.5 0 0");
			world.getConnection().waitForChunksRender();
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				double feetY = player.getY();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FLYING_CARPET));
				ModItems.FLYING_CARPET.use(player.level(), player, InteractionHand.MAIN_HAND);
				var carpet = (FlyingCarpetEntity) player.getVehicle();
				if (carpet == null || Math.abs(carpet.getY() - feetY) > 1.0E-6) {
					throw new AssertionError("Carpet must spawn at the player's feet");
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});
			context.waitFor(client -> client.player.getVehicle() instanceof FlyingCarpetEntity, 80);
			context.waitTicks(5);
			context.runOnClient(client -> {
				var player = client.player;
				var carpet = (FlyingCarpetEntity) player.getVehicle();
				if (Math.abs(player.getX() - carpet.getX()) > 0.02
						|| Math.abs(player.getZ() - carpet.getZ()) > 0.02) {
					throw new AssertionError("Seated player must stay centered on the carpet surface");
				}
				var state = (AvatarRenderState) client.getEntityRenderDispatcher().extractEntity(player, 1.0f);
				var model = ((AvatarRenderer<?>) client.getEntityRenderDispatcher().getRenderer(player)).getModel();
				double lowestLeg = lowestLegPoint(model, state);
				double surface = carpet.getBoundingBox().maxY;
				if (lowestLeg < surface - 0.001 || lowestLeg > surface + 0.035) {
					throw new AssertionError("Feet and pants must rest on the carpet, actual gap: " + (lowestLeg - surface));
				}
				var armor = new HumanoidModel<>(LayerDefinition.create(
						HumanoidModel.createMesh(new CubeDeformation(1.0f), 0), 64, 32).bakeRoot());
				if (lowestLegPoint(armor, state) < surface - 0.05) {
					throw new AssertionError("Armor boots must not protrude beneath the carpet");
				}
				// Use a detached observer camera for the screenshots below.
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				client.gui.hud.toggle();
			});
			// Minecraft intentionally hides LocalPlayer when a different entity owns
			// the camera. Use the real observed-player class for these observer views.
			int riderId = context.computeOnClient(client -> {
				var rider = new RemotePlayer(client.level, new GameProfile(new java.util.UUID(0, 67), "CarpetRider"));
				rider.setId(-67);
				rider.snapTo(client.player.position(), client.player.getYRot(), 0);
				rider.setOldPosAndRot();
				client.level.addEntity(rider);
				rider.startRiding(client.player.getVehicle(), true, false);
				client.player.getVehicle().positionRider(rider);
				return rider.getId();
			});
			setCamera(context, false);
			context.waitTicks(5);
			context.takeScreenshot("hbk-flying-carpet-front");
			setCamera(context, true);
			context.waitTicks(5);
			context.takeScreenshot("hbk-flying-carpet-side");
			server.runOnServer(instance -> {
				var player = instance.getPlayerList().getPlayers().getFirst();
				player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
				player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
			});
			context.waitFor(client -> client.player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS), 40);
			context.runOnClient(client -> {
				var rider = (RemotePlayer) client.level.getEntity(riderId);
				for (var slot : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
					rider.setItemSlot(slot, client.player.getItemBySlot(slot).copy());
				}
			});
			context.waitTicks(5);
			context.takeScreenshot("hbk-flying-carpet-armor");
			context.runOnClient(client -> client.level.getEntity(riderId).discard());
			server.runOnServer(instance -> instance.getPlayerList().getPlayers().getFirst().stopRiding());
			context.waitFor(client -> !client.player.isPassenger(), 40);
			context.runOnClient(client -> {
				var state = (AvatarRenderState) client.getEntityRenderDispatcher().extractEntity(client.player, 1.0f);
				if (((FlyingCarpetPassengerRenderState) state).hbk$isOnFlyingCarpet()) {
					throw new AssertionError("Carpet pose must be cleared after dismounting");
				}
				var model = ((AvatarRenderer<?>) client.getEntityRenderDispatcher().getRenderer(client.player)).getModel();
				model.setupAnim(state);
				if (Math.abs(model.leftLeg.xRot + Math.PI / 2) < 0.01) {
					throw new AssertionError("Normal leg animation must return after dismounting");
				}
				client.setCameraEntity(client.player);
				client.options.setCameraType(CameraType.FIRST_PERSON);
				client.gui.hud.toggle();
			});
		}
	}

	/** Inspect actual transformed vertices, including the skin's outer pants layer. */
	private static double lowestLegPoint(HumanoidModel<? super AvatarRenderState> model, AvatarRenderState state) {
		model.setupAnim(state);
		PoseStack pose = new PoseStack();
		pose.translate(state.x, state.y, state.z);
		pose.scale(state.scale, state.scale, state.scale);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - state.bodyRot));
		pose.scale(-0.9375f, -0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		double[] lowest = {Double.POSITIVE_INFINITY};
		model.root().visit(pose, (partPose, path, index, cube) -> {
			if (!path.contains("left_leg") && !path.contains("right_leg")) return;
			for (var polygon : cube.polygons) {
				for (var vertex : polygon.vertices()) {
					var point = new Vector3f(vertex.worldX(), vertex.worldY(), vertex.worldZ());
					partPose.pose().transformPosition(point);
					lowest[0] = Math.min(lowest[0], point.y);
				}
			}
		});
		return lowest[0];
	}

	/** View the rider as an observer, so screenshots also exercise rendering other players. */
	private static void setCamera(ClientGameTestContext context, boolean side) {
		context.runOnClient(client -> {
			var player = client.player;
			var carpet = (FlyingCarpetEntity) player.getVehicle();
			var camera = new ArmorStand(client.level, 0, 0, 0);
			float yaw = side ? 90.0f : 180.0f;
			// The detached camera adds four blocks behind this viewing entity.
			camera.snapTo(player.getX() + (side ? -1.4 : 0),
					carpet.getBoundingBox().maxY + 0.65 - camera.getEyeHeight(),
					player.getZ() + (side ? 0 : -1.4), yaw, 0);
			camera.setYHeadRot(yaw);
			camera.yHeadRotO = yaw;
			camera.setOldPosAndRot();
			client.setCameraEntity(camera);
		});
	}
}
