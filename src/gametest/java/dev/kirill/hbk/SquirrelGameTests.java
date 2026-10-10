package dev.kirill.hbk;

import dev.kirill.hbk.entity.SquirrelEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.world.SquirrelEncounter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class SquirrelGameTests {
	@GameTest
	public void squirrelsCannotBeHitOrKilled(GameTestHelper test) {
		var level = test.getLevel();
		var owner = test.makeMockServerPlayerInLevel();
		var squirrel = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		try {
			squirrel.setEncounterOwner(owner);
			squirrel.snapTo(owner.position());
			float health = squirrel.getHealth();
			var sources = level.damageSources();
			for (var source : new net.minecraft.world.damagesource.DamageSource[]{
					sources.playerAttack(owner), sources.generic(), sources.onFire(), sources.lava(),
					sources.fall(), sources.drown(), sources.fellOutOfWorld(), sources.genericKill()}) {
				test.assertTrue(squirrel.isInvulnerableTo(level, source)
						&& !squirrel.hurtServer(level, source, Float.MAX_VALUE),
						"All direct and environmental damage must be rejected");
			}
			owner.attack(squirrel);
			squirrel.kill(level);
			test.assertTrue(squirrel.isAlive() && squirrel.getHealth() == health && squirrel.hurtTime == 0,
					"Player attacks and kill damage must not cause death or the hurt animation");
			test.assertTrue(!squirrel.isPickable() && !squirrel.isAttackable()
					&& squirrel.skipAttackInteraction(owner) && !squirrel.isPushable(),
					"The squirrel must not be a crosshair/projectile target or accept attack interactions");
		} finally {
			squirrel.discard();
			level.getServer().getPlayerList().remove(owner);
		}
		test.succeed();
	}

	@GameTest
	public void squirrelStandsOnlyWhileResting(GameTestHelper test) {
		var level = test.getLevel();
		var owner = test.makeMockServerPlayerInLevel();
		var squirrel = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		try {
			squirrel.setEncounterOwner(owner);
			squirrel.snapTo(owner.position());
			squirrel.setNoAi(true);
			squirrel.setNoGravity(true);
			for (int tick = 0; tick < 12; tick++) {
				squirrel.setOnGround(true);
				squirrel.tick();
			}
			test.assertTrue(squirrel.getUprightAmount(1) == 1,
					"An idle squirrel must smoothly become fully upright");
			for (int tick = 0; tick < 4; tick++) {
				squirrel.setDeltaMovement(new Vec3(0.12, 0, 0));
				squirrel.tick();
			}
			test.assertTrue(squirrel.getUprightAmount(1) == 0,
					"A running squirrel must lower its front paws back to the ground");
			squirrel.setDeltaMovement(Vec3.ZERO);
			squirrel.setOnGround(false);
			squirrel.tick();
			test.assertTrue(squirrel.getUprightAmount(1) == 0,
					"An airborne squirrel must not stand upright");
		} finally {
			squirrel.discard();
			level.getServer().getPlayerList().remove(owner);
		}
		test.succeed();
	}

	@GameTest
	public void squirrelFollowsItsOwnerButKeepsSixBlocksOfSpace(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos origin = test.absolutePos(new BlockPos(350, 280, 600));
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -1, -5), origin.offset(14, -1, 5))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		var owner = test.makeMockServerPlayerInLevel();
		var bystander = test.makeMockServerPlayerInLevel();
		var squirrel = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		try {
			owner.snapTo(Vec3.atBottomCenterOf(origin));
			bystander.snapTo(owner.position().add(9, 0, 0));
			squirrel.setEncounterOwner(owner);
			squirrel.snapTo(owner.position().add(10, 0, 0));
			squirrel.setOnGround(true);
			level.addFreshEntity(squirrel);
			for (int tick = 0; tick < 100; tick++) squirrel.tick();
			test.assertTrue(squirrel.getEncounterOwner() == owner
					&& squirrel.distanceToSqr(owner) < 49 && squirrel.distanceToSqr(owner) > 20,
					"The squirrel must approach its owner, not a closer bystander, and stop around six blocks away");
			owner.snapTo(owner.position().add(-6, 0, 0));
			for (int tick = 0; tick < 100; tick++) squirrel.tick();
			test.assertTrue(squirrel.distanceToSqr(owner) < 49,
					"Following must restart when the owner moves beyond seven blocks");
		} finally {
			squirrel.discard();
			level.getServer().getPlayerList().remove(owner);
			level.getServer().getPlayerList().remove(bystander);
		}
		test.succeed();
	}

	@GameTest
	public void encountersAreIndependentAndPrivate(GameTestHelper test) {
		for (int x = 0; x <= 8; x++) {
			for (int z = 0; z <= 8; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
			}
		}
		var first = test.makeMockServerPlayerInLevel();
		var second = test.makeMockServerPlayerInLevel();
		var center = test.absolutePos(new BlockPos(4, 2, 4));
		first.snapTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
		second.snapTo(first.position());
		test.assertTrue(SquirrelEncounter.start(first) == SquirrelEncounter.StartResult.STARTED,
				"A personal squirrel must spawn on nearby clear ground");
		test.assertTrue(SquirrelEncounter.start(first) == SquirrelEncounter.StartResult.ALREADY_ACTIVE,
				"One player must not receive duplicate squirrels");
		test.assertTrue(SquirrelEncounter.start(second) == SquirrelEncounter.StartResult.STARTED,
				"Another player must have an independent encounter, even at the same location");
		var squirrels = test.getLevel().getEntitiesOfClass(SquirrelEntity.class, first.getBoundingBox().inflate(10));
		var firstSquirrel = squirrels.stream().filter(s -> s.broadcastToPlayer(first)).findFirst().orElseThrow();
		var secondSquirrel = squirrels.stream().filter(s -> s.broadcastToPlayer(second)).findFirst().orElseThrow();
		test.assertTrue(firstSquirrel != secondSquirrel && !firstSquirrel.broadcastToPlayer(second)
				&& !secondSquirrel.broadcastToPlayer(first),
				"The entity tracking filter must send each squirrel exclusively to its owner");
		test.assertTrue(test.getLevel().noCollision(firstSquirrel) && firstSquirrel.isSilent()
				&& !firstSquirrel.isPushable() && !firstSquirrel.shouldBeSaved(),
				"Personal squirrels must have clear spawn space, no public sounds/pushing, and no world persistence");
		firstSquirrel.discard();
		secondSquirrel.discard();
		test.succeed();
	}

	@GameTest
	public void squirrelsExpireAndCleanUpAfterOwnerDeath(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		var squirrel = ModEntityTypes.SQUIRREL.create(test.getLevel(), EntitySpawnReason.EVENT);
		test.assertTrue(squirrel != null, "Squirrel entity must be constructible");
		squirrel.setEncounterOwner(player);
		squirrel.snapTo(player.position());
		squirrel.tickCount = SquirrelEntity.LIFETIME_TICKS;
		squirrel.tick();
		test.assertTrue(!squirrel.isRemoved() && squirrel.isSayingGoodbye(),
				"After a minute the squirrel must begin its farewell, not disappear immediately");
		squirrel.tickCount = SquirrelEntity.LIFETIME_TICKS + SquirrelEntity.MAX_GOODBYE_APPROACH_TICKS
				+ SquirrelEntity.GOODBYE_DURATION_TICKS;
		squirrel.tick();
		test.assertTrue(squirrel.isRemoved(), "A blocked farewell must not leave a permanent squirrel");

		var afterDeath = ModEntityTypes.SQUIRREL.create(test.getLevel(), EntitySpawnReason.EVENT);
		afterDeath.setEncounterOwner(player);
		afterDeath.snapTo(player.position());
		player.setHealth(0);
		afterDeath.tick();
		test.assertTrue(afterDeath.isRemoved(), "A dead owner's squirrel must be removed before respawn");
		test.succeed();
	}

	@GameTest
	public void farewellApproachesFrontExtendsPawGivesExactlyOneBeerAndLeaves(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos floor = test.absolutePos(new BlockPos(420, 280, 600));
		for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-6, -1, -6), floor.offset(6, -1, 9))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		var owner = test.makeMockServerPlayerInLevel();
		var squirrel = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		try {
			owner.snapTo(Vec3.atBottomCenterOf(floor));
			owner.setYRot(0);
			squirrel.setEncounterOwner(owner);
			squirrel.snapTo(owner.position().add(0, 0, 7));
			squirrel.setOnGround(true);
			squirrel.tickCount = SquirrelEntity.LIFETIME_TICKS;
			level.addFreshEntity(squirrel);
			for (int tick = 0; tick < 220 && !squirrel.isGivingBeer(); tick++) tickSquirrel(squirrel);
			test.assertTrue(squirrel.isGivingBeer() && squirrel.getZ() > owner.getZ() + 0.6
					&& squirrel.distanceToSqr(owner) < 2.8 * 2.8,
					"Farewell approach: owner=" + owner.position() + ", squirrel=" + squirrel.position()
							+ ", ground=" + squirrel.onGround() + ", giving=" + squirrel.isGivingBeer()
							+ ", removed=" + squirrel.isRemoved() + ", path=" + squirrel.getNavigation().getTargetPos());
			test.assertTrue(squirrel.getMainHandItem().is(ModItems.BEER_BOTTLE),
					"The actual bottle item must be visible in the paw before it is given");
			for (int tick = 0; tick < SquirrelEntity.GIFT_HANDOVER_TICKS - 1; tick++) tickSquirrel(squirrel);
			test.assertTrue(squirrel.getGiveProgress(1) == 1 && squirrel.getUprightAmount(1) == 1
					&& countBeer(owner) == 0, "Stand upright and extend the paw before transferring the gift");
			tickSquirrel(squirrel);
			test.assertTrue(countBeer(owner) == 1 && squirrel.getMainHandItem().isEmpty() && !squirrel.isRemoved(),
					"Handover must move exactly one beer from the paw to its owner's inventory");
			for (int tick = 0; tick < SquirrelEntity.GOODBYE_DURATION_TICKS + 20; tick++) tickSquirrel(squirrel);
			test.assertTrue(squirrel.isRemoved() && countBeer(owner) == 1,
					"The squirrel must leave after the gesture, without duplicating the beer on later ticks");
		} finally {
			squirrel.discard();
			level.getServer().getPlayerList().remove(owner);
		}
		test.succeed();
	}

	@GameTest
	public void fullInventoryReceivesOwnerLockedBeerAndOwnerDeathCancelsFarewell(GameTestHelper test) {
		var level = test.getLevel();
		// Item-entity queries only see accessible sections; stay in the test's loaded chunks.
		BlockPos floor = test.absolutePos(new BlockPos(2, 280, 2));
		for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-3, -1, -3), floor.offset(3, -1, 5))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		var owner = test.makeMockServerPlayerInLevel();
		var bystander = test.makeMockServerPlayerInLevel();
		var squirrel = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		var canceled = ModEntityTypes.SQUIRREL.create(level, EntitySpawnReason.EVENT);
		try {
			owner.snapTo(Vec3.atBottomCenterOf(floor));
			owner.setYRot(0);
			for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
				owner.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
			}
			test.assertTrue(owner.getInventory().getFreeSlot() < 0,
					"The overflow scenario needs a genuinely full inventory");
			squirrel.setEncounterOwner(owner);
			squirrel.snapTo(owner.position().add(0, 0, 1.6));
			squirrel.setOnGround(true);
			squirrel.tickCount = SquirrelEntity.LIFETIME_TICKS;
			level.addFreshEntity(squirrel);
			for (int tick = 0; tick < 100 && !squirrel.isRemoved(); tick++) tickSquirrel(squirrel);
			var gifts = level.getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(3),
					item -> item.getItem().is(ModItems.BEER_BOTTLE));
			test.assertTrue(gifts.size() == 1 && gifts.getFirst().getItem().getCount() == 1,
					"Full inventory gift: drops=" + gifts.size() + ", squirrel=" + squirrel.position()
							+ ", giving=" + squirrel.isGivingBeer() + ", progress=" + squirrel.getGiveProgress(1)
							+ ", removed=" + squirrel.isRemoved() + ", held=" + squirrel.getMainHandItem());
			bystander.snapTo(gifts.getFirst().position());
			gifts.getFirst().playerTouch(bystander);
			test.assertTrue(!gifts.getFirst().isRemoved() && countBeer(bystander) == 0,
					"A bystander must not be able to pick up another player's gift");
			gifts.forEach(ItemEntity::discard);
			canceled.setEncounterOwner(owner);
			canceled.snapTo(owner.position().add(0, 0, 1.6));
			canceled.setOnGround(true);
			canceled.tickCount = SquirrelEntity.LIFETIME_TICKS;
			tickSquirrel(canceled);
			owner.setHealth(0);
			tickSquirrel(canceled);
			test.assertTrue(canceled.isRemoved()
					&& level.getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(3),
							item -> item.getItem().is(ModItems.BEER_BOTTLE)).isEmpty(),
					"Death during the gesture must cancel the event without giving or dropping beer");
		} finally {
			squirrel.discard();
			canceled.discard();
			level.getServer().getPlayerList().remove(owner);
			level.getServer().getPlayerList().remove(bystander);
		}
		test.succeed();
	}

	private static void tickSquirrel(SquirrelEntity squirrel) {
		// ServerLevel normally advances entity age before tick(), including the goal scheduler's parity.
		squirrel.tickCount++;
		squirrel.tick();
	}

	private static int countBeer(net.minecraft.server.level.ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ModItems.BEER_BOTTLE)) count += stack.getCount();
		}
		return count;
	}
}
