package dev.kirill.hbk;

import dev.kirill.hbk.entity.SquirrelEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.world.SquirrelEncounter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

public final class SquirrelGameTests {
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
		test.assertTrue(squirrel.isRemoved(), "The squirrel must disappear after a minute");

		var afterDeath = ModEntityTypes.SQUIRREL.create(test.getLevel(), EntitySpawnReason.EVENT);
		afterDeath.setEncounterOwner(player);
		afterDeath.snapTo(player.position());
		player.setHealth(0);
		afterDeath.tick();
		test.assertTrue(afterDeath.isRemoved(), "A dead owner's squirrel must be removed before respawn");
		test.succeed();
	}
}
