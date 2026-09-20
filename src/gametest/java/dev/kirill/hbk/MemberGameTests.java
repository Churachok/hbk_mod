package dev.kirill.hbk;

import dev.kirill.hbk.item.ColossalMemberItem;
import dev.kirill.hbk.item.MemberDestruction;
import dev.kirill.hbk.item.MemberWeaponItem;
import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import dev.kirill.hbk.player.MechanicsPlayerData;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;

public final class MemberGameTests {
	@GameTest
	public void foodRecipesLoadWithExpectedResults(GameTestHelper test) {
		var level = test.getLevel();
		var manager = level.getServer().getRecipeManager();
		var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);
		var names = List.of("condensed_milk", "currant_tincture", "stew");
		var items = List.of(ModItems.CONDENSED_MILK, ModItems.CURRANT_TINCTURE, ModItems.STEW);
		for (int i = 0; i < names.size(); i++) {
			var name = names.get(i);
			var recipe = manager.byKey(ResourceKey.create(Registries.RECIPE, HbkMod.id(name)));
			test.assertTrue(recipe.isPresent(), "Food recipe must load: " + name);
			var expectedItem = items.get(i);
			int expectedCount = i == 0 ? 2 : 1;
			test.assertTrue(recipe.orElseThrow().value().display().stream()
					.flatMap(display -> display.result().resolveForStacks(context).stream())
					.anyMatch(stack -> stack.is(expectedItem) && stack.getCount() == expectedCount),
					"Food recipe must retain its expected result and count: " + name);
		}
		test.succeed();
	}

	private static AdvancementHolder advancement(GameTestHelper test, String name) {
		var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id(name));
		test.assertTrue(advancement != null, "Advancement must load: " + name);
		return advancement;
	}

	private static boolean done(ServerPlayer player, AdvancementHolder advancement) {
		return player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static void removePlayer(GameTestHelper test, ServerPlayer player) {
		test.getLevel().getServer().getPlayerList().remove(player);
	}

	@GameTest
	public void inventoryAchievementsAcceptEachOfSevenWeapons(GameTestHelper test) {
		var beginning = advancement(test, "hentai_beginnings");
		var vagina = advancement(test, "world_scale_vagina");
		var player = test.makeMockServerPlayerInLevel();
		try {
			for (var item : List.of(Blocks.DIRT.asItem(), ModItems.FEMALE_VAGINA)) {
				var stack = new ItemStack(item);
				player.getInventory().setItem(0, stack);
				CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
				test.assertFalse(done(player, beginning), "Neither dirt nor vagina counts as a penis");
			}
			test.assertTrue(done(player, vagina), "Vagina achievement must work without obtaining a penis first");
			for (var item : List.of(ModItems.ATTACKING_MEMBER, ModItems.ARMORED_MEMBER,
					ModItems.COLOSSAL_MEMBER, ModItems.JAW_MEMBER, ModItems.CARRIER_MEMBER,
					ModItems.BEASTLIKE_MEMBER, ModItems.HAMMER_FIGHTER_MEMBER)) {
				player.getAdvancements().revoke(beginning, "obtain_any_penis");
				var stack = new ItemStack(item);
				player.getInventory().setItem(0, stack);
				CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
				test.assertTrue(done(player, beginning), "A single weapon must suffice: " + item);
			}
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void visitsGrantAchievementsWithoutObtainingWeapons(GameTestHelper test) {
		var level = test.getLevel();
		var graveyard = advancement(test, "graveyard_visit");
		var city = advancement(test, "gornoslavyansk_visit");
		var beginning = advancement(test, "hentai_beginnings");
		var player = test.makeMockServerPlayerInLevel();
		var pos = test.absolutePos(new BlockPos(2, 160, 2));
		var chunk = level.getChunkAt(pos);
		var savedStarts = new HashMap<>(chunk.getAllStarts());
		var savedReferences = new HashMap<>(chunk.getAllReferences());
		try {
			player.setPos(Vec3.atCenterOf(pos));
			CriteriaTriggers.LOCATION.trigger(player);
			test.assertFalse(done(player, graveyard) || done(player, city), "Unrelated locations must not grant visits");
			var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.getOrThrow(ResourceKey.create(Registries.STRUCTURE, HbkMod.id("graveyard"))).value();
			var pool = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
					.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, HbkMod.id("graveyard"))).value();
			var element = pool.getRandomTemplate(level.getRandom());
			var bounds = element.getBoundingBox(level.getStructureManager(), pos, Rotation.NONE);
			var piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0,
					Rotation.NONE, bounds, LiquidSettings.APPLY_WATERLOGGING);
			chunk.setStartForStructure(structure, new StructureStart(structure, chunk.getPos(), 0,
					new PiecesContainer(List.of(piece))));
			chunk.addReferenceForStructure(structure, chunk.getPos().pack());
			CriteriaTriggers.LOCATION.trigger(player);
			test.assertTrue(done(player, graveyard), "Standing inside the graveyard must grant its achievement");
			test.setBiome(ResourceKey.create(Registries.BIOME, HbkMod.id("past_gornoslavyansk")));
			// Biome replacement covers the test's normal vertical volume, not the high-altitude fixture.
			player.setPos(test.absoluteVec(new Vec3(2.5, 2, 2.5)));
			CriteriaTriggers.LOCATION.trigger(player);
			test.assertTrue(done(player, city), "Entering past_gornoslavyansk must grant its achievement");
			test.assertFalse(done(player, beginning), "Visits must not require or grant a weapon achievement");
		} finally {
			chunk.setAllStarts(savedStarts);
			chunk.setAllReferences(savedReferences);
			test.setBiome(net.minecraft.world.level.biome.Biomes.PLAINS);
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void allShiftAbilitiesDestroyBlocksAndHaveCooldowns(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var center = test.absolutePos(new BlockPos(2, 160, 2));
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.setPos(Vec3.atCenterOf(center).add(0, 1, 0));
			player.setXRot(90);
			player.setShiftKeyDown(true);
			for (var item : List.of(ModItems.ARMORED_MEMBER, ModItems.JAW_MEMBER, ModItems.BEASTLIKE_MEMBER,
					ModItems.HAMMER_FIGHTER_MEMBER, ModItems.CARRIER_MEMBER)) {
				var stack = new ItemStack(item);
				player.setItemInHand(InteractionHand.MAIN_HAND, stack);
				level.setBlockAndUpdate(center, Blocks.SAND.defaultBlockState());
				float health = player.getHealth();
				test.assertTrue(item.use(level, player, InteractionHand.MAIN_HAND).consumesAction(), "Ability must activate: " + item);
				test.assertTrue(level.getBlockState(center).isAir(), "Ability must destroy sand: " + item);
				test.assertTrue(player.getHealth() == health, "Own explosion must not damage its wielder");
				test.assertTrue(player.getCooldowns().isOnCooldown(stack), "Ability must have a cooldown");
				level.setBlockAndUpdate(center, Blocks.SAND.defaultBlockState());
				test.assertTrue(item.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
						"Repeated activation must fail during cooldown");
				test.assertTrue(level.getBlockState(center).is(Blocks.SAND), "Cooldown must prevent another explosion");
			}
		} finally {
			level.removeBlock(center, false);
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void explosionsRespectAdventureAndBedrock(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var center = test.absolutePos(new BlockPos(2, 200, 2));
		try {
			player.setPos(Vec3.atCenterOf(center));
			player.setGameMode(GameType.ADVENTURE);
			level.setBlockAndUpdate(center, Blocks.SAND.defaultBlockState());
			MemberDestruction.explode(level, player, Vec3.atCenterOf(center), 6);
			test.assertTrue(level.getBlockState(center).is(Blocks.SAND), "Adventure players cannot destroy terrain");
			player.setGameMode(GameType.SURVIVAL);
			level.setBlockAndUpdate(center, Blocks.BEDROCK.defaultBlockState());
			MemberDestruction.explode(level, player, Vec3.atCenterOf(center), ColossalMemberItem.EXPLOSION_POWER);
			test.assertTrue(level.getBlockState(center).is(Blocks.BEDROCK), "Colossal must not break bedrock");
			player.setGameMode(GameType.SPECTATOR);
			level.setBlockAndUpdate(center, Blocks.SAND.defaultBlockState());
			MemberDestruction.explode(level, player, Vec3.atCenterOf(center), 6);
			test.assertTrue(level.getBlockState(center).is(Blocks.SAND), "Spectators cannot cause explosions");
		} finally {
			level.removeBlock(center, false);
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void attackingExplosiveProjectilePreservesNormalFire(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var center = test.absolutePos(new BlockPos(2, 240, 2));
		var area = new AABB(center).inflate(20);
		try {
			player.setPos(Vec3.atCenterOf(center).add(0, 3, 0));
			player.setXRot(90);
			var stack = new ItemStack(ModItems.ATTACKING_MEMBER);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			for (int mode = 0; mode < 2; mode++) {
				level.setBlockAndUpdate(center, Blocks.SAND.defaultBlockState());
				if (mode == 0) {
					ModItems.ATTACKING_MEMBER.use(level, player, InteractionHand.MAIN_HAND);
				} else {
					((MemberWeaponItem) ModItems.ATTACKING_MEMBER).fire(player);
				}
				var bullets = level.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET, area, entity -> true);
				test.assertTrue(bullets.size() == 1, "Each activation must spawn one projectile");
				var bullet = bullets.getFirst();
				for (int tick = 0; tick < 10 && !bullet.isRemoved(); tick++) {
					bullet.tick();
				}
				test.assertTrue(bullet.isRemoved(), "Projectile must hit the target");
				test.assertTrue(mode == 0 ? level.getBlockState(center).isAir() : level.getBlockState(center).is(Blocks.SAND),
						"Only the RMB explosive shot should destroy terrain");
				player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
			}
		} finally {
			level.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET, area, entity -> true).forEach(entity -> entity.discard());
			level.removeBlock(center, false);
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void colossalCataclysmIsMuchStrongerThanTnt(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var center = test.absolutePos(new BlockPos(2, 300, 2));
		var area = new AABB(center).inflate(64);
		try {
			player.setPos(Vec3.atCenterOf(center).add(0, 3, 0));
			player.setXRot(90);
			player.setGameMode(GameType.SURVIVAL);
			var stack = new ItemStack(ModItems.COLOSSAL_MEMBER);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			int ordinaryDestruction = 0;
			int colossalDestruction = 0;
			for (int mode = 0; mode < 2; mode++) {
				for (int x = -12; x <= 12; x++) {
					for (int z = -12; z <= 12; z++) {
						level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.SAND.defaultBlockState());
					}
				}
				float health = player.getHealth();
				if (mode == 0) {
					MemberDestruction.explode(level, player, MemberDestruction.aimedPosition(level, player, 48), 4);
				} else {
					test.assertTrue(ModItems.COLOSSAL_MEMBER.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
							"Colossal RMB must activate");
					test.assertTrue(ModItems.COLOSSAL_MEMBER.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
							"Cataclysm must have a cooldown");
				}
				test.assertTrue(player.getHealth() == health, "Even the cataclysm must not directly damage its wielder");
				for (int x = -12; x <= 12; x++) {
					for (int z = -12; z <= 12; z++) {
						if (level.getBlockState(center.offset(x, 0, z)).isAir()) {
							if (mode == 0) ordinaryDestruction++; else colossalDestruction++;
						}
					}
				}
			}
			test.assertTrue(ordinaryDestruction > 0 && colossalDestruction > ordinaryDestruction * 3,
					"Cataclysm must destroy over 3x as many blocks as power-4 TNT: " + colossalDestruction + " vs " + ordinaryDestruction);
			((MemberWeaponItem) ModItems.COLOSSAL_MEMBER).fire(player);
			((MemberWeaponItem) ModItems.COLOSSAL_MEMBER).fire(player);
			test.assertTrue(level.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET, area, entity -> true).size() == 100,
					"Colossal LMB must fire exactly 100 projectiles; a second burst is blocked by cooldown");
		} finally {
			level.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET, area, entity -> true).forEach(entity -> entity.discard());
			for (int x = -12; x <= 12; x++) {
				for (int z = -12; z <= 12; z++) level.removeBlock(center.offset(x, 0, z), false);
			}
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void vaginaCreatesSixCratersAndRetainsRadialLaunch(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockServerPlayerInLevel();
		var center = test.absolutePos(new BlockPos(2, 350, 2));
		var craters = new java.util.ArrayList<BlockPos>();
		var target = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
		test.assertTrue(target != null, "Launch test requires a target");
		try {
			player.setPos(Vec3.atCenterOf(center));
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FEMALE_VAGINA));
			for (int i = 0; i < 6; i++) {
				double angle = Math.PI * 2 * i / 6;
				var pos = BlockPos.containing(player.position().add(Math.cos(angle) * 8, 0, Math.sin(angle) * 8));
				craters.add(pos);
				level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
			}
			target.setPos(player.position().add(0, 0, 2));
			target.setNoAi(true);
			level.addFreshEntity(target);
			float targetHealth = target.getHealth();
			float playerHealth = player.getHealth();
			test.assertTrue(ModItems.FEMALE_VAGINA.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
					"Vagina RMB must activate");
			for (var pos : craters) test.assertTrue(level.getBlockState(pos).isAir(), "All six blast centers must become craters");
			test.assertTrue(target.getHealth() < targetHealth && target.getDeltaMovement().y == 0.9,
					"The radial ability must damage and launch nearby targets");
			test.assertTrue(player.getHealth() == playerHealth, "Ring explosions must not directly hurt their wielder");
			test.assertTrue(ModItems.FEMALE_VAGINA.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
					"Ring ability must have a cooldown");
		} finally {
			target.discard();
			for (var pos : craters) level.removeBlock(pos, false);
			removePlayer(test, player);
		}
		test.succeed();
	}

	@GameTest
	public void progenitorRecipeAndTransformationLastOneMinute(GameTestHelper test) {
		var level = test.getLevel();
		var input = CraftingInput.of(2, 1, List.of(
				new ItemStack(ModItems.ATTACKING_MEMBER), new ItemStack(ModItems.FEMALE_VAGINA)));
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		test.assertTrue(recipe.id().identifier().equals(HbkMod.id("founding_penis")),
				"The attacking member and female vagina must craft the transformation item");
		test.assertTrue(recipe.value().assemble(input).is(ModItems.FOUNDING_PENIS),
				"The recipe must yield the transformation item");

		var player = test.makeMockServerPlayerInLevel();
		try {
			player.setPos(Vec3.atCenterOf(test.absolutePos(new BlockPos(2, 250, 2))));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FOUNDING_PENIS));
			test.assertTrue(ModItems.FOUNDING_PENIS.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
					"Right click must start the transformation");
			MechanicsPlayerData data = (MechanicsPlayerData) player;
			test.assertTrue(data.hbk$getProgenitorTicks() == 1200 && ProgenitorTransformation.isActive(player),
					"The giant form must start with 60 seconds remaining");
			test.assertTrue(done(player, advancement(test, "founding_penis")),
					"Transforming must award the Founding Penis advancement");
			test.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
					"The creative mock player must retain the transformation item");
			test.assertTrue(ProgenitorTransformation.fireProjectile(player), "Left click must launch the giant shot");
			test.assertTrue(level.getEntities(ModEntityTypes.FOUNDING_PENIS_PROJECTILE,
					AABB.ofSize(player.getEyePosition(), 32, 32, 32), entity -> true).size() == 1,
					"The giant shot must spawn as a projectile");
			player.setXRot(90);
			test.assertTrue(ProgenitorTransformation.blast(player), "Right click must trigger the blast");
			long blastReadyTick = data.hbk$getFoundingBlastReadyTick();
			test.assertTrue(blastReadyTick == level.getServer().overworld().getGameTime() + 1200,
					"The blast must start a 60 second cooldown");
			test.assertTrue(!ProgenitorTransformation.blast(player) && data.hbk$getFoundingBlastReadyTick() == blastReadyTick,
					"Right click must not trigger a second blast during this transformation");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FOUNDING_PENIS));
			test.assertTrue(ModItems.FOUNDING_PENIS.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
					"The player cannot transform twice at once");
			ProgenitorTransformation.tick(player);
			test.assertTrue(data.hbk$getProgenitorTicks() == 1199,
					"Server ticks must count down the effect");
			data.hbk$setProgenitorTicks(1);
			ProgenitorTransformation.tick(player);
			test.assertTrue(data.hbk$getProgenitorTicks() == 0 && !ProgenitorTransformation.isActive(player),
					"The player must return to normal after the final tick");
		} finally {
			removePlayer(test, player);
		}
		test.succeed();
	}
}
