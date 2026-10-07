package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SovietBusEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SovietBusGameTests {
	@GameTest
	public void destinationLotteryUsesAllSixTypesThenKeepsNearestInstanceOrdering(GameTestHelper test) {
		RandomSource random = RandomSource.create(67L);
		int[] counts = new int[SovietBusEvent.Destination.values().length];
		for (int draw = 0; draw < 6000; draw++) {
			counts[SovietBusEvent.chooseDestination(random).ordinal()]++;
		}
		for (int count : counts) {
			test.assertTrue(count > 800 && count < 1200,
					"All six stop types must take part in the equal-probability lottery, not just nearby types");
		}
		// Deliberately unordered instances of ONE selected type: only here does distance matter.
		var candidates = new ArrayList<>(List.of(new ChunkPos(40, 40), new ChunkPos(2, 1),
				new ChunkPos(-12, 7), new ChunkPos(0, 0)));
		BusDestinationSearch.sortNearestFirst(candidates, new BlockPos(8, 64, 8));
		test.assertTrue(candidates.equals(List.of(new ChunkPos(0, 0), new ChunkPos(2, 1),
				new ChunkPos(-12, 7), new ChunkPos(40, 40))),
				"After random type selection, instances must be searched nearest-first, not randomly");
		test.succeed();
	}

	@GameTest
	public void boardingUsesLotteryResultAndNeverRerollsAnExistingTrip(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(320, 280, 320))));
			player.getRandom().setSeed(67L);
			var expected = SovietBusEvent.chooseDestination(RandomSource.create(67L));
			test.assertTrue(SovietBusEvent.startTrip(player), "Boarding must start exactly one randomly selected trip");
			var search = SovietBusEvent.activeTrip(player);
			test.assertTrue(search != null && search.destination == expected,
					"The search must keep the lottery result, not choose whichever stop is nearest or available");
			player.setPos(player.position().add(1000, 0, -1000));
			test.assertFalse(SovietBusEvent.startTrip(player), "Repeated boarding must not replace a trip already being searched");
			test.assertTrue(SovietBusEvent.activeTrip(player) == search && search.destination == expected,
					"Moving or clicking again must not change the selected type, even if the search fails");
		} finally {
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void headHasBloodParticlesTurnsAndPicksUpWithoutSneakingOrEmptyHand(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var head = ModEntityTypes.LEBEDEV_HEAD.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		head.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(2, 3, 2))));
		level.addFreshEntity(head);
		test.assertTrue(head.getParticleType() instanceof DustParticleOptions dust
				&& dust.getColor().x > dust.getColor().y * 3 && dust.getColor().x > dust.getColor().z * 3,
				"Head must use red blood dust, not slime particles");
		head.setDeltaMovement(new Vec3(0.4, 0.1, 0));
		head.tick();
		test.assertTrue(Math.abs(head.getYRot() + 90) < 10 && head.getRoll(1) > 1,
				"Moving head must turn toward movement and roll");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		player.setShiftKeyDown(false);
		test.assertTrue(head.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
				&& head.isRemoved() && player.getMainHandItem().is(Items.STICK)
				&& player.getInventory().contains(stack -> stack.is(ModItems.LEBEDEV_HEAD)),
				"Normal right click with an occupied hand must recover exactly one head and preserve held item");
		test.succeed();
	}

	@GameTest
	public void busApproachesWaitsTwentySecondsAndEasesOutTwentyChunksInTwentySeconds(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos center = test.absolutePos(new BlockPos(100, 280, 100));
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-19, 0, -19), center.offset(19, 0, 19))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setPos(Vec3.atBottomCenterOf(center.above()));
			player.setYRot(0);
			player.setOnGround(true);
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"A clear loaded surface must allow bus event to start");
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.ALREADY_ACTIVE,
					"A player must not accumulate duplicate buses");
			var bus = SovietBusEvent.activeBus(player);
			test.assertTrue(bus != null && !bus.isRemoved(), "Exactly one bus must be spawned");
			test.assertTrue(bus.isApproaching() && !bus.isPickable()
					&& Math.abs(bus.getAnimationOffset(1).length() - 24) < 0.001,
					"Bus must visibly appear twenty-four blocks from its stop, before boarding is allowed");
			for (int tick = 0; tick < SovietBusEntity.APPROACH_TICKS / 2; tick++) bus.tick();
			test.assertTrue(bus.isApproaching() && Math.abs(bus.getAnimationOffset(1).length() - 6) < 0.001,
					"Approach must brake smoothly toward the stop, not instantly appear there");
			for (int tick = SovietBusEntity.APPROACH_TICKS / 2; tick < SovietBusEntity.APPROACH_TICKS; tick++) bus.tick();
			test.assertTrue(bus.isWaiting() && bus.isPickable() && bus.getAnimationOffset(1).equals(Vec3.ZERO),
					"After three seconds, the arriving bus must stop and open boarding");
			Vec3 stop = bus.position();
			for (int tick = 0; tick < SovietBusEntity.WAIT_TICKS - 1; tick++) bus.tick();
			test.assertTrue(bus.isWaiting() && bus.position().equals(stop), "Bus must remain stopped for the full wait");
			bus.tick();
			test.assertFalse(bus.isWaiting(), "Bus must start departing after exactly 400 waiting ticks");
			test.assertTrue(bus.getDepartureOffset(1).equals(Vec3.ZERO) && !bus.isPickable(),
					"Departure must start at rest and remove the invisible anchor's interaction box");
			// A wall in the old approach/departure lane must not terminate the visual ride.
			level.setBlockAndUpdate(BlockPos.containing(stop.add(0, 0, 4)), Blocks.STONE.defaultBlockState());
			player.setPos(player.position().add(160, 0, 0));
			for (int tick = 0; tick < 200; tick++) bus.tick();
			test.assertTrue(!bus.isRemoved() && Math.abs(bus.getDepartureOffset(1).length() - 80) < 0.001,
					"Halfway through a twenty-second quadratic ease-in, the model must be eighty blocks away");
			double midpoint = bus.getDepartureOffset(1).length();
			bus.tick();
			double midpointSpeed = bus.getDepartureOffset(1).length() - midpoint;
			for (int tick = 201; tick < SovietBusEntity.DEPART_TICKS - 1; tick++) bus.tick();
			test.assertTrue(!bus.isRemoved() && bus.getDepartureOffset(1).length() > 318
					&& bus.getDepartureOffset(1).length() - bus.getDepartureOffset(0).length() > midpointSpeed,
					"Bus must remain alive through its distant accelerating departure, even after owner teleport");
			bus.tick();
			test.assertTrue(Math.abs(bus.getDepartureOffset(1).length() - 320) < 0.001
					&& bus.position().equals(stop), "Animation must reach twenty chunks without moving its loaded network anchor");
			test.assertTrue(bus.isRemoved() && !bus.shouldBeSaved(), "Transient bus must leave without becoming a saved orphan");
		} finally {
			var active = SovietBusEvent.activeBus(player);
			if (active != null) active.discard();
			level.getEntities(ModEntityTypes.SOVIET_BUS, bus -> player.getUUID().equals(bus.getOwner())).forEach(SovietBusEntity::discard);
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void busBoardingIsOnlyAvailableDuringStopAndTeleportAwardsFare(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var bus = ModEntityTypes.SOVIET_BUS.create(level, EntitySpawnReason.EVENT);
		Vec3 start = Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(180, 280, 180)));
		level.getChunkAt(BlockPos.containing(start));
		bus.begin(player.getUUID(), start, new Vec3(0, 0, 1));
		player.setPos(start.add(0, 0, -4));
		try {
			test.assertFalse(bus.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
					"Approaching bus must not accept boarding");
			for (int tick = 0; tick < SovietBusEntity.APPROACH_TICKS; tick++) bus.tick();
			test.assertTrue(bus.isWaiting(), "Bus must open boarding only after arriving");
			test.assertTrue(bus.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
					&& bus.isRemoved() && !bus.isPickable(),
					"Boarding must start a trip and immediately consume the van instead of playing departure");
			test.assertFalse(bus.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
					"A vanished bus must not accept a second boarding");
			var advancement = level.getServer().getAdvancements().get(HbkMod.id("bus_fare"));
			test.assertTrue(advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone(),
					"Starting a search must not award an achievement before an actual ride");
			Vec3 destination = start.add(8, 0, 0);
			SovietBusEvent.completeTrip(player, level, destination);
			test.assertTrue(player.position().distanceToSqr(destination) < 0.00001
					&& player.getAdvancements().getOrStartProgress(advancement).isDone() && player.fallDistance == 0,
					"Successful trip must teleport and award the fare achievement");
			test.assertTrue(level.getServer().getCommands().getDispatcher().getRoot().getChild("bus")
					.getChild("player") != null, "Bus command must support the player argument");
		} finally {
			bus.discard();
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void boardingSmokeConsumesBusAndCompletedTeleportAllowsAnotherCall(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var observer = test.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(400, 280, 400)));
		try {
			player.setPos(origin);
			player.setYRot(0);
			observer.setPos(origin.add(3, 0, 3));
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"A bus must start before boarding");
			var bus = SovietBusEvent.activeBus(player);
			for (int tick = 0; tick < SovietBusEntity.APPROACH_TICKS; tick++) bus.tick();
			test.assertTrue(SovietBusEvent.sendBoardingSmoke(level, bus.position()) >= 2
					&& SovietBusEvent.BOARDING_SMOKE_RADIUS == 10 && SovietBusEvent.MIN_BOARDING_FOG_TICKS >= 10,
					"Ten-block boarding smoke must be broadcast to passenger and bystanders, not only one client");
			test.assertTrue(SovietBusEvent.board(player, bus) && bus.isRemoved()
					&& SovietBusEvent.activeBus(player) == null && SovietBusEvent.activeTrip(player) != null,
					"Boarding must remove both the model and its active-bus index while retaining only the pending trip");
			test.assertFalse(SovietBusEvent.board(observer, bus), "An already consumed bus must not accept another passenger");
			Vec3 destination = origin.add(2048, 0, 2048);
			level.getChunkAt(BlockPos.containing(destination));
			SovietBusEvent.completeTrip(player, level, destination);
			test.assertTrue(player.position().distanceToSqr(destination) < 0.00001
					&& SovietBusEvent.activeTrip(player) == null && !SovietBusEvent.isActive(player),
					"Distant arrival must release the route immediately, without waiting for the departure chunk to tick");
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"A player must be able to call a new bus immediately after arriving");
		} finally {
			var active = SovietBusEvent.activeBus(player);
			if (active != null) active.discard();
			level.getServer().getPlayerList().remove(player);
			level.getServer().getPlayerList().remove(observer);
		}
		test.succeed();
	}

	@GameTest
	public void unloadedOrUntickedBusExpiresByServerClock(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(440, 280, 440))));
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"A bus must start before checking its independent expiry");
			var bus = SovietBusEvent.activeBus(player);
			int startedAt = level.getServer().getTickCount();
			// Do not tick the entity at all: reproduce an old chunk no longer being simulated.
			SovietBusEvent.cleanupBuses(level.getServer(), startedAt + SovietBusEntity.MAX_EVENT_TICKS - 1);
			test.assertTrue(!bus.isRemoved() && SovietBusEvent.activeBus(player) == bus,
					"Cleanup must not truncate the normal approach, wait and unboarded departure");
			SovietBusEvent.cleanupBuses(level.getServer(), startedAt + SovietBusEntity.MAX_EVENT_TICKS);
			test.assertTrue(bus.isRemoved() && SovietBusEvent.activeBus(player) == null
					&& !SovietBusEvent.isActive(player), "Global expiry must discard even an entity that never ticked");
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"An expired bus in an old chunk must not block another call");
		} finally {
			var active = SovietBusEvent.activeBus(player);
			if (active != null) active.discard();
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void destinationsProbabilitySearchRingsAndSafeLandingAreExact(GameTestHelper test) {
		Set<String> structures = new HashSet<>();
		for (var destination : SovietBusEvent.Destination.values()) {
			if (destination.structure != null) structures.add(destination.structure);
		}
		test.assertTrue(structures.equals(Set.of("stalin_dacha", "gulag", "stalinka", "kirill_house", "kirill_house_hbk"))
				&& SovietBusEvent.Destination.values().length == 6 && SovietBusEvent.CHANCE_PER_SECOND == 0.1,
				"Bus must choose among exactly the six requested destinations and use ten percent per second");
		for (int ring = 1; ring <= 5; ring++) {
			Set<String> positions = new HashSet<>();
			for (int index = 0; index < ring * 8; index++) {
				int[] offset = BusDestinationSearch.ringOffset(ring, index);
				test.assertTrue(Math.max(Math.abs(offset[0]), Math.abs(offset[1])) == ring, "Each sample must lie on its ring");
				positions.add(offset[0] + ":" + offset[1]);
			}
			test.assertTrue(positions.size() == ring * 8, "Search rings must not duplicate or skip candidates");
		}
		var level = test.getLevel();
		BlockPos target = test.absolutePos(new BlockPos(80, 280, 80));
		for (BlockPos pos : BlockPos.betweenClosed(target.offset(-8, 0, -8), target.offset(8, 0, 8))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		level.setBlockAndUpdate(target, Blocks.MAGMA_BLOCK.defaultBlockState());
		Vec3 landing = BusDestinationSearch.safeLanding(level, target, false);
		test.assertTrue(landing != null && !level.getBlockState(BlockPos.containing(landing).below()).is(Blocks.MAGMA_BLOCK)
				&& level.getFluidState(BlockPos.containing(landing)).isEmpty(), "Landing must avoid damaging ground and liquids");
		var box = SovietBusEntity.footprint(Vec3.ZERO, 0);
		test.assertTrue(Math.abs(box.getZsize() - SovietBusEntity.LENGTH) < 0.00001
				&& Math.abs(box.getXsize() - SovietBusEntity.WIDTH) < 0.00001
				&& Math.abs(box.getXsize() - 2.9) < 0.00001 && Math.abs(box.getZsize() - 6.7) < 0.00001
				&& Math.abs(box.getYsize() - 3.2) < 0.00001 && SovietBusEntity.MODEL_SCALE == 1,
				"Bus must retain its original full-sized model and interaction box");
		test.assertTrue(SovietBusEntity.departureDistance(0) == 0
				&& SovietBusEntity.departureDistance(200) == 80 && SovietBusEntity.departureDistance(400) == 320
				&& SovietBusEntity.departureDistance(600) == 320 && SovietBusEntity.DEPART_TICKS == 400,
				"Departure easing must begin at rest, reach twenty chunks in twenty seconds, and clamp its endpoint");
		test.succeed();
	}

	@GameTest
	public void busSpawnsInsideSolidTerrainWithoutCheckingSpaceOrSurface(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos center = test.absolutePos(new BlockPos(240, 280, 240));
		// Underground solid rock where BOTH the stopping footprint and the route are obstructed.
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, 0, -5), center.offset(5, 8, 30))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setPos(Vec3.atBottomCenterOf(center.above()));
			player.setYRot(0);
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"Solid walls, a low ceiling and underground terrain must never block the event");
			var bus = SovietBusEvent.activeBus(player);
			test.assertTrue(bus.isApproaching() && bus.noPhysics && !level.noBlockCollision(bus, bus.getBoundingBox())
					&& Math.abs(bus.getY() - player.getY()) < 0.00001,
					"Bus must spawn at the player's elevation even when its entire footprint is inside stone");
			for (int tick = 0; tick < SovietBusEntity.APPROACH_TICKS; tick++) bus.tick();
			test.assertTrue(bus.isWaiting() && !bus.isRemoved(), "Bus must arrive through solid blocks");
		} finally {
			var active = SovietBusEvent.activeBus(player);
			if (active != null) active.discard();
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}

	@GameTest
	public void busSpawnsWithoutGroundEvenInWater(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos center = test.absolutePos(new BlockPos(280, 280, 280));
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -1), center.offset(3, 4, 9))) {
			level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
		}
		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setPos(Vec3.atBottomCenterOf(center));
			player.setYRot(0);
			test.assertTrue(SovietBusEvent.start(player) == SovietBusEvent.StartResult.STARTED,
					"Water and lack of solid ground must not prevent a manual bus event");
			var bus = SovietBusEvent.activeBus(player);
			for (int tick = 0; tick < SovietBusEntity.APPROACH_TICKS; tick++) bus.tick();
			test.assertTrue(bus.isWaiting() && !bus.isRemoved(), "Bus must arrive through water without being discarded");
		} finally {
			var active = SovietBusEvent.activeBus(player);
			if (active != null) active.discard();
			level.getServer().getPlayerList().remove(player);
		}
		test.succeed();
	}
}
