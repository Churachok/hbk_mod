package dev.kirill.hbk;

import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTestHelper;

public final class BossGameTests {
	@GameTest(maxTicks = 80)
	public void kirillAndMadLiberalPrioritizeEachOtherOverPlayers(GameTestHelper test) {
		for (int x = 0; x < 7; x++) {
			for (int z = 0; z < 7; z++) {
				test.setBlock(x, 1, z, net.minecraft.world.level.block.Blocks.STONE);
			}
		}
		var player = test.makeMockServerPlayerInLevel();
		BlockPos playerPos = test.absolutePos(new BlockPos(3, 2, 5));
		player.teleportTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
		var liberal = test.spawn(ModEntityTypes.MAD_LIBERAL, 2, 2, 2);
		var kirill = test.spawn(ModEntityTypes.KIRILL_DOOM, 4, 2, 2);
		liberal.setTarget(player);
		kirill.setTarget(player);

		test.succeedWhen(() -> {
			test.assertTrue(liberal.getTarget() == kirill,
					"Mad Liberal must target Kirill before a nearby player");
			test.assertTrue(kirill.getTarget() == liberal,
					"Kirill Doom must target Mad Liberal before a nearby player");
		});
	}

	@GameTest
	public void bossesHaveRequestedHealthAndArmorBehavior(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var liberal = test.spawn(ModEntityTypes.MAD_LIBERAL, 1, 2, 1);
		var kirill = test.spawn(ModEntityTypes.KIRILL_DOOM, 3, 2, 1);
		liberal.setNoAi(true);
		kirill.setNoAi(true);

		test.assertTrue(liberal.getMaxHealth() == 400.0f, "Mad Liberal must have 400 HP");
		test.assertTrue(kirill.getMaxHealth() == 800.0f, "Kirill Doom must have 800 HP");
		test.assertTrue(kirill.getMainHandItem().is(ModItems.ATTACKING_MEMBER),
				"Kirill Doom must wield Attacking Member");

		Arrow arrow = new Arrow(EntityTypes.ARROW, level);
		arrow.setOwner(player);
		float before = liberal.getHealth();
		liberal.hurtServer(level, level.damageSources().arrow(arrow, player), 50.0f);
		test.assertTrue(Math.abs((before - liberal.getHealth()) - 5.0f) < 0.01f,
				"Phase-one armor must absorb 90% of arrow damage");

		var phaseTwoLiberal = test.spawn(ModEntityTypes.MAD_LIBERAL, 2, 2, 3);
		phaseTwoLiberal.setNoAi(true);
		phaseTwoLiberal.setHealth(190.0f);
		before = phaseTwoLiberal.getHealth();
		phaseTwoLiberal.hurtServer(level, level.damageSources().arrow(arrow, player), 20.0f);
		test.assertTrue(Math.abs((before - phaseTwoLiberal.getHealth()) - 20.0f) < 0.01f,
				"Broken armor must stop reducing arrow damage");
		test.succeed();
	}

	@GameTest
	public void bossDropsAndKillAdvancementsAreExact(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();

		var liberal = test.spawn(ModEntityTypes.MAD_LIBERAL, 1, 2, 1);
		liberal.setNoAi(true);
		liberal.hurtServer(level, level.damageSources().playerAttack(player), 1000.0f);
		test.assertFalse(liberal.isAlive(), "Mad Liberal must die in the loot test");
		test.assertTrue(count(test, ModItems.ARMORED_MEMBER) == 1,
				"Mad Liberal must always drop one Armored Member");
		test.assertTrue(count(test, ModItems.URANIUM_CHESTPLATE) == 1,
				"Mad Liberal must always drop one uranium chestplate");
		test.assertTrue(done(test, player, "first_season_finished"),
				"Killing Mad Liberal must grant its advancement");
		clearItems(test);

		var kirill = test.spawn(ModEntityTypes.KIRILL_DOOM, 3, 2, 1);
		kirill.setNoAi(true);
		kirill.hurtServer(level, level.damageSources().playerAttack(player), 2000.0f);
		test.assertFalse(kirill.isAlive(), "Kirill Doom must die in the loot test");
		test.assertTrue(count(test, ModItems.FOUNDING_PENIS) == 4,
				"Kirill Doom must drop four Founding Penis transformations");
		test.assertTrue(count(test, ModItems.URANIUM_LEGGINGS) == 1,
				"Kirill Doom must drop uranium leggings");
		test.assertTrue(count(test, Items.NETHERITE_INGOT) == 3,
				"Kirill Doom must drop three netherite ingots");
		test.assertTrue(count(test, Items.ENCHANTED_GOLDEN_APPLE) == 64,
				"Kirill Doom must drop 64 enchanted golden apples");
		test.assertTrue(done(test, player, "kirill_end_of_world"),
				"Killing Kirill Doom must grant its advancement");
		test.succeed();
	}

	private static int count(GameTestHelper test, net.minecraft.world.item.Item item) {
		return test.getEntities(EntityTypes.ITEM).stream()
				.filter(entity -> entity.getItem().is(item))
				.mapToInt(entity -> entity.getItem().getCount())
				.sum();
	}

	private static void clearItems(GameTestHelper test) {
		for (var entity : test.getEntities(EntityTypes.ITEM)) {
			entity.discard();
		}
	}

	private static boolean done(GameTestHelper test, net.minecraft.server.level.ServerPlayer player, String name) {
		var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id(name));
		test.assertTrue(advancement != null, "Advancement must load: " + name);
		return player.getAdvancements().getOrStartProgress(advancement).isDone();
	}
}
