package dev.kirill.hbk.client;

import com.mojang.authlib.GameProfile;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.UUID;

/** Real synchronized outlines/night vision, one HUD icon, and the server-triggered native stag activation. */
public final class HuntersInstinctClientGameTest implements FabricClientGameTest {
	private static final int REMOTE_PLAYER_ID = -2267;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(HuntersInstinctClientGameTest::verifyIcon);
		try (var world = context.worldBuilder().adjustSettings(ui -> {
			ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(ui.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
			ui.setGenerateStructures(false);
			ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			ui.setAllowCommands(true);
		}).create()) {
			var originalCamera = context.computeOnClient(client -> client.options.getCameraType());
			try {
				var server = world.getServer();
				server.runCommand("fill -40 179 -40 40 179 40 minecraft:stone");
				server.runCommand("fill -6 180 4 6 184 4 minecraft:white_concrete");
				server.runCommand("tp @a 0.5 180.25 0.5 0 0");
				server.runCommand("time set midnight");
				world.getConnection().waitForChunksRender();
				int[] ids = server.computeOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setGameMode(GameType.SURVIVAL);
					player.setNoGravity(true);
					player.setOnGround(false);
					player.removeAllEffects();
					player.getInventory().clearContent();
					var level = player.level();
					var sheep = spawn(level, EntityTypes.SHEEP, -3.5, 7.5);
					var cow = spawn(level, EntityTypes.COW, 0.5, 8.5);
					var zombie = spawn(level, EntityTypes.ZOMBIE, 3.5, 7.5);
					// Both horizontal deltas are below thirty, but the true spherical distance is above thirty.
					var farCow = spawn(level, EntityTypes.COW, 24.5, 24.5);
					var boat = spawn(level, EntityTypes.CHERRY_BOAT, -5.5, 2.5);
					var vanillaGlow = spawn(level, EntityTypes.COW, -7.5, 8.5);
					vanillaGlow.setGlowingTag(true);
					return new int[] {sheep.getId(), cow.getId(), zombie.getId(), farCow.getId(), boat.getId(), vanillaGlow.getId()};
				});
				context.waitFor(client -> client.level != null && client.player != null
						&& allPresent(client, ids) && client.level.getEntity(ids[5]).isCurrentlyGlowing(), 120);
				context.runOnClient(client -> {
					client.options.setCameraType(CameraType.FIRST_PERSON);
					var remote = new RemotePlayer(client.level, new GameProfile(new UUID(0, 2267), "HunterObserver"));
					remote.setId(REMOTE_PLAYER_ID);
					remote.snapTo(5.5, 180.25, 9.5, 0, 0);
					remote.setOldPosAndRot();
					remote.setNoGravity(true);
					client.level.addEntity(remote);
					verifyGlowing(client, ids, false);
				});
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.setItemInHand(InteractionHand.MAIN_HAND,
							new ItemStack(ModItems.JAGERMEISTER_BOTTLE).finishUsingItem(player.level(), player));
					player.inventoryMenu.broadcastChanges();
				});
				context.waitFor(client -> client.player.hasEffect(ModEffects.HUNTERS_INSTINCT)
						&& client.player.hasEffect(MobEffects.NAUSEA)
						&& client.gameRenderer.gameRenderState().lightmapRenderState.nightVisionEffectIntensity == 1.0f, 100);
				context.runOnClient(client -> {
					verifyGlowing(client, ids, true);
					verifyStagModel(client);
					if (!HuntersInstinctClient.hasNightVision(client.player)
							|| GameRenderer.nightVisionScale(client.player, 0.5f) != 1.0f
							|| client.player.hasEffect(MobEffects.NIGHT_VISION)
							|| client.player.hasEffect(MobEffects.STRENGTH) || client.player.hasEffect(MobEffects.REGENERATION)
							|| client.player.getActiveEffects().size() != 2) {
						throw new AssertionError("Hunter's Instinct must provide full native night vision and one own icon, without extra vanilla buff effects");
					}
				});
				capture(context, "hunters-instinct-night-vision-personal-outlines-and-hud", 5);

				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
					player.removeEffect(MobEffects.NAUSEA);
					player.removeEffect(ModEffects.HUNTERS_INSTINCT);
				});
				context.waitFor(client -> !client.player.hasEffect(ModEffects.HUNTERS_INSTINCT)
						&& client.player.hasEffect(MobEffects.NIGHT_VISION) && !client.player.hasEffect(MobEffects.NAUSEA), 100);
				context.runOnClient(client -> {
					verifyGlowing(client, ids, false);
					if (HuntersInstinctClient.hasNightVision(client.player)
							|| client.player.getEffect(MobEffects.NIGHT_VISION).getDuration() < 450
							|| GameRenderer.nightVisionScale(client.player, 0.5f) != 1.0f) {
						throw new AssertionError("Removing the custom effect must preserve independent vanilla night vision");
					}
				});
				capture(context, "hunters-instinct-removed-vanilla-glow-and-night-vision-preserved", 3);

				// A real ServerPlayer also has login spawn protection, independent of invulnerableTime.
				context.waitTicks(65);
				server.runOnServer(instance -> {
					var player = instance.getPlayerList().getPlayers().getFirst();
					player.removeAllEffects();
					player.addEffect(new MobEffectInstance(ModEffects.HUNTERS_INSTINCT, HuntersInstinctEffect.DURATION_TICKS));
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
					player.setHealth(1.0f);
					player.invulnerableTime = 0;
					player.hurtServer(player.level(), player.damageSources().generic(), 100.0f);
					if (!player.isAlive() || player.getHealth() != 2.0f
							|| player.getEffect(ModEffects.HUNTERS_INSTINCT) == null
							|| player.getEffect(ModEffects.HUNTERS_INSTINCT).getDuration()
									!= HuntersInstinctEffect.DURATION_TICKS - HuntersInstinctEffect.RESCUE_COST_TICKS) {
						throw new AssertionError("A real lethal server hit must be cancelled and spend exactly two minutes");
					}
				});
				context.waitFor(client -> isStagActivation(nativeActivation(client).item())
						&& nativeActivation(client).ticks() > 0, 100);
				capture(context, "hunters-instinct-server-rescue-native-stag-activation", 5);
				context.runOnClient(client -> {
					var activation = nativeActivation(client);
					if (!isStagActivation(activation.item()) || activation.ticks() <= 0
							|| client.player.isDeadOrDying()) {
						throw new AssertionError("The received rescue payload must animate the stag model on its render-only carrier, not an ordinary totem model");
					}
				});
			} finally {
				context.runOnClient(client -> {
					client.setScreenAndShow(null);
					client.options.setCameraType(originalCamera);
				});
			}
		}
	}

	private static <T extends Entity> T spawn(ServerLevel level, EntityType<T> type, double x, double z) {
		var entity = type.create(level, EntitySpawnReason.COMMAND);
		if (entity == null) throw new AssertionError("Could not create the client fixture entity: " + type);
		entity.snapTo(x, 180.25, z);
		entity.setNoGravity(true);
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setPersistenceRequired();
		}
		if (!level.addFreshEntity(entity)) throw new AssertionError("Could not spawn the client fixture entity: " + type);
		return entity;
	}

	private static boolean allPresent(Minecraft client, int[] ids) {
		for (int id : ids) if (client.level.getEntity(id) == null) return false;
		return true;
	}

	private static void verifyGlowing(Minecraft client, int[] ids, boolean active) {
		for (int index = 0; index < 3; index++) {
			var entity = client.level.getEntity(ids[index]);
			if (HuntersInstinctClient.shouldHighlight(entity) != active || client.shouldEntityAppearGlowing(entity) != active
					|| entity.isCurrentlyGlowing()
					|| (client.getEntityRenderDispatcher().extractEntity(entity, 0.5f).outlineColor != 0) != active) {
				throw new AssertionError("Nearby sheep, cow and hostile must gain only this viewer's actual render outline: " + entity);
			}
		}
		var remote = client.level.getEntity(REMOTE_PLAYER_ID);
		if (HuntersInstinctClient.shouldHighlight(remote) != active || client.shouldEntityAppearGlowing(remote) != active
				|| HuntersInstinctClient.shouldHighlight(client.player)) {
			throw new AssertionError("Other player avatars must be included, but the viewer must not highlight itself");
		}
		for (int index : new int[] {3, 4}) {
			var entity = client.level.getEntity(ids[index]);
			if (HuntersInstinctClient.shouldHighlight(entity) || client.shouldEntityAppearGlowing(entity)) {
				throw new AssertionError("The radius must be spherical and nonliving boats must never be added to the glow");
			}
		}
		var vanillaGlow = client.level.getEntity(ids[5]);
		if (!vanillaGlow.isCurrentlyGlowing() || !client.shouldEntityAppearGlowing(vanillaGlow)) {
			throw new AssertionError("Vanilla glowing must survive both applying and removing Hunter's Instinct");
		}
	}

	private static void verifyIcon(Minecraft client) {
		var sprite = client.getAtlasManager().get(new SpriteId(Sheets.GUI_SHEET, Hud.getMobEffectSprite(ModEffects.HUNTERS_INSTINCT)));
		if (!sprite.contents().name().equals(HbkMod.id("mob_effect/hunters_instinct"))
				|| sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())
				|| sprite.contents().width() != 64 || sprite.contents().height() != 64) {
			throw new AssertionError("Hunter's Instinct must load its own high-resolution sixty-four-pixel icon for the native eighteen-pixel HUD slot");
		}
	}

	private static void verifyStagModel(Minecraft client) {
		if (BuiltInRegistries.ITEM.containsKey(HbkMod.id("jagermeister_stag"))) {
			throw new AssertionError("The stag must be a render resource, never a separately registered item");
		}
		var carrier = HuntersInstinctClient.animationStack();
		if (!isStagActivation(carrier) || carrier == HuntersInstinctClient.animationStack()) {
			throw new AssertionError("Every animation must get a fresh vanilla totem carrier with the stag item-model override");
		}
		var model = new ItemStackRenderState();
		client.getItemModelResolver().updateForLiving(model, carrier, ItemDisplayContext.GUI, client.player);
		var particle = model.pickParticleMaterial(RandomSource.create(0));
		if (model.isEmpty() || particle == null || !particle.sprite().contents().name().equals(HbkMod.id("item/jagermeister_stag"))) {
			throw new AssertionError("The native activation carrier must still load the exact stag sprite, not the vanilla totem");
		}
	}

	private static boolean isStagActivation(ItemStack stack) {
		return stack.is(Items.TOTEM_OF_UNDYING)
				&& HbkMod.id("jagermeister_stag").equals(stack.get(DataComponents.ITEM_MODEL));
	}

	private static NativeActivation nativeActivation(Minecraft client) {
		try {
			var rendererField = GameRenderer.class.getDeclaredField("screenEffectRenderer");
			rendererField.setAccessible(true);
			var renderer = rendererField.get(client.gameRenderer);
			var itemField = ScreenEffectRenderer.class.getDeclaredField("itemActivationItem");
			var ticksField = ScreenEffectRenderer.class.getDeclaredField("itemActivationTicks");
			itemField.setAccessible(true);
			ticksField.setAccessible(true);
			var item = (ItemStack) itemField.get(renderer);
			return new NativeActivation(item == null ? ItemStack.EMPTY : item, ticksField.getInt(renderer));
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Unable to inspect verified Minecraft26.2 native activation state", error);
		}
	}

	private static void capture(ClientGameTestContext context, String name, int ticks) {
		context.waitTicks(ticks);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.takeScreenshot(name);
	}

	private record NativeActivation(ItemStack item, int ticks) {
	}
}
