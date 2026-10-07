package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;

public final class SashaShieldGameTests {
	@GameTest
	public void shieldAbsorbsOnePlayerHitAndKnocksThemBack(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var sasha = test.spawn(ModEntityTypes.SASHA, 2, 2, 2);
		player.setPos(sasha.getX() + 1.0, sasha.getY(), sasha.getZ());
		sasha.setTarget(player);
		for (int tick = 0; tick < 120; tick++) sasha.tickShield(level);
		var attack = level.damageSources().playerAttack(player);
		test.assertTrue(sasha.breakShield(level, attack), "Sasha's raised shield must absorb a player hit");
		test.assertTrue(player.getDeltaMovement().horizontalDistanceSqr() > 0.5,
				"The broken shield must throw the player away");
		test.assertFalse(sasha.breakShield(level, attack), "The shield must break after one hit");
		sasha.discard();
		test.succeed();
	}
}
