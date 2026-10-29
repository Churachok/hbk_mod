package dev.kirill.hbk.world;

import com.mojang.authlib.GameProfile;
import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.entity.SkippingOwlEntity;
import dev.kirill.hbk.mechanic.HuntersInstinct;
import dev.kirill.hbk.network.SkippingOwlLaughFinishedPayload;
import dev.kirill.hbk.network.SkippingOwlScenePayload;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.ChannelFutureListener;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Real event entities and connected players, with virtual scene deadlines instead of wall-clock waits. */
public final class SkippingOwlGameTests {
	@GameTest
	public void safeGroundAppearanceFacesItsOwnerAndRemainsOneInvulnerableTransientEntity(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		try {
			test.assertTrue(SkippingOwlEncounter.DURATION_TICKS == 360 && SkippingOwlEncounter.OMEN_TICKS == 240
					&& SkippingOwlEncounter.PULSE_INTERVAL_TICKS == 40 && SkippingOwlEncounter.PULSE_DAMAGE == 0.0f
					&& SkippingOwlEncounter.CARD_HOLD_TICKS == 40 && SkippingOwlEncounter.LAUGH_DURATION_TICKS == 160
					&& SkippingOwlEncounter.FINALE_TICKS == 560 && SkippingOwlEncounter.CLIENT_FINISH_GRACE_TICKS == 100
					&& SkippingOwlEncounter.MAX_DURATION_TICKS == 660
					&& SkippingOwlEncounter.FINALE_DAMAGE == 1000.0f
					&& SkippingOwlEncounter.WORLD_TRIGGER_DISTANCE == 8.0 && SkippingOwlEntity.ACTIVATION_DISTANCE == 8.0,
					"The scene must jump for eighteen seconds, hold the card for two and play eight seconds of laughter before its one-thousand-HP finale");
			var owl = start(test, player);
			var offset = owl.position().subtract(player.position()).multiply(1, 0, 1);
			var forward = player.getLookAngle().multiply(1, 0, 1).normalize();
			var facing = owl.getLookAngle().multiply(1, 0, 1).normalize();
			var ground = owl.blockPosition().below();
			test.assertTrue(offset.length() >= 3.99 && offset.length() <= 6.01 && offset.normalize().dot(forward) > 0.99
					&& facing.dot(offset.normalize().scale(-1)) > 0.99
					&& test.getLevel().getBlockState(ground).isFaceSturdy(test.getLevel(), ground, Direction.UP)
					&& test.getLevel().noCollision(owl),
					"An unobstructed first candidate must stand four to six blocks ahead on solid ground and face the owner");
			test.assertTrue(owl.getOwnerUUID().equals(player.getUUID()) && owl.getEncounterOwner() == player
					&& owl.isNoAi() && owl.isNoGravity() && owl.isInvulnerable() && !owl.shouldBeSaved()
					&& owl.getPerformanceAge(0) == 0.0f && owl.getPerformanceAge(0.5f) == 0.5f
					&& !owl.isPickable() && !owl.canBeHitByProjectile() && !owl.isPushable()
					&& !owl.isAttackable() && owl.skipAttackInteraction(player) && !owl.canAttack(player),
					"The owl itself must stay an untouchable owned stage prop, not an attack target, projectile obstacle, pushing mob or persistent NPC");
			float health = owl.getHealth();
			var sources = test.getLevel().damageSources();
			for (var source : new DamageSource[]{sources.playerAttack(player), sources.generic(), sources.onFire(),
					sources.lava(), sources.fall(), sources.drown(), sources.fellOutOfWorld(), sources.genericKill()}) {
				test.assertTrue(owl.isInvulnerableTo(test.getLevel(), source)
						&& !owl.hurtServer(test.getLevel(), source, Float.MAX_VALUE),
						"Melee, environmental damage and kill damage must be rejected");
			}
			var position = owl.position();
			var ownerMovement = player.getDeltaMovement();
			owl.push(player);
			owl.setDeltaMovement(0.7, 0.9, 0.4);
			owl.tick();
			owl.kill(test.getLevel());
			test.assertTrue(owl.position().distanceToSqr(position) < 0.000001 && owl.getDeltaMovement().lengthSqr() == 0
					&& player.getDeltaMovement().equals(ownerMovement) && owl.isAlive() && owl.getHealth() == health && owl.hurtTime == 0,
					"Native ticks must not let the cosmetic owl drift, fall, push its owner, die or play a hurt animation");
			test.assertTrue(owl.getPerformanceAge(0) == 0.0f,
					"Performance phase must follow synchronized world time, not how often this entity has ticked");
			test.assertTrue(SkippingOwlEncounter.start(player) == SkippingOwlEncounter.StartResult.ALREADY_ACTIVE
					&& SkippingOwlEncounter.activeOwl(player) == owl,
					"Repeated requests must retain the same owl instead of stacking scenes");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void lowCeilingsHazardousFloorsAndUnloadedCandidatesNeverForceASpawnOrAChunkLoad(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		try {
			for (var pos : BlockPos.betweenClosed(origin.offset(-9, 1, -9), origin.offset(9, 5, 9))) {
				test.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
			}
			assertBlocked(test, player, "A one-block-high gap and all candidate rooftops must reject the full owl clearance");
			arena(test);
			floor(test, origin, Blocks.MAGMA_BLOCK);
			assertBlocked(test, player, "The scene must not be placed on a magma floor");
			arena(test);
			for (var pos : BlockPos.betweenClosed(origin.offset(-9, 0, -9), origin.offset(9, 0, 9))) {
				test.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
			}
			assertBlocked(test, player, "Water covering otherwise solid ground must not become a spawn position");
			player.snapTo(29_000_000.5, origin.getY(), 29_000_000.5, 0, 0);
			var unloaded = player.blockPosition().south(5);
			test.assertTrue(!test.getLevel().hasChunkAt(unloaded), "The distant candidate must actually be outside loaded chunks");
			int chunksBefore = test.getLevel().getChunkSource().getLoadedChunksCount();
			assertBlocked(test, player, "An unloaded candidate must be skipped, not generated just to stage the scene");
			test.assertTrue(!test.getLevel().hasChunkAt(unloaded)
					&& test.getLevel().getChunkSource().getLoadedChunksCount() == chunksBefore,
					"Failed placement must not force-load any new chunk");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void deathDimensionDepartureLogoutAndExcessiveDistanceDiscardTheCachedOwlWithoutEntityTicks(GameTestHelper test) {
		var origin = arena(test);
		for (String reason : new String[]{"death", "dimension", "logout", "distance"}) {
			var player = player(test, origin);
			try {
				var owl = start(test, player);
				long now = test.getLevel().getServer().getTickCount();
				int entityTicks = owl.tickCount;
				switch (reason) {
					case "death" -> player.setHealth(0);
					case "dimension" -> {
						var other = test.getLevel().getServer().getLevel(Level.NETHER);
						test.assertTrue(other != null, "The native test server must expose its Nether dimension");
						player.setServerLevel(other);
					}
					case "logout" -> test.getLevel().getServer().getPlayerList().remove(player);
					case "distance" -> player.snapTo(owl.position().add(41, 0, 0));
					default -> throw new AssertionError(reason);
				}
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now);
				test.assertTrue(owl.isRemoved() && !SkippingOwlEncounter.isActive(player)
						&& SkippingOwlEncounter.activeOwl(player) == null && owl.tickCount == entityTicks,
						"Manager cleanup must discard its cached owl immediately after owner " + reason + ", without waiting for an entity tick");
			} finally {
				player.setServerLevel(test.getLevel());
				removePlayer(test, player);
			}
		}
		test.succeed();
	}

	@GameTest
	public void omenVisualWavesAndCardHoldWaitUntilLaughterFinishesBeforeOneLethalFinaleWithoutEntityTicks(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		try {
			var owl = start(test, player);
			long now = test.getLevel().getServer().getTickCount();
			int entityTicks = owl.tickCount;
			int food = player.getFoodData().getFoodLevel();
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 239);
			test.assertTrue(SkippingOwlEncounter.isActive(player) && !player.hasEffect(MobEffects.DARKNESS) && player.getHealth() == 20,
					"Neither darkness nor damage may occur before the exact twelfth second");
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 240);
			var darkness = player.getEffect(MobEffects.DARKNESS);
			test.assertTrue(darkness != null && darkness.getDuration() == 60 && darkness.getAmplifier() == 0
					&& player.getHealth() == 20 && player.getFoodData().getFoodLevel() == food,
					"The twelve-second omen must apply exactly sixty ticks of Darkness I without harming the owner before the laughter");
			player.removeEffect(MobEffects.DARKNESS);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 241);
			test.assertTrue(!player.hasEffect(MobEffects.DARKNESS) && player.getHealth() == 20,
					"The omen must be sent once, without refreshing darkness or damage between pulse boundaries");
			for (int elapsed : new int[]{279, 280, 319, 320, 359, 360, 399, 400, 559}) {
				clearDamageWindow(player);
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + elapsed);
				test.assertTrue(SkippingOwlEncounter.isActive(player) && !owl.isRemoved()
						&& player.isAlive() && player.getHealth() == 20 && player.getFoodData().getFoodLevel() == food,
						"Visual pulses, the final card, its two-second hold and the unfinished laughter must never hurt or retire the owner at tick " + elapsed);
			}
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
			test.assertTrue(owl.isRemoved() && !SkippingOwlEncounter.isActive(player)
					&& SkippingOwlEncounter.activeOwl(player) == null && owl.tickCount == entityTicks
					&& !player.isAlive() && player.getHealth() == 0 && player.getFoodData().getFoodLevel() == food,
					"The final wave must kill an unprotected owner only after the full hold and laughter at tick 560, even if the owl never ticked");
			test.assertTrue(player.getLastDamageSource() != null && player.getLastDamageSource().is(SkippingOwlEncounter.DAMAGE_TYPE),
					"The single actual hit after the laughter must use the registered skipping-owl death source");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void delayedCleanupSkipsMissedPulsesAndNeverRepeatsAnAlreadyDeliveredThreshold(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		try {
			var sounds = capturePackets(player, ClientboundSoundPacket.class);
			start(test, player);
			long now = test.getLevel().getServer().getTickCount();
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 319);
			test.assertTrue(player.getHealth() == 20 && waveCount(sounds) == 1,
					"Jumping over the first two thresholds must deliver only one visual sound wave, with no queued hits");
			// Native hit immunity must not conceal accidentally repeated damage or visual waves.
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 319);
			test.assertTrue(player.getHealth() == 20 && waveCount(sounds) == 1,
					"Repeating the same cleanup timestamp must not repeat a wave or cause damage");
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 320);
			test.assertTrue(player.getHealth() == 20 && waveCount(sounds) == 2,
					"The next newly reached threshold must deliver exactly one additional harmless visual wave");
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 359);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 359);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 360);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 400);
			test.assertTrue(player.getHealth() == 20 && waveCount(sounds) == 2 && SkippingOwlEncounter.isActive(player),
					"Repeated cleanups and the card/laughing phases must not repeat the last visual wave or damage the owner");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void crossingFortyBlocksCancelsBeforeTheFinalHitWhileTheExactBoundaryRemainsActive(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		try {
			var owl = start(test, player);
			long now = test.getLevel().getServer().getTickCount();
			player.snapTo(owl.position().add(40, 0, 0));
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 240);
			test.assertTrue(SkippingOwlEncounter.isActive(player) && player.getHealth() == 20,
					"Exactly forty blocks must remain inside the scene without pre-laughter damage");
			player.snapTo(owl.position().add(40.01, 0, 0));
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
			test.assertTrue(player.isAlive() && player.getHealth() == 20 && owl.isRemoved() && !SkippingOwlEncounter.isActive(player),
					"Crossing beyond forty blocks must cancel before a pending finale, not damage an owner who has escaped");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void adventureOwnerAloneTakesTheWaveWhileNearbyPlayersAnimalsAndBlocksRemainUntouched(GameTestHelper test) {
		var origin = arena(test);
		var owner = player(test, origin);
		var bystander = player(test, origin);
		var cow = EntityTypes.COW.create(test.getLevel(), EntitySpawnReason.EVENT);
		try {
			owner.setGameMode(GameType.ADVENTURE);
			bystander.snapTo(Vec3.atBottomCenterOf(origin.east(2)));
			var owl = start(test, owner);
			test.assertTrue(cow != null, "The bystander-animal fixture must be constructible");
			cow.setNoAi(true);
			cow.setNoGravity(true);
			cow.snapTo(owl.position().add(1, 0, 0));
			test.assertTrue(test.getLevel().addFreshEntity(cow), "The nearby cow must be an actual world entity");
			float animalHealth = cow.getHealth();
			test.getLevel().setBlockAndUpdate(origin.east(4), Blocks.GLASS.defaultBlockState());
			test.getLevel().setBlockAndUpdate(origin.west(4), Blocks.OAK_PLANKS.defaultBlockState());
			var blocks = blockSnapshot(test, origin);
			long now = test.getLevel().getServer().getTickCount();
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 240);
			test.assertTrue(owner.getHealth() == 20 && bystander.getHealth() == 20 && cow.getHealth() == animalHealth
					&& !bystander.hasEffect(MobEffects.DARKNESS),
					"Only the adventure owner must receive the omen; its visual wave must not damage any participant or bystander before the laughter");
			clearDamageWindow(owner);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
			test.assertTrue(!owner.isAlive() && bystander.isAlive() && bystander.getHealth() == 20
					&& cow.isAlive() && cow.getHealth() == animalHealth && owl.isRemoved(),
					"Even the lethal final wave must target only its owning player, not behave as an area explosion");
			for (var entry : blocks.entrySet()) {
				test.assertTrue(test.getLevel().getBlockState(entry.getKey()).equals(entry.getValue()),
						"Pulses and the final wave must preserve every nearby block, including fragile glass and wooden structures");
			}
		} finally {
			if (cow != null) cow.discard();
			removePlayer(test, owner);
			removePlayer(test, bystander);
		}
		test.succeed();
	}

	@GameTest
	public void creativeAndSpectatorPlayersNeverTakePulseOrFinalDamage(GameTestHelper test) {
		var origin = arena(test);
		var creative = player(test, origin);
		try {
			creative.setGameMode(GameType.CREATIVE);
			var owl = start(test, creative);
			long now = test.getLevel().getServer().getTickCount();
			for (int elapsed : new int[]{240, 280, 320, 360, 400, 559, SkippingOwlEncounter.FINALE_TICKS}) {
				clearDamageWindow(creative);
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + elapsed);
				test.assertTrue(creative.isAlive() && creative.getHealth() == 20 && !creative.hasEffect(MobEffects.DARKNESS),
						"An explicitly summoned creative scene must never damage or darken its owner, even at the finale");
			}
			test.assertTrue(owl.isRemoved() && !SkippingOwlEncounter.isActive(creative),
					"Creative immunity must not leave an event model after the card and laughter have completed");
		} finally {
			removePlayer(test, creative);
		}
		var spectator = player(test, origin);
		try {
			spectator.setGameMode(GameType.SPECTATOR);
			test.assertTrue(SkippingOwlEncounter.start(spectator) == SkippingOwlEncounter.StartResult.UNAVAILABLE,
					"A spectator must never start a damaging owl scene");
			spectator.setGameMode(GameType.SURVIVAL);
			var owl = start(test, spectator);
			spectator.setGameMode(GameType.SPECTATOR);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(),
					(long) test.getLevel().getServer().getTickCount() + 360);
			test.assertTrue(owl.isRemoved() && !SkippingOwlEncounter.isActive(spectator) && spectator.getHealth() == 20,
					"Switching into spectator must cancel before either a pending pulse or finale can hurt the owner");
		} finally {
			removePlayer(test, spectator);
		}
		test.succeed();
	}

	@GameTest
	public void registeredDamagePreservesOrdinaryArmorResistanceAndInvulnerability(GameTestHelper test) {
		var origin = arena(test);
		var source = new DamageSource(test.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(SkippingOwlEncounter.DAMAGE_TYPE));
		test.assertTrue(!source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& !source.is(DamageTypeTags.BYPASSES_COOLDOWN) && !source.is(DamageTypeTags.BYPASSES_EFFECTS)
				&& !source.is(DamageTypeTags.BYPASSES_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS),
				"The owl damage type must not be inserted into any ordinary-defense bypass tags");
		for (String protection : new String[]{"armor", "resistance", "invulnerability"}) {
			var player = player(test, origin);
			try {
				switch (protection) {
					case "armor" -> {
						player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
						// Native equipment attributes are collected during the living/player tick.
						player.doTick();
						test.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 8,
								"The armor fixture must activate the chestplate's real native attribute modifiers before checking the registered damage source");
					}
					case "resistance" -> player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 1));
					case "invulnerability" -> player.setInvulnerable(true);
					default -> throw new AssertionError(protection);
				}
				var owl = start(test, player);
				long now = test.getLevel().getServer().getTickCount();
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 240);
				test.assertTrue(player.getHealth() == 20, "The encounter's warning wave must remain visual even when native defenses are equipped");
				// The new prelude has no damage; exercise the same registered damage source directly
				// with a small native hit so ordinary armor/resistance remain observable before the fatal finale.
				player.hurtServer(test.getLevel(), source, 4);
				if (protection.equals("armor")) {
					test.assertTrue(player.getHealth() > 16 && player.getHealth() < 20,
							"Real armor must reduce the registered damage source instead of being bypassed by a forced-health change");
				} else if (protection.equals("resistance")) {
					test.assertTrue(Math.abs(player.getHealth() - 17.6f) < 0.0001f,
							"Resistance II must reduce an ordinary four-HP hit from the registered source by forty percent");
				} else {
					SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
					test.assertTrue(player.getHealth() == 20 && player.isAlive() && owl.isRemoved(),
							"Native invulnerability must reject both an ordinary hit and the single finale without preventing event cleanup");
				}
			} finally {
				removePlayer(test, player);
			}
		}
		test.succeed();
	}

	@GameTest
	public void directExpiryDealsExactlyOneThousandDamageWithoutQueuedPulsesOrForcedDeath(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		try {
			player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
			player.setHealth(1024);
			test.assertTrue(player.getHealth() == 1024, "The high-health fixture must fit the native maximum-health attribute");
			var owl = start(test, player);
			long now = test.getLevel().getServer().getTickCount();
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
			test.assertTrue(player.isAlive() && player.getHealth() == 24 && !player.hasEffect(MobEffects.DARKNESS)
					&& owl.isRemoved() && !SkippingOwlEncounter.isActive(player),
					"Jumping directly to expiry must deal exactly one thousand ordinary damage, with no queued pulses, omen or forced kill");
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.MAX_DURATION_TICKS);
			test.assertTrue(player.isAlive() && player.getHealth() == 24,
					"The completed event must remain retired even if cleanup is called again at or after its finale timestamp");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void nativeTotemAndHuntersInstinctSaveTheFinaleWithoutAnySecondHit(GameTestHelper test) {
		var origin = arena(test);
		for (boolean hunter : new boolean[]{false, true}) {
			var player = player(test, origin);
			try {
				player.setHealth(3);
				if (hunter) {
					HuntersInstinct.drink(player);
				} else {
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
					player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
				}
				var owl = start(test, player);
				long now = test.getLevel().getServer().getTickCount();
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
				test.assertTrue(player.isAlive() && owl.isRemoved() && !SkippingOwlEncounter.isActive(player),
						"A genuine death-protection mechanic must be allowed to save the owner from the single final hit");
				if (hunter) {
					test.assertTrue(player.getHealth() == 2 && player.hasEffect(ModEffects.HUNTERS_INSTINCT)
							&& player.getEffect(ModEffects.HUNTERS_INSTINCT).getDuration()
							== HuntersInstinctEffect.DURATION_TICKS - HuntersInstinctEffect.RESCUE_COST_TICKS,
							"One Hunter rescue must retain one heart and spend exactly two minutes, not consume a queued series of rescues");
				} else {
					test.assertTrue(player.getHealth() == 1 && heldTotems(player) == 1,
							"Vanilla death protection must consume exactly one of two held totems and leave its one-health survivor alive");
				}
				float savedHealth = player.getHealth();
				clearDamageWindow(player);
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.MAX_DURATION_TICKS);
				test.assertTrue(player.isAlive() && player.getHealth() == savedHealth
						&& (hunter ? player.getEffect(ModEffects.HUNTERS_INSTINCT).getDuration()
						== HuntersInstinctEffect.DURATION_TICKS - HuntersInstinctEffect.RESCUE_COST_TICKS : heldTotems(player) == 1),
						"Rescue must finish the scene permanently, without another cleanup consuming protection or killing the survivor");
			} finally {
				removePlayer(test, player);
			}
		}
		test.succeed();
	}

	@GameTest
	public void handImmortalityMustStillBePresentWhenLaughterEndsAndIsNotConsumedByTheRescue(GameTestHelper test) {
		var origin = arena(test);
		for (boolean expired : new boolean[]{false, true}) {
			var player = player(test, origin);
			try {
				player.setHealth(1);
				player.addEffect(new MobEffectInstance(ModEffects.HAND_IMMORTALITY, 800, 0));
				if (!expired) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
				var owl = start(test, player);
				long now = test.getLevel().getServer().getTickCount();
				for (int elapsed : new int[]{240, 280, 320, 360, 400, 559}) {
					clearDamageWindow(player);
					SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + elapsed);
					test.assertTrue(player.isAlive() && player.getHealth() == 1 && SkippingOwlEncounter.isActive(player),
							"Even a one-HP owner must remain alive through every visual wave, the card hold and unfinished laughter");
				}
				if (expired) player.removeEffect(ModEffects.HAND_IMMORTALITY);
				clearDamageWindow(player);
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
				test.assertTrue(owl.isRemoved() && !SkippingOwlEncounter.isActive(player),
						"Both protected and unprotected outcomes must retire the completed scene");
				if (expired) {
					test.assertTrue(!player.isAlive() && player.getHealth() == 0,
							"An effect that ended before the laugh finished must not protect the final hit");
				} else {
					test.assertTrue(player.isAlive() && player.getHealth() == 1
							&& player.getEffect(ModEffects.HAND_IMMORTALITY) != null
							&& player.getEffect(ModEffects.HAND_IMMORTALITY).getDuration() == 800
							&& heldTotems(player) == 1 && player.getLastDamageSource() == null,
							"Active hand immortality must skip the hit outright, without spending the effect, health or a held totem");
				}
			} finally {
				removePlayer(test, player);
			}
		}
		test.succeed();
	}

	@GameTest
	public void finalDeadlinePolicyNeverAcceptsAnEarlyAcknowledgementAndBoundsMissingClientReplies(GameTestHelper test) {
		for (long elapsed : new long[]{0, 359, 360, 399, 400, 559, 560, 659, 660, 1000}) {
			for (boolean waitsForClient : new boolean[]{false, true}) {
				for (boolean acknowledged : new boolean[]{false, true}) {
					boolean expected = elapsed >= 560 && (!waitsForClient || acknowledged || elapsed >= 660);
					test.assertTrue(SkippingOwlEncounter.isFinaleReady(elapsed, waitsForClient, acknowledged) == expected,
							"The actual cleanup policy must wait for the full 28-second sequence, then require a capable client's reply or the five-second grace deadline");
				}
			}
		}
		test.succeed();
	}

	@GameTest
	public void laughterAcknowledgementCannotStealAnotherPlayersSceneSkipTheHoldOrAcknowledgeAnOldOwl(GameTestHelper test) {
		var origin = arena(test);
		var owner = player(test, origin);
		var other = player(test, origin);
		try {
			other.snapTo(Vec3.atBottomCenterOf(origin.east(2)));
			owner.addEffect(new MobEffectInstance(ModEffects.HAND_IMMORTALITY, 800, 0));
			test.assertTrue(!SkippingOwlEncounter.acknowledgeLaughFinished(owner, UUID.randomUUID()),
					"A completion packet without an active scene must be ignored");
			var owl = start(test, owner);
			var oldId = owl.getUUID();
			long now = test.getLevel().getServer().getTickCount();
			test.assertTrue(!SkippingOwlEncounter.acknowledgeLaughFinished(other, oldId)
					&& !SkippingOwlEncounter.acknowledgeLaughFinished(owner, UUID.randomUUID())
					&& SkippingOwlEncounter.acknowledgeLaughFinished(owner, oldId),
					"Only the owning player's exact active owl UUID may acknowledge the current scene");
			for (int elapsed : new int[]{360, 400, 559}) {
				SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + elapsed);
				test.assertTrue(owner.isAlive() && owner.getHealth() == 20
						&& SkippingOwlEncounter.activeOwl(owner) == owl && !owl.isRemoved(),
						"A deliberately early valid packet must not skip the final card hold or any part of the laughter");
			}
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + SkippingOwlEncounter.FINALE_TICKS);
			test.assertTrue(owner.isAlive() && owl.isRemoved() && !SkippingOwlEncounter.isActive(owner)
					&& !SkippingOwlEncounter.acknowledgeLaughFinished(owner, oldId),
					"Completion must retire its UUID permanently instead of accepting duplicate packets after rescue");
			var next = start(test, owner);
			test.assertTrue(!next.getUUID().equals(oldId)
					&& !SkippingOwlEncounter.acknowledgeLaughFinished(owner, oldId)
					&& !SkippingOwlEncounter.acknowledgeLaughFinished(other, next.getUUID())
					&& SkippingOwlEncounter.acknowledgeLaughFinished(owner, next.getUUID()),
					"A delayed old packet or another player's packet must not be mistaken for the replacement owl's completion");
		} finally {
			removePlayer(test, owner);
			removePlayer(test, other);
		}
		test.succeed();
	}

	@GameTest
	public void sceneClockAndLaughterCompletionPayloadsPreserveIdentityAndAllTimingFields(GameTestHelper test) {
		var sceneId = UUID.randomUUID();
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
		try {
			for (boolean active : new boolean[]{true, false}) {
				var scene = new SkippingOwlScenePayload(sceneId, 12345, 9_876_543_210L, active);
				SkippingOwlScenePayload.CODEC.encode(buffer, scene);
				var decoded = SkippingOwlScenePayload.CODEC.decode(buffer);
				test.assertTrue(scene.equals(decoded) && decoded.type() == SkippingOwlScenePayload.TYPE && !buffer.isReadable(),
						"Both start snapshots and stop packets must round-trip the exact UUID, entity ID, long world clock and active bit");
				buffer.clear();
			}
			var complete = new SkippingOwlLaughFinishedPayload(sceneId);
			SkippingOwlLaughFinishedPayload.CODEC.encode(buffer, complete);
			var decoded = SkippingOwlLaughFinishedPayload.CODEC.decode(buffer);
			test.assertTrue(complete.equals(decoded) && decoded.type() == SkippingOwlLaughFinishedPayload.TYPE && !buffer.isReadable(),
					"The serverbound completion must preserve its scene identity without an attacker-supplied duration or damage value");
		} finally {
			buffer.release();
		}
		test.succeed();
	}

	@GameTest
	public void actualOwlCommandStartsTheSceneButNativeSummonLeavesAPersistentWaitingOwl(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		SkippingOwlEntity unowned = null;
		try {
			var server = test.getLevel().getServer();
			var source = server.createCommandSourceStack().withLevel(test.getLevel()).withPosition(player.position())
					.withEntity(player).withSuppressedOutput();
			test.assertTrue(server.getCommands().getDispatcher().getRoot().getChild("owl") != null,
					"The real /owl command must be registered");
			server.getCommands().performPrefixedCommand(source, "owl");
			var owl = SkippingOwlEncounter.activeOwl(player);
			test.assertTrue(owl != null && SkippingOwlEncounter.isActive(player) && owl.getEncounterOwner() == player,
					"Executing the actual player command must create the same managed scene as the automatic event");
			owl.discard();
			SkippingOwlEncounter.cleanupEncounters(server, server.getTickCount());
			server.getCommands().performPrefixedCommand(source, "summon hbk:skipping_owl ~ ~ ~");
			var candidates = test.getLevel().getEntitiesOfClass(SkippingOwlEntity.class,
					player.getBoundingBox().inflate(2), entity -> !entity.isRemoved() && entity.getOwnerUUID() == null);
			test.assertTrue(candidates.size() == 1, "Native /summon must create one unowned fixture entity");
			unowned = candidates.getFirst();
			var position = unowned.position();
			player.snapTo(position.add(20, 0, 0));
			unowned.tickCount = 10;
			unowned.tick();
			test.assertTrue(!unowned.isRemoved() && unowned.isWaitingForPlayer() && unowned.shouldBeSaved()
					&& unowned.getOwnerUUID() == null && unowned.position().equals(position) && !SkippingOwlEncounter.isActive(player),
					"An unowned summoned owl must remain at its world position and wait persistently until a suitable player approaches");
		} finally {
			if (unowned != null) unowned.discard();
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void existingWorldOwlActivatesAtEightBlocksInPlaceAndCannotBeClaimedByTwoPlayers(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		var owl = waitingOwl(test, Vec3.atBottomCenterOf(origin.south(5)));
		SkippingOwlEntity spare = null;
		ServerPlayer second = null;
		try {
			var position = owl.position();
			var uuid = owl.getUUID();
			player.snapTo(position.add(0, 0, -8.01));
			owl.tickCount = 10;
			owl.tick();
			test.assertTrue(owl.isWaitingForPlayer() && owl.shouldBeSaved() && !SkippingOwlEncounter.isActive(player),
					"An already present owl must keep waiting while the nearest eligible player is outside eight blocks");
			player.snapTo(position.add(0, 0, -8));
			player.setOnGround(false);
			owl.tickCount = 20;
			owl.tick();
			test.assertTrue(SkippingOwlEncounter.activeOwl(player) == owl && !owl.isWaitingForPlayer() && !owl.shouldBeSaved()
					&& owl.getEncounterOwner() == player && owl.getUUID().equals(uuid) && owl.position().equals(position),
					"At exactly eight blocks the owl's real tick must attach the existing entity without requiring grounded feet or teleporting it");
			second = player(test, origin);
			test.assertTrue(!SkippingOwlEncounter.tryStartWorldEncounter(second, owl)
					&& !SkippingOwlEncounter.isActive(second) && owl.getEncounterOwner() == player,
					"A second nearby player must not steal or duplicate an already owned owl");
			spare = waitingOwl(test, Vec3.atBottomCenterOf(origin.west(5)));
			test.assertTrue(!SkippingOwlEncounter.tryStartWorldEncounter(player, spare) && spare.isWaitingForPlayer()
					&& SkippingOwlEncounter.activeOwl(player) == owl,
					"An already participating player must not consume another waiting owl or replace the active scene");
		} finally {
			if (spare != null) spare.discard();
			owl.discard();
			removePlayer(test, player);
			if (second != null) removePlayer(test, second);
		}
		test.succeed();
	}

	@GameTest
	public void waitingWorldOwlIgnoresCreativeSpectatorPassengersOtherDimensionsAndBlockedSight(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		var owl = waitingOwl(test, Vec3.atBottomCenterOf(origin.south(5)));
		var vehicle = EntityTypes.PIG.create(test.getLevel(), EntitySpawnReason.EVENT);
		try {
			for (var mode : new GameType[]{GameType.CREATIVE, GameType.SPECTATOR}) {
				player.setGameMode(mode);
				assertWorldOwlKeepsWaiting(test, player, owl, "World encounters must ignore players in " + mode);
			}
			player.setGameMode(GameType.SURVIVAL);
			test.assertTrue(vehicle != null, "The native passenger fixture must be constructible");
			vehicle.setNoAi(true);
			vehicle.setNoGravity(true);
			vehicle.snapTo(player.position());
			test.assertTrue(test.getLevel().addFreshEntity(vehicle) && player.startRiding(vehicle, true, false) && player.isPassenger(),
					"The fixture must be a genuine mounted ServerPlayer before testing passenger rejection");
			assertWorldOwlKeepsWaiting(test, player, owl, "A riding player must not activate a waiting world owl");
			player.stopRiding();
			player.snapTo(Vec3.atBottomCenterOf(origin));
			var other = test.getLevel().getServer().getLevel(Level.NETHER);
			test.assertTrue(other != null, "The native test server must expose the Nether");
			player.setServerLevel(other);
			test.assertTrue(!SkippingOwlEncounter.tryStartWorldEncounter(player, owl) && owl.isWaitingForPlayer(),
					"An equally close player in another dimension must not claim this owl");
			player.setServerLevel(test.getLevel());
			for (var pos : BlockPos.betweenClosed(origin.offset(-2, 0, 2), origin.offset(2, 4, 2))) {
				test.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
			}
			assertWorldOwlKeepsWaiting(test, player, owl, "A solid wall must block both direct attachment and the owl's own nearest-player scan");
			for (var pos : BlockPos.betweenClosed(origin.offset(-2, 0, 2), origin.offset(2, 4, 2))) {
				test.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
			player.setGameMode(GameType.ADVENTURE);
			player.setOnGround(false);
			owl.tickCount = 20;
			owl.tick();
			test.assertTrue(SkippingOwlEncounter.activeOwl(player) == owl && !owl.isWaitingForPlayer(),
					"Removing the wall must let an eligible airborne adventure player activate the same waiting entity");
		} finally {
			player.stopRiding();
			player.setServerLevel(test.getLevel());
			if (vehicle != null) vehicle.discard();
			owl.discard();
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void idleWorldOwlSurvivesDistanceDespawnAndNativeSaveReloadWithoutLeavingTheCreatureCap(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		var owl = waitingOwl(test, Vec3.atBottomCenterOf(origin.south(5)));
		SkippingOwlEntity restored = null;
		try {
			player.snapTo(owl.position().add(256, 0, 0));
			owl.checkDespawn();
			test.assertTrue(!owl.isRemoved() && owl.shouldBeSaved() && !owl.removeWhenFarAway(256 * 256)
					&& !owl.isPersistenceRequired() && !owl.requiresCustomPersistence()
					&& owl.getType().getCategory() == MobCategory.CREATURE,
					"Waiting owls must resist distance despawn without PersistenceRequired or custom-persistence flags that exempt creatures from the cap");
			var position = owl.position();
			var uuid = owl.getUUID();
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
			owl.saveWithoutId(output);
			restored = ModEntityTypes.SKIPPING_OWL.create(test.getLevel(), EntitySpawnReason.LOAD);
			test.assertTrue(restored != null, "A saved owl must be constructible for native reload");
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), output.buildResult()));
			owl.discard();
			test.assertTrue(test.getLevel().addFreshEntity(restored), "The restored owl must rejoin the world after its old instance is removed");
			restored.tickCount = 10;
			restored.tick();
			restored.checkDespawn();
			test.assertTrue(!restored.isRemoved() && restored.isWaitingForPlayer() && restored.shouldBeSaved()
					&& restored.getOwnerUUID() == null && restored.getEncounterOwner() == null && restored.getPerformanceAge(0) == 0
					&& restored.getUUID().equals(uuid) && restored.position().equals(position)
					&& !restored.isPersistenceRequired() && !restored.requiresCustomPersistence(),
					"Native NBT reload must retain the same waiting owl, position and UUID without inventing an owner, timer or cap exemption");
		} finally {
			owl.discard();
			if (restored != null) restored.discard();
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void nativeCreatureSpawnRegistrationUsesRareSingleOwlsAndChecksSafeLoadedGround(GameTestHelper test) {
		var origin = arena(test);
		test.assertTrue(ModEntityTypes.SKIPPING_OWL.getCategory() == MobCategory.CREATURE
				&& SpawnPlacements.getPlacementType(ModEntityTypes.SKIPPING_OWL) == SpawnPlacementTypes.ON_GROUND
				&& SpawnPlacements.getHeightmapType(ModEntityTypes.SKIPPING_OWL) == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				"Owls must use the native creature cap and on-ground leaf-free heightmap placement");
		var biomes = test.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		var entries = biomes.getOrThrow(Biomes.PLAINS).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()
				.stream().filter(entry -> entry.value().type() == ModEntityTypes.SKIPPING_OWL).toList();
		test.assertTrue(entries.size() == 1 && entries.getFirst().weight() == 4
				&& entries.getFirst().value().minCount() == 1 && entries.getFirst().value().maxCount() == 1,
				"An ordinary Overworld biome must contain one weight-four entry spawning single owls");
		for (var biome : List.of(Biomes.NETHER_WASTES, Biomes.THE_END)) {
			test.assertTrue(biomes.getOrThrow(biome).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
					.noneMatch(entry -> entry.value().type() == ModEntityTypes.SKIPPING_OWL),
					"Nether and End biome tables must not receive natural owls");
		}
		test.assertTrue(canSpawnOwl(test, origin), "The actual registered native predicate must allow safe dry ground");
		var actual = ModEntityTypes.SKIPPING_OWL.create(test.getLevel(), EntitySpawnReason.NATURAL);
		test.assertTrue(actual != null, "The instance-level native spawn validator must be constructible");
		try {
			test.getLevel().setBlockAndUpdate(origin.below().west(), Blocks.AIR.defaultBlockState());
			actual.snapTo(Vec3.atBottomCenterOf(origin));
			test.assertTrue(canSpawnOwl(test, origin) && actual.checkSpawnRules(test.getLevel(), EntitySpawnReason.NATURAL),
					"A centered candidate may fit the supported side of a narrow ledge");
			actual.snapTo(origin.getX(), origin.getY(), origin.getZ() + 0.5);
			test.assertTrue(!actual.checkSpawnRules(test.getLevel(), EntitySpawnReason.NATURAL),
					"Native chunk generation's integer-coordinate final position must be revalidated instead of trusting the earlier centered candidate");
		} finally {
			actual.discard();
			test.getLevel().setBlockAndUpdate(origin.below().west(), Blocks.STONE.defaultBlockState());
		}
		test.getLevel().setBlockAndUpdate(origin.above(2), Blocks.STONE.defaultBlockState());
		test.assertTrue(!canSpawnOwl(test, origin), "A low ceiling must reject the full rope clearance during native spawning");
		test.getLevel().setBlockAndUpdate(origin.above(2), Blocks.AIR.defaultBlockState());
		test.getLevel().setBlockAndUpdate(origin, Blocks.WATER.defaultBlockState());
		test.assertTrue(!canSpawnOwl(test, origin), "The native predicate must reject liquid inside the owl's clearance");
		test.getLevel().setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
		test.getLevel().setBlockAndUpdate(origin.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
		test.assertTrue(!canSpawnOwl(test, origin), "The native predicate must reject hazardous supporting ground");
		var unloaded = new BlockPos(29_000_000, origin.getY(), 29_000_000);
		int chunks = test.getLevel().getChunkSource().getLoadedChunksCount();
		test.assertTrue(!test.getLevel().hasChunkAt(unloaded) && !canSpawnOwl(test, unloaded)
				&& !test.getLevel().hasChunkAt(unloaded) && test.getLevel().getChunkSource().getLoadedChunksCount() == chunks,
				"Even the real SpawnPlacements predicate must reject unloaded ground without forcing chunks");
		test.succeed();
	}

	@GameTest
	public void introAndOmenAreOwnerOnlyOrdinaryChatPacketsWithoutAnyActionBarWarning(GameTestHelper test) {
		var origin = arena(test);
		var player = player(test, origin);
		var bystander = player(test, origin);
		try {
			bystander.snapTo(Vec3.atBottomCenterOf(origin.east(2)));
			var ownerChat = captureSystemChat(player);
			var bystanderChat = captureSystemChat(bystander);
			start(test, player);
			long now = test.getLevel().getServer().getTickCount();
			test.assertTrue(ownerChat.size() == 1 && chatKey(ownerChat.getFirst()).equals("message.hbk.skipping_owl.intro")
					&& !ownerChat.getFirst().overlay() && bystanderChat.isEmpty(),
					"Starting the scene must send only the introductory chat line to its owner, with no escape warning or action-bar packet");
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 239);
			test.assertTrue(ownerChat.size() == 1, "The ominous chat continuation must not arrive before the twelfth second");
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 240);
			clearDamageWindow(player);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 240);
			SkippingOwlEncounter.cleanupEncounters(test.getLevel().getServer(), now + 241);
			test.assertTrue(ownerChat.stream().map(SkippingOwlGameTests::chatKey).toList().equals(List.of(
					"message.hbk.skipping_owl.intro", "message.hbk.skipping_owl.omen"))
					&& ownerChat.stream().noneMatch(ClientboundSystemChatPacket::overlay) && bystanderChat.isEmpty(),
					"The omen must be exactly one ordinary-chat continuation, never an overlay and never broadcast to bystanders");
		} finally {
			removePlayer(test, player);
			removePlayer(test, bystander);
		}
		test.succeed();
	}

	private static SkippingOwlEntity waitingOwl(GameTestHelper test, Vec3 position) {
		var owl = ModEntityTypes.SKIPPING_OWL.create(test.getLevel(), EntitySpawnReason.NATURAL);
		test.assertTrue(owl != null, "The native world owl must be constructible");
		owl.snapTo(position);
		test.assertTrue(test.getLevel().addFreshEntity(owl) && owl.isWaitingForPlayer() && owl.shouldBeSaved(),
				"A new world owl must begin as a real saved waiting entity without an owning encounter");
		return owl;
	}

	private static void assertWorldOwlKeepsWaiting(GameTestHelper test, ServerPlayer player, SkippingOwlEntity owl, String message) {
		test.assertTrue(!SkippingOwlEncounter.tryStartWorldEncounter(player, owl), message);
		owl.tickCount = 10;
		owl.tick();
		test.assertTrue(owl.isWaitingForPlayer() && owl.shouldBeSaved() && !SkippingOwlEncounter.isActive(player), message);
	}

	private static boolean canSpawnOwl(GameTestHelper test, BlockPos position) {
		return SpawnPlacements.checkSpawnRules(ModEntityTypes.SKIPPING_OWL, test.getLevel(), EntitySpawnReason.NATURAL,
				position, RandomSource.create(0x4f574cL));
	}

	private static List<ClientboundSystemChatPacket> captureSystemChat(ServerPlayer player) {
		return capturePackets(player, ClientboundSystemChatPacket.class);
	}

	private static long waveCount(List<ClientboundSoundPacket> sounds) {
		return sounds.stream().filter(sound -> sound.getSound().value() == SoundEvents.WARDEN_SONIC_BOOM).count();
	}

	private static <T extends Packet<?>> List<T> capturePackets(ServerPlayer player, Class<T> packetType) {
		var packets = new ArrayList<T>();
		player.connection = new ServerGamePacketListenerImpl(player.level().getServer(), new Connection(PacketFlow.SERVERBOUND),
				player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
			@Override
			public void send(Packet<?> packet) {
				if (packetType.isInstance(packet)) packets.add(packetType.cast(packet));
			}
			@Override
			public void send(Packet<?> packet, ChannelFutureListener listener) {
				// ServerPlayer's system-chat path sends with a failure callback, unlike ordinary packets.
				this.send(packet);
			}
		};
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return packets;
	}

	private static String chatKey(ClientboundSystemChatPacket packet) {
		return packet.content().getContents() instanceof TranslatableContents translated ? translated.getKey() : packet.content().getString();
	}

	private static SkippingOwlEntity start(GameTestHelper test, ServerPlayer player) {
		test.assertTrue(SkippingOwlEncounter.start(player) == SkippingOwlEncounter.StartResult.STARTED,
				"The clear grounded fixture must start a real owl event");
		var owl = SkippingOwlEncounter.activeOwl(player);
		test.assertTrue(owl != null && !owl.isRemoved() && SkippingOwlEncounter.isActive(player),
				"A started event must expose its real tracked owl");
		return owl;
	}

	private static void assertBlocked(GameTestHelper test, ServerPlayer player, String message) {
		test.assertTrue(SkippingOwlEncounter.start(player) == SkippingOwlEncounter.StartResult.BLOCKED
				&& !SkippingOwlEncounter.isActive(player) && SkippingOwlEncounter.activeOwl(player) == null, message);
	}

	private static BlockPos arena(GameTestHelper test) {
		var origin = test.absolutePos(new BlockPos(4, 12, 4));
		for (var pos : BlockPos.betweenClosed(origin.offset(-9, -3, -9), origin.offset(9, 5, 9))) {
			test.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		floor(test, origin, Blocks.STONE);
		return origin;
	}

	private static void floor(GameTestHelper test, BlockPos origin, Block block) {
		for (var pos : BlockPos.betweenClosed(origin.offset(-9, -1, -9), origin.offset(9, -1, 9))) {
			test.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
		}
	}

	private static Map<BlockPos, BlockState> blockSnapshot(GameTestHelper test, BlockPos origin) {
		var blocks = new LinkedHashMap<BlockPos, BlockState>();
		for (var pos : BlockPos.betweenClosed(origin.offset(-9, -1, -9), origin.offset(9, 3, 9))) {
			blocks.put(pos.immutable(), test.getLevel().getBlockState(pos));
		}
		return blocks;
	}

	private static int heldTotems(ServerPlayer player) {
		int count = 0;
		for (var hand : InteractionHand.values()) {
			var stack = player.getItemInHand(hand);
			if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
		}
		return count;
	}

	private static void clearDamageWindow(ServerPlayer player) {
		// Cleanup timestamps jump by forty or more ticks without ticking the player.
		// Clear only native hit immunity, as ordinary intervening player ticks would.
		player.invulnerableTime = 0;
	}

	private static ServerPlayer player(GameTestHelper test, BlockPos origin) {
		var server = test.getLevel().getServer();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "owl-test"), false);
		var player = new ServerPlayer(server, test.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		test.assertTrue(player.connection.hasClientLoaded(), "The damage fixture must finish native client-loading immunity");
		clearDamageWindow(player);
		player.setNoGravity(true);
		player.snapTo(Vec3.atBottomCenterOf(origin), 0, 0);
		player.setOnGround(true);
		player.getInventory().clearContent();
		return player;
	}

	private static void removePlayer(GameTestHelper test, ServerPlayer player) {
		var server = test.getLevel().getServer();
		if (server.getPlayerList().getPlayer(player.getUUID()) == player) server.getPlayerList().remove(player);
		SkippingOwlEncounter.cleanupEncounters(server, server.getTickCount());
		player.removeEffect(MobEffects.DARKNESS);
	}
}
