package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.authlib.GameProfile;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.PinkFurryWolfEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.mechanic.FurryTransformation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/** Native age synchronization and half-sized babies, with the separate held 3D item. */
public final class FurryBreedingClientGameTest implements FabricClientGameTest {
	private static final int REMOTE_FURRY_ID = -4421;
	private static final int REMOTE_HUMAN_ID = -4422;
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var camera = context.computeOnClient(client -> client.options.getCameraType());
			try {
				var server = world.getServer();
				server.runCommand("fill -16 179 -16 16 179 16 minecraft:stone");
				server.runCommand("tp @a 0.5 180 0.5 0 0");
				server.runCommand("time set day");
				world.getConnection().waitForChunksRender();
				int[] ids = server.computeOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.getInventory().clearContent();
					player.getInventory().setSelectedSlot(0);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FURRY_PLUG));
					player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.FURRY_PLUG));
					player.inventoryMenu.broadcastChanges();
					var adult = spawn(player.level(), -1.1, DyeColor.PINK, false);
					var baby = spawn(player.level(), 1.7, DyeColor.LIGHT_BLUE, true);
					return new int[] {adult.getId(), baby.getId()};
				});
				context.waitFor(client -> client.player != null && client.level.getEntity(ids[0]) instanceof PinkFurryWolfEntity adult
						&& client.level.getEntity(ids[1]) instanceof PinkFurryWolfEntity baby && !adult.isBaby() && baby.isBaby()
						&& adult.getFurColor() == DyeColor.PINK && baby.getFurColor() == DyeColor.LIGHT_BLUE
						&& client.player.getMainHandItem().is(ModItems.FURRY_PLUG)
						&& client.player.getOffhandItem().is(ModItems.FURRY_PLUG), 100);
				context.runOnClient(client -> {
					verifyEntities(client, ids);
					verifyPlugModel(client);
					client.options.setCameraType(CameraType.FIRST_PERSON);
				});
				capture(context, "furry-plug-gui-and-first-person-both-hands", 40);
				context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
				capture(context, "furry-plug-third-person-both-hands", 8);
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
					player.inventoryMenu.broadcastChanges();
				});
				context.waitFor(client -> client.player.getMainHandItem().isEmpty() && client.player.getOffhandItem().isEmpty(), 100);
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				capture(context, "furry-wolf-adult-and-half-size-baby", 40);

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					if (!FurryTransformation.start(player) || !FurryBreeding.enterLove(player)) {
						throw new AssertionError("The real server fixture must enter a transformed player's love mode");
					}
				});
				context.waitFor(client -> FurryLoveClientEffects.isActive(), 100);
				context.runOnClient(client -> {
					addObserver(client, REMOTE_FURRY_ID, "FurryObserver", -3.5, true);
					addObserver(client, REMOTE_HUMAN_ID, "HumanObserver", 3.5, false);
					verifyLoveVision(client, ids, true);
					verifyHearts(client, true);
				});
				Vec3 beforePull = context.computeOnClient(client -> client.player.position());
				context.waitTicks(30);
				double pulled = context.computeOnClient(client -> Math.hypot(client.player.getX() - beforePull.x,
						client.player.getZ() - beforePull.z));
				if (Math.abs(pulled - 1.5) > 0.5) {
					throw new AssertionError("Without input the real local movement must drift about 1.5 blocks in thirty ticks, got " + pulled);
				}
				double beforeInput = context.computeOnClient(client -> client.player.getZ());
				context.getInput().holdKey(options -> options.keyDown);
				context.waitTicks(12);
				context.getInput().releaseKey(options -> options.keyDown);
				if (context.computeOnClient(client -> client.player.getZ()) > beforeInput - 0.2) {
					throw new AssertionError("Ordinary backward input must overcome the additive pull, not be multiplied by the old input resistance");
				}
				server.runCommand("fill -6 180 3 6 184 3 minecraft:white_concrete");
				capture(context, "furry-love-red-vision-and-personal-outlines-through-wall", 10);
				capture(context, "furry-love-small-floating-hearts", 12);
				context.runOnClient(client -> {
					verifyLoveVision(client, ids, true);
					if (client.player.hasLineOfSight(client.level.getEntity(ids[0]))) {
						throw new AssertionError("The outline screenshot must actually put the furry behind an opaque wall");
					}
				});
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setAttached(ModAttachments.FURRY_LOVE_UNTIL, instance.overworld().getGameTime());
					FurryBreeding.tickPlayer(player);
				});
				context.waitFor(client -> !FurryLoveClientEffects.isActive(), 100);
				context.runOnClient(client -> verifyHearts(client, false));
				server.runCommand("fill -6 180 3 6 184 3 minecraft:air");
				context.waitTicks(5);
				Vec3 stopped = context.computeOnClient(client -> client.player.position());
				context.waitTicks(20);
				context.runOnClient(client -> {
					verifyLoveVision(client, ids, false);
					verifyHearts(client, false);
					if (Math.hypot(client.player.getX() - stopped.x, client.player.getZ() - stopped.z) > 0.05) {
						throw new AssertionError("The attraction must stop when love expires, without waiting for a reload");
					}
				});
				capture(context, "furry-love-ended-normal-vision", 2);

				server.runCommand("tp @a 8.5 180 -6.5 0 0");
				context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
				context.getInput().holdKey(options -> options.keyUp);
				context.getInput().holdKey(options -> options.keySprint);
				context.waitFor(client -> client.player.isSprinting(), 100);
				context.runOnClient(client -> verifySprintPose(client, true));
				capture(context, "furry-player-four-legged-sprint", 5);
				context.getInput().releaseKey(options -> options.keyUp);
				context.getInput().releaseKey(options -> options.keySprint);
				context.waitFor(client -> !client.player.isSprinting(), 100);
				context.runOnClient(client -> verifySprintPose(client, false));

				// Exercise the real seeded NPC/player outcome, then observe from outside
				// its ring. These are ordinary networked dropped items, not a fixture model.
				server.runCommand("tp @a 0.5 180 2.5 0 0");
				context.waitFor(client -> Math.abs(client.player.getZ() - 2.5) < 0.2, 100);
				context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					var level = player.level();
					var adult = (PinkFurryWolfEntity) level.getEntity(ids[0]);
					adult.setAge(0);
					adult.resetLove();
					player.removeAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL);
					int wolves = level.getEntities(ModEntityTypes.PINK_FURRY_WOLF, entity -> true).size();
					if (!FurryTransformation.start(player) || !FurryBreeding.enterLove(adult) || !FurryBreeding.enterLove(player)) {
						throw new AssertionError("The burst fixture must prepare a real ready adult/player pair");
					}
					player.getRandom().setSeed(0);
					if (!player.getRandom().nextBoolean()) throw new AssertionError("Seed zero must select the true burst branch");
					player.getRandom().setSeed(0);
					if (!FurryBreeding.tryBreed(adult, player)
							|| level.getEntities(ModEntityTypes.PINK_FURRY_WOLF, entity -> true).size() != wolves) {
						throw new AssertionError("A successful seeded burst must not create another furry-wolf baby");
					}
					var drops = level.getEntitiesOfClass(ItemEntity.class, adult.getBoundingBox().inflate(8),
							item -> item.getItem().is(ModItems.SHPERMA));
					if (drops.size() != 12 || drops.stream().anyMatch(item -> item.getItem().getCount() != 1
							|| !item.hasPickUpDelay() || item.getDeltaMovement().horizontalDistanceSqr() < 0.1)) {
						throw new AssertionError("The server must create twelve single normal material drops with radial velocity and pickup delay");
					}
					var advancement = instance.getAdvancements().get(HbkMod.id("torn_ace"));
					if (advancement == null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
						throw new AssertionError("The real burst participant must receive torn_ace");
					}
					player.setDeltaMovement(Vec3.ZERO);
					player.connection.teleport(0.5, 180, -0.5, 0, -8);
				});
				context.waitFor(client -> burstItems(client).size() >= 4
						&& burstItems(client).stream().mapToInt(item -> item.getItem().getCount()).sum() == 12
						&& Math.abs(client.player.getZ() + 0.5) < 0.2 && !FurryLoveClientEffects.isActive(), 100);
				// Packets normally arrive in one/two ticks; two more ticks expose
				// the native radial flight in its first three-to-five client ticks.
				context.waitTicks(2);
				context.runOnClient(FurryBreedingClientGameTest::verifyRadialDrops);
				context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
				context.takeScreenshot("furry-player-seeded-radial-material-item-drops");
				capture(context, "furry-player-torn-ace-advancement-toast", 8);
			} finally {
				context.getInput().releaseKey(options -> options.keyDown);
				context.getInput().releaseKey(options -> options.keyUp);
				context.getInput().releaseKey(options -> options.keySprint);
				context.runOnClient(client -> {
					client.options.setCameraType(camera);
					client.setScreenAndShow(null);
				});
			}
		}
	}

	private static List<ItemEntity> burstItems(Minecraft client) {
		var result = new ArrayList<ItemEntity>();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof ItemEntity item && !item.isRemoved() && item.getItem().is(ModItems.SHPERMA)) result.add(item);
		}
		return result;
	}

	private static void verifyRadialDrops(Minecraft client) {
		var items = burstItems(client);
		if (items.size() < 4 || items.stream().mapToInt(item -> item.getItem().getCount()).sum() != 12
				|| items.stream().anyMatch(item -> item.tickCount > 10 || item.getY() <= 180.8)) {
			throw new AssertionError("The client must see the twelve real material units spread into separate airborne ItemEntities, not a projectile");
		}
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		for (ItemEntity item : items) {
			if (!(client.getEntityRenderDispatcher().getRenderer(item) instanceof ItemEntityRenderer)) {
				throw new AssertionError("Radial material drops must use the ordinary vanilla ItemEntityRenderer");
			}
			var state = (ItemEntityRenderState) client.getEntityRenderDispatcher().extractEntity(item, 0.5f);
			var particle = state.item.pickParticleMaterial(RandomSource.create(0));
			if (state.item.isEmpty() || particle == null
					|| !particle.sprite().contents().name().equals(HbkMod.id("item/shperma"))) {
				throw new AssertionError("Each real drop must retain the existing two-white-drops material sprite");
			}
			minX = Math.min(minX, item.getX()); maxX = Math.max(maxX, item.getX());
			minZ = Math.min(minZ, item.getZ()); maxZ = Math.max(maxZ, item.getZ());
		}
		if (maxX - minX < 0.6 || maxZ - minZ < 0.6) throw new AssertionError("The screenshot must catch a visible radial ring in both horizontal axes");
	}

	private static void verifyHearts(Minecraft client, boolean active) {
		int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
		var first = FurryLoveClientEffects.heartsForHud(width, height, 100);
		if (!active) {
			if (!first.isEmpty()) throw new AssertionError("Ending love must remove all HUD hearts immediately");
			return;
		}
		var later = FurryLoveClientEffects.heartsForHud(width, height, 120);
		var partial = FurryLoveClientEffects.heartsForHud(width, height, 100.5f);
		if (first.size() != 8 || later.getFirst().y() >= first.getFirst().y()
				|| partial.getFirst().y() >= first.getFirst().y()
				|| Math.abs(partial.getFirst().y() - first.getFirst().y()) > 2
				|| partial.getFirst().alpha() != first.getFirst().alpha()) {
			throw new AssertionError("A small deterministic set of HUD hearts must float continuously with partial ticks, without flashing");
		}
		for (var heart : first) {
			if (heart.x() < 0 || heart.x() + 7 > width || heart.y() < 20 || heart.y() + 6 > height - 42
					|| !(heart.x() + 7 < width * 0.3f || heart.x() > width * 0.7f)
					|| heart.alpha() < 0 || heart.alpha() > 0.85f) {
				throw new AssertionError("Small hearts must remain on the periphery, outside the crosshair and inventory/hotbar");
			}
		}
	}

	private static void addObserver(Minecraft client, int id, String name, double x, boolean furry) {
		var observer = new RemotePlayer(client.level, new GameProfile(new UUID(0, -id), name));
		observer.setId(id);
		observer.snapTo(x, 180, 7, 180, 0);
		observer.setOldPosAndRot();
		observer.setNoGravity(true);
		if (furry) {
			observer.setAttached(ModAttachments.FURRY_COLOR, DyeColor.PURPLE.getId());
			observer.setSprinting(true);
		}
		client.level.addEntity(observer);
		var state = (AvatarRenderState) client.getEntityRenderDispatcher().extractEntity(observer, 0.5f);
		if (((FurryPlayerRenderState) state).hbk$isFurrySprinting() != furry) {
			throw new AssertionError("Remote transformed avatars must snapshot their actual synchronized sprint flag too");
		}
	}

	private static void verifyLoveVision(Minecraft client, int[] ids, boolean active) {
		if (FurryLoveClientEffects.isActive() != active || FurryLoveClientEffects.shouldHighlight(client.player)
				|| (FurryLoveClientEffects.OVERLAY_COLOR >>> 24) > 80
				|| ((FurryLoveClientEffects.OVERLAY_COLOR >>> 16) & 255) < 150) {
			throw new AssertionError("Love vision must be a transparent red wash for the owner only, never a black screen or self outline");
		}
		for (int id : new int[] {ids[0], ids[1], REMOTE_FURRY_ID}) {
			Entity entity = client.level.getEntity(id);
			if (FurryLoveClientEffects.shouldHighlight(entity) != active || client.shouldEntityAppearGlowing(entity) != active
					|| entity.isCurrentlyGlowing()
					|| (client.getEntityRenderDispatcher().extractEntity(entity, 0.5f).outlineColor != 0) != active) {
				throw new AssertionError("Only this viewer's render must highlight furry NPCs/babies and transformed players through walls");
			}
		}
		if (FurryLoveClientEffects.shouldHighlight(client.level.getEntity(REMOTE_HUMAN_ID))) {
			throw new AssertionError("Ordinary players must not receive the furry outline");
		}
	}

	private static void verifySprintPose(Minecraft client, boolean sprinting) {
		var state = (AvatarRenderState) client.getEntityRenderDispatcher().extractEntity(client.player, 0.5f);
		if (((FurryPlayerRenderState) state).hbk$isFurrySprinting() != sprinting) {
			throw new AssertionError("The actual local sprint flag must be extracted into the deferred avatar state");
		}
		var model = new FurryPlayerModel();
		model.setupAnim(state);
		if ((model.body.xRot == 1.35f) != sprinting) throw new AssertionError("Only sprinting furry avatars must use the four-legged pose");
		if (sprinting) {
			state.isPassenger = true;
			model.setupAnim(state);
			if (model.body.xRot == 1.35f) throw new AssertionError("Riding must retain the normal passenger pose");
			state.isPassenger = false;
			state.swimAmount = 1;
			model.setupAnim(state);
			if (model.body.xRot == 1.35f) throw new AssertionError("Swimming must retain its native animation instead of the sprint pose");
		}
	}

	private static PinkFurryWolfEntity spawn(ServerLevel level, double x, DyeColor color, boolean baby) {
		var wolf = ModEntityTypes.PINK_FURRY_WOLF.create(level, EntitySpawnReason.COMMAND);
		if (wolf == null) throw new AssertionError("Unable to create the existing furry-wolf fixture");
		wolf.setAge(baby ? -24000 : 0);
		wolf.setFurColor(color);
		wolf.setNoAi(true);
		wolf.setPersistenceRequired();
		wolf.snapTo(x, 180, 5.0, 180, 0);
		wolf.yBodyRot = wolf.yBodyRotO = wolf.yHeadRot = wolf.yHeadRotO = 180;
		if (!level.addFreshEntity(wolf)) throw new AssertionError("Unable to spawn the furry-wolf fixture");
		return wolf;
	}

	private static void verifyEntities(Minecraft client, int[] ids) {
		var adult = (PinkFurryWolfEntity) client.level.getEntity(ids[0]);
		var baby = (PinkFurryWolfEntity) client.level.getEntity(ids[1]);
		var renderer = (PinkFurryWolfRenderer) client.getEntityRenderDispatcher().getRenderer(baby);
		var adultState = renderer.createRenderState();
		var babyState = renderer.createRenderState();
		renderer.extractRenderState(adult, adultState, 0.5f);
		renderer.extractRenderState(baby, babyState, 0.5f);
		var adultPose = new PoseStack();
		var babyPose = new PoseStack();
		renderer.scale(adultState, adultPose);
		renderer.scale(babyState, babyPose);
		if (adultState.isBaby || !babyState.isBaby || adultState.ageScale != 1.0f || babyState.ageScale != 0.5f
				|| adultPose.last().pose().m00() != 1.0f || babyPose.last().pose().m00() != 0.5f
				|| Math.abs(baby.getBbHeight() / adult.getBbHeight() - 0.5f) > 0.001f
				|| babyState.furColorId != DyeColor.LIGHT_BLUE.getId()) {
			throw new AssertionError("The real native age flag, render geometry, hitbox and synchronized fur colour must identify a half-sized baby");
		}
		for (var color : DyeColor.VALUES) {
			babyState.furColorId = color.getId();
			var texture = HbkMod.id("textures/entity/pink_furry_wolf_" + color.getName() + ".png");
			if (!renderer.getTextureLocation(babyState).equals(texture)
					|| client.getResourceManager().getResource(texture).isEmpty()) {
				throw new AssertionError("Babies must reuse all sixteen intact adult colour textures: " + color);
			}
		}
		var model = renderer.getModel();
		babyState.fleeing = true;
		model.setupAnim(babyState);
		if (model.body.xRot != 1.35f) throw new AssertionError("Half-sized babies must retain the existing four-legged flee animation");
		babyState.fleeing = false;
		babyState.ageInTicks = 0;
		model.setupAnim(babyState);
		float wag = model.body.getChild("tail").yRot;
		babyState.ageInTicks = 12;
		model.setupAnim(babyState);
		if (wag == model.body.getChild("tail").yRot) throw new AssertionError("Baby tails must still gently wag");
	}

	private static void verifyPlugModel(Minecraft client) {
		for (var display : new ItemDisplayContext[] {ItemDisplayContext.GUI, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
				ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND}) {
			var state = new ItemStackRenderState();
			client.getItemModelResolver().updateForLiving(state, new ItemStack(ModItems.FURRY_PLUG), display, client.player);
			var particle = state.pickParticleMaterial(RandomSource.create(0));
			var bounds = state.getModelBoundingBox();
			if (state.isEmpty() || particle == null || particle.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())
					|| bounds.getXsize() < 0.01 || bounds.getYsize() < 0.01 || bounds.getZsize() < 0.01) {
				throw new AssertionError("The plug must load its separate real 3D item model in GUI and both hands: " + display);
			}
		}
	}

	private static void capture(ClientGameTestContext context, String name, int ticks) {
		context.waitTicks(ticks);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}
}
