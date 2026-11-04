package dev.kirill.hbk.world;

import com.mojang.serialization.JsonOps;
import com.mojang.authlib.GameProfile;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class GodGameTests {
	@GameTest
	public void arrivalSpawnsExactlyTwoHundredBlocksAwayAndDoesNotDuplicate(GameTestHelper test) {
		var player = player(test);
		try {
			test.assertTrue(GodEncounter.start(player) == GodEncounter.StartResult.STARTED, "GOD must start");
			var chase = data(player).chase(player);
			test.assertTrue(Math.abs(chase.position().distanceTo(player.getBoundingBox().getCenter()) - 200) < 0.0001,
					"The initial world-space distance must be exactly 200 blocks");
			test.assertTrue(GodEncounter.start(player) == GodEncounter.StartResult.ALREADY_ACTIVE
					&& chase.equals(data(player).chase(player)), "Repeated /god must not reset or duplicate the pursuit");
			test.assertTrue(UnknownEncounter.start(player) == UnknownEncounter.StartResult.UNAVAILABLE,
					"The Unknown must not freeze a player during the GOD chase");
			var visuals = player.level().getEntities(ModEntityTypes.GOD, god -> god.position().distanceToSqr(player.position()) < 0.01);
			test.assertTrue(visuals.size() == 1 && !visuals.getFirst().shouldBeSaved()
					&& visuals.getFirst().position().add(visuals.getFirst().visualOffset()).distanceTo(chase.position()) < 0.001,
					"One temporary visual must represent the saved position without requiring a distant ticking chunk");
		} finally { cleanup(player); }
		test.succeed();
	}

	@GameTest
	public void naturalEligibilityUsesTheUnknownWalkingRules(GameTestHelper test) {
		var player = player(test);
		try {
			player.setOnGround(true);
			player.setKnownMovement(new Vec3(0.2, 0, 0));
			player.setLastClientInput(new Input(true, false, false, false, false, false, false));
			test.assertTrue(UnknownEncounter.WALKING_TRIGGER_CHANCE == 0.0001 && UnknownEncounter.canTriggerNaturally(player),
					"GOD walking eligibility: alive=" + player.isAlive() + ", creative=" + player.isCreative()
							+ ", spectator=" + player.isSpectator() + ", passenger=" + player.isPassenger() + ", ground=" + player.onGround()
							+ ", movement=" + player.getKnownMovement() + ", input=" + player.getLastClientInput());
			player.setLastClientInput(Input.EMPTY);
			test.assertFalse(UnknownEncounter.canTriggerNaturally(player), "Standing still must not roll the event");
			player.setGameMode(GameType.CREATIVE);
			test.assertFalse(UnknownEncounter.canTriggerNaturally(player), "Creative must be command-only");
		} finally { cleanup(player); }
		test.succeed();
	}

	@GameTest
	public void sprintSpeedPotionAndFastFlightAlwaysGainFourBlocksPerSecond(GameTestHelper test) {
		var player = player(test);
		try {
			double walking = GodEncounter.chaseStep(player);
			player.setSprinting(true);
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 4));
			double boosted = GodEncounter.chaseStep(player);
			test.assertTrue(boosted > walking, "Sprinting and Speed V must raise the pursuer's speed immediately");
			for (double speed : new double[]{0.5, 1, 3, 12}) {
				player.setKnownMovement(new Vec3(speed, 0, 0));
				double expected = Math.max(speed, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 2.1585) + 0.2;
				test.assertTrue(Math.abs(GodEncounter.chaseStep(player) - expected) < 1.0E-8,
						"Flight, vehicles and external acceleration must retain the 0.2 block/tick advantage");
			}
			GodEncounter.start(player);
			Vec3 direction = data(player).chase(player).position().subtract(player.getBoundingBox().getCenter()).normalize();
			for (int tick = 0; tick < 30; tick++) {
				player.setPos(player.position().subtract(direction.scale(3)));
				player.setKnownMovement(direction.scale(-3));
				data(player).advance(player);
			}
			test.assertTrue(Math.abs(data(player).chase(player).position().distanceTo(player.getBoundingBox().getCenter()) - 194) < 0.001,
					"Even a player moving at 60 blocks/s must lose exactly four blocks/s of separation");
		} finally { cleanup(player); }
		test.succeed();
	}

	@GameTest
	public void wallsAndLongTeleportsCannotEndTheChase(GameTestHelper test) {
		var player = player(test);
		try {
			GodEncounter.start(player);
			var chase = data(player).chase(player);
			BlockPos wall = BlockPos.containing(chase.position());
			for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-2, -2, -2), wall.offset(2, 2, 2))) {
				player.level().setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
			}
			double initial = chase.position().distanceTo(player.getBoundingBox().getCenter());
			for (int tick = 0; tick < 20; tick++) data(player).advance(player);
			test.assertTrue(data(player).chase(player).position().distanceTo(player.getBoundingBox().getCenter()) < initial - 8,
					"A bedrock enclosure must not block the nextbot");
			player.setPos(player.position().add(5000, 0, 0));
			Vec3 before = data(player).chase(player).position();
			for (int tick = 0; tick < 2401; tick++) data(player).advance(player);
			test.assertTrue(GodEncounter.isActive(player) && data(player).chase(player).position().distanceTo(before) > 990,
					"After more than two minutes and a long teleport, pursuit must continue without a timeout");
		} finally { cleanup(player); }
		test.succeed();
	}

	@GameTest
	public void saveReloadDimensionChangeAndRespawnPreservePursuit(GameTestHelper test) {
		var player = player(test);
		try {
			GodEncounter.start(player);
			for (int tick = 0; tick < 20; tick++) data(player).advance(player);
			var saved = GodEncounter.CODEC.encodeStart(JsonOps.INSTANCE, data(player)).getOrThrow();
			var loaded = GodEncounter.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
			test.assertTrue(loaded.chase(player).equals(data(player).chase(player)),
					"Saving must retain the owner, dimension and exact chase position for reconnect/restart");
			var center = player.getBoundingBox().getCenter();
			var switched = new GodEncounter.Chase("minecraft:the_nether", center.add(0, 0, 50), center);
			var json = saved.getAsJsonObject().deepCopy();
			json.add(player.getUUID().toString(), GodEncounter.Chase.CODEC.encodeStart(JsonOps.INSTANCE, switched).getOrThrow());
			var otherDimension = GodEncounter.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
			try {
				otherDimension.advance(player);
				test.assertTrue(Math.abs(otherDimension.chase(player).position().distanceTo(center) - (50 - GodEncounter.chaseStep(player))) < 0.001,
						"Portals must preserve the remaining distance instead of restarting a 200-block pursuit");
			} finally {
				player.setPos(otherDimension.chase(player).position());
				otherDimension.advance(player);
				player.setHealth(20);
			}
			player.setHealth(0);
			data(player).advance(player);
			test.assertTrue(GodEncounter.isActive(player), "An unrelated death must pause the pursuit until respawn");
			player.setHealth(20);
			Vec3 before = data(player).chase(player).position();
			data(player).advance(player);
			test.assertTrue(!before.equals(data(player).chase(player).position()), "Pursuit must resume with the living player");
		} finally { cleanup(player); }
		test.succeed();
	}

	@GameTest
	public void catchingKillsEvenInvulnerableCreativeAndAwardsOnlyTheVictim(GameTestHelper test) {
		var player = player(test);
		var bystander = player(test);
		try {
			player.setGameMode(GameType.CREATIVE);
			player.setInvulnerable(true);
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2000, 4));
			player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			GodEncounter.start(player);
			player.setPos(data(player).chase(player).position());
			data(player).advance(player);
			test.assertTrue(!player.isAlive() && !GodEncounter.isActive(player)
					&& player.getLastDamageSource().is(GodEncounter.GOD_DAMAGE),
					"Contact must kill, bypassing creative, resistance, invulnerability and totems, and end the chase");
			var advancement = player.level().getServer().getAdvancements().get(HbkMod.id("god"));
			test.assertTrue(advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()
					&& !bystander.getAdvancements().getOrStartProgress(advancement).isDone(),
					"Only the caught player must receive the Ivan Boyko advancement");
		} finally { cleanup(player); cleanup(bystander); }
		test.succeed();
	}

	private static ServerPlayer player(GameTestHelper test) {
		var server = test.getLevel().getServer();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(java.util.UUID.randomUUID(), "god-test"), false);
		var player = new ServerPlayer(server, test.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setGameMode(GameType.SURVIVAL);
		player.setNoGravity(true);
		player.snapTo(test.absoluteVec(new Vec3(2.5, 6, 2.5)));
		player.setKnownMovement(Vec3.ZERO);
		return player;
	}
	private static GodEncounter data(ServerPlayer player) { return GodEncounter.get(player.level().getServer()); }
	private static void cleanup(ServerPlayer player) {
		var chase = data(player).chase(player);
		if (chase != null) {
			player.setHealth(20);
			player.setGameMode(GameType.SURVIVAL);
			player.setPos(chase.position());
			data(player).advance(player);
		}
		player.level().getServer().getPlayerList().remove(player);
	}
}
