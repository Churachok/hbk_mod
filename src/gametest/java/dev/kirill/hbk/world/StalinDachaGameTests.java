package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class StalinDachaGameTests {
	@GameTest
	public void templateLoadsWithEightChests(GameTestHelper test) {
		var template = test.getLevel().getStructureManager().get(HbkMod.id("stalin_dacha"));
		test.assertTrue(template.isPresent(), "The dacha NBT template must load");
		Vec3i size = template.orElseThrow().getSize();
		test.assertTrue(size.getX() == 84 && size.getY() == 30 && size.getZ() == 79,
				"The supplied dacha template must have its original dimensions");
		var chests = template.orElseThrow().filterBlocks(BlockPos.ZERO,
				new StructurePlaceSettings(), Blocks.CHEST);
		test.assertTrue(chests.size() == 8, "The supplied dacha must have eight regular chests");
		test.assertTrue(test.getLevel().getServer().getAdvancements()
				.get(HbkMod.id("stalin_dacha_visit")) != null,
				"The National Secret advancement must load");
		test.succeed();
	}

	@GameTest
	public void dachaResidentsSpawnOnce(GameTestHelper test) {
		for (int x = 1; x <= 8; x++) {
			for (int z = 1; z <= 8; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
			}
		}
		BlockPos first = test.absolutePos(new BlockPos(1, 1, 1));
		BlockPos last = test.absolutePos(new BlockPos(8, 5, 8));
		BoundingBox box = new BoundingBox(first.getX(), first.getY(), first.getZ(),
				last.getX(), last.getY(), last.getZ());
		var data = ModWorldData.get(test.getLevel());
		String key = "test_dacha@" + first.asLong();
		ModStructurePopulator.populateStalinDachaResidents(test.getLevel(), box, data, key);
		test.assertTrue(test.getEntities(ModEntityTypes.NKVD).size() == 4,
				"The dacha must create four NKVD guards");
		test.assertTrue(test.getEntities(ModEntityTypes.GRISHA).size() == 1,
				"The dacha must create Grisha");
		ModStructurePopulator.populateStalinDachaResidents(test.getLevel(), box, data, key);
		test.assertTrue(test.getEntities(ModEntityTypes.NKVD).size() == 4
				&& test.getEntities(ModEntityTypes.GRISHA).size() == 1,
				"Successful dacha spawns must not duplicate on later visits");
		test.succeed();
	}

	@GameTest
	public void everyDachaHasOneEggAndOnePipe(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos first = test.absolutePos(new BlockPos(2, 2, 2));
		BlockPos last = test.absolutePos(new BlockPos(5, 2, 5));
		for (int x : new int[] {2, 5}) {
			for (int z : new int[] {2, 5}) {
				test.setBlock(x, 2, z, Blocks.CHEST);
			}
		}
		BoundingBox box = new BoundingBox(first.getX(), first.getY(), first.getZ(),
				last.getX(), last.getY(), last.getZ());
		test.assertTrue(ModStructurePopulator.fillStalinDachaChests(level, box), "Dacha chests must fill");
		int eggs = 0;
		int pipes = 0;
		for (int x : new int[] {2, 5}) {
			for (int z : new int[] {2, 5}) {
				ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(test.absolutePos(new BlockPos(x, 2, z)));
				for (int slot = 0; slot < chest.getContainerSize(); slot++) {
					eggs += chest.getItem(slot).is(ModItems.STALIN_SPAWN_EGG) ? chest.getItem(slot).getCount() : 0;
					pipes += chest.getItem(slot).is(ModItems.STALIN_PIPE) ? chest.getItem(slot).getCount() : 0;
				}
			}
		}
		test.assertTrue(eggs == 1 && pipes == 1,
				"All room chests together must have exactly one Stalin egg and one pipe");
		test.succeed();
	}

	@GameTest
	public void nkvdNeverAttacksGrishaAndPipeGuardsOnlyAttackMonsters(GameTestHelper test) {
		var owner = test.makeMockServerPlayerInLevel();
		var guard = test.spawn(ModEntityTypes.NKVD, 2, 2, 2);
		var grisha = test.spawn(ModEntityTypes.GRISHA, 3, 2, 2);
		var zombie = test.spawn(EntityTypes.ZOMBIE, 4, 2, 2);
		var cow = test.spawn(EntityTypes.COW, 5, 2, 2);
		var hostileNkvd = test.spawn(ModEntityTypes.NKVD, 6, 2, 2);
		var otherGuard = test.spawn(ModEntityTypes.NKVD, 7, 2, 2);
		otherGuard.setGuardOwner(owner);
		var sasha = test.spawn(ModEntityTypes.SASHA, 8, 2, 2);
		test.assertFalse(guard.canAttack(grisha), "Ordinary NKVD must spare Grisha");
		guard.setGuardOwner(owner);
		test.assertTrue(guard.canAttack(zombie), "Pipe guards must attack hostile mobs");
		test.assertTrue(guard.canAttack(hostileNkvd), "Pipe guards must attack hostile NKVD");
		test.assertTrue(guard.canAttack(sasha), "Pipe guards must attack hostile mod NPCs");
		test.assertFalse(guard.canAttack(otherGuard), "Pipe guards must spare other pipe guards");
		test.assertFalse(guard.canAttack(cow), "Pipe guards must spare passive mobs");
		test.assertFalse(guard.canAttack(owner), "Pipe guards must spare their player");
		test.assertFalse(guard.canAttack(grisha), "Pipe guards must spare Grisha");
		float cowHealth = cow.getHealth();
		cow.hurtServer(test.getLevel(), test.getLevel().damageSources().mobAttack(guard), 5.0f);
		test.assertTrue(cow.getHealth() == cowHealth,
				"A guard's stray hit must not damage passive bystanders");
		float hostileHealth = hostileNkvd.getHealth();
		hostileNkvd.hurtServer(test.getLevel(), test.getLevel().damageSources().mobAttack(guard), 5.0f);
		test.assertTrue(hostileNkvd.getHealth() < hostileHealth,
				"Pipe guards must be able to damage hostile NKVD");
		float guardHealth = otherGuard.getHealth();
		otherGuard.hurtServer(test.getLevel(), test.getLevel().damageSources().mobAttack(guard), 5.0f);
		test.assertTrue(otherGuard.getHealth() == guardHealth,
				"A guard's stray hit must not damage another pipe guard");
		test.succeed();
	}

	@GameTest(maxTicks = 80)
	public void pipeGuardTargetsHostileNkvd(GameTestHelper test) {
		var owner = test.makeMockServerPlayerInLevel();
		var guard = test.spawn(ModEntityTypes.NKVD, 2, 2, 2);
		var hostileNkvd = test.spawn(ModEntityTypes.NKVD, 4, 2, 2);
		guard.setGuardOwner(owner);
		test.succeedWhen(() -> test.assertTrue(guard.getTarget() == hostileNkvd,
				"Pipe guards must acquire a hostile NKVD target"));
	}

	@GameTest
	public void pipeSummonsThreeGuards(GameTestHelper test) {
		for (int x = 1; x <= 8; x++) {
			for (int z = 1; z <= 8; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
			}
		}
		var player = test.makeMockServerPlayerInLevel();
		BlockPos center = test.absolutePos(new BlockPos(4, 2, 4));
		player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STALIN_PIPE));
		ModItems.STALIN_PIPE.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
		long guards = test.getEntities(ModEntityTypes.NKVD).stream().filter(entity -> entity.isPipeGuard()).count();
		test.assertTrue(guards == 3, "One use of the pipe must summon three friendly NKVD");
		test.succeed();
	}
}
