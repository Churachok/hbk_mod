package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.component.WrittenBookContent;

public final class SpawnAndGraveyardGameTests {
	@GameTest
	public void npcSpawnsMatchPigWeight(GameTestHelper test) {
		var spawns = test.getLevel().getBiome(test.absolutePos(new BlockPos(2, 2, 2)))
				.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
		assertPigLikeSpawn(test, spawns, ModEntityTypes.ANTON);
		assertPigLikeSpawn(test, spawns, ModEntityTypes.GRISHA);
		test.succeed();
	}

	private static void assertPigLikeSpawn(GameTestHelper test,
			java.util.List<net.minecraft.util.random.Weighted<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData>> spawns,
			EntityType<?> type) {
		var spawn = spawns.stream().filter(entry -> entry.value().type() == type).findFirst();
		test.assertTrue(spawn.isPresent(), "NPC must be in the expected biome spawn table: " + type);
		test.assertTrue(spawn.orElseThrow().weight() == 10 && spawn.orElseThrow().value().minCount() == 4
				&& spawn.orElseThrow().value().maxCount() == 4, "NPC must match vanilla pig weight and group size: " + type);
	}

	@GameTest(maxTicks = 60)
	public void spermBlocksCanBeMinedAndHurtEntities(GameTestHelper test) {
		var level = test.getLevel();
		for (int x : new int[] {2, 4}) {
			var block = x == 2 ? ModBlocks.SPERM : ModBlocks.BLOODY_SPERM;
			test.setBlock(x, 2, 2, block);
			BlockPos pos = test.absolutePos(new BlockPos(x, 2, 2));
			test.assertTrue(block.defaultBlockState().getDestroySpeed(level, pos) >= 0.0f
					&& block.defaultBlockState().getDestroySpeed(level, pos) <= 1.0f,
					"Sperm blocks must have ordinary mineable hardness");
		}
		var cow = test.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 2);
		var sheep = test.spawnWithNoFreeWill(EntityTypes.SHEEP, 4, 2, 2);
		test.runAfterDelay(35, () -> {
			test.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Sperm must hurt entities standing inside it");
			test.assertTrue(sheep.getHealth() < sheep.getMaxHealth(), "Bloody sperm must still hurt entities");
			test.succeed();
		});
	}

	@GameTest
	public void spermDropsOneToThreeShperma(GameTestHelper test) {
		BlockPos pos = new BlockPos(2, 2, 2);
		test.setBlock(pos, ModBlocks.SPERM);
		test.getLevel().destroyBlock(test.absolutePos(pos), true);
		int shperma = test.getEntities(EntityTypes.ITEM).stream()
				.filter(entity -> entity.getItem().is(ModItems.SHPERMA))
				.mapToInt(entity -> entity.getItem().getCount()).sum();
		test.assertTrue(shperma >= 1 && shperma <= 3, "Mining sperm must drop 1-3 Shperma");
		test.succeed();
	}

	@GameTest
	public void gulagBarrelsKeepExistingItemsAndGainSupplies(GameTestHelper test) {
		BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
		test.setBlock(2, 2, 2, Blocks.BARREL);
		BarrelBlockEntity barrel = (BarrelBlockEntity) test.getLevel().getBlockEntity(pos);
		barrel.setItem(0, new ItemStack(Items.DIAMOND, 2));
		ModStructurePopulator.fillGulagBarrels(test.getLevel(), new BoundingBox(
				pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
		test.assertTrue(barrel.getItem(0).is(Items.DIAMOND) && barrel.getItem(0).getCount() == 2,
				"Gulag supplies must preserve existing barrel loot");
		boolean hasSupplies = false;
		for (int slot = 1; slot < barrel.getContainerSize(); slot++) {
			var stack = barrel.getItem(slot);
			hasSupplies |= stack.is(ModItems.RATION) || stack.is(ModItems.STEW) || stack.is(ModItems.CONDENSED_MILK);
		}
		test.assertTrue(hasSupplies, "Gulag barrel must contain food supplies");
		test.succeed();
	}

	@GameTest
	public void gulagHasExactlyOneCompassWithoutReplacingLoot(GameTestHelper test) {
		BlockPos first = test.absolutePos(new BlockPos(2, 2, 2));
		BlockPos second = test.absolutePos(new BlockPos(4, 2, 2));
		test.setBlock(2, 2, 2, Blocks.BARREL);
		test.setBlock(4, 2, 2, Blocks.BARREL);
		BarrelBlockEntity firstBarrel = (BarrelBlockEntity) test.getLevel().getBlockEntity(first);
		BarrelBlockEntity secondBarrel = (BarrelBlockEntity) test.getLevel().getBlockEntity(second);
		firstBarrel.setItem(0, new ItemStack(Items.DIAMOND, 2));
		BoundingBox box = new BoundingBox(first.getX(), first.getY(), first.getZ(),
				second.getX(), second.getY(), second.getZ());
		test.assertTrue(ModStructurePopulator.placeGulagCompass(test.getLevel(), box),
				"A Gulag must receive a compass");
		test.assertTrue(ModStructurePopulator.placeGulagCompass(test.getLevel(), box),
				"Retrying placement must recognize the existing compass");
		int count = 0;
		for (BarrelBlockEntity barrel : new BarrelBlockEntity[] {firstBarrel, secondBarrel}) {
			for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
				if (barrel.getItem(slot).is(ModItems.RESET_COMPASS)) {
					count++;
				}
			}
		}
		test.assertTrue(count == 1, "Exactly one Gulag barrel must contain the compass");
		test.assertTrue(firstBarrel.getItem(0).is(Items.DIAMOND) && firstBarrel.getItem(0).getCount() == 2,
				"The compass must preserve existing loot");
		test.succeed();
	}

	@GameTest(maxTicks = 40)
	public void obtainingResetCompassGrantsAdvancement(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		ItemStack compass = new ItemStack(ModItems.RESET_COMPASS);
		test.assertTrue(player.getInventory().add(compass), "The player must receive the compass");
		// Mock players do not run their normal inventory tick in every GameTest environment.
		player.getInventory().tick();
		test.runAfterDelay(2, () -> {
			var advancement = test.getLevel().getServer().getAdvancements()
					.get(HbkMod.id("find_enemies_of_people"));
			test.assertTrue(advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone(),
					"Receiving the compass must award Find the Enemies of the People");
			test.assertTrue(player.getInventory().getItem(0).has(DataComponents.LODESTONE_TRACKER),
					"The compass must initialize its needle target while in inventory");
			player.getInventory().clearContent();
			test.succeed();
		});
	}

	@GameTest
	public void graveyardPlacesChestWithRecipeBook(GameTestHelper test) {
		var level = test.getLevel();
		BlockPos center = test.absolutePos(new BlockPos(2, 2, 2));
		BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
		var graveyard = new BoundingBox(center.getX(), surface.getY(), center.getZ(),
				center.getX(), surface.getY() + 1, center.getZ());
		test.assertTrue(ModStructurePopulator.placeRecipeChest(level, graveyard),
				"A graveyard must place a nearby chest on a free surface");
		int found = 0;
		for (int x = center.getX() - 4; x <= center.getX() + 4; x++) {
			for (int z = center.getZ() - 4; z <= center.getZ() + 4; z++) {
				BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
				if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest
						&& chest.getItem(13).is(Items.WRITTEN_BOOK)) {
					WrittenBookContent content = chest.getItem(13).get(DataComponents.WRITTEN_BOOK_CONTENT);
					test.assertTrue(content != null && content.pages().size() == GraveyardRecipeBook.RECIPES.size() + 1,
							"Graveyard book must contain an introduction and one page per recipe");
					found++;
				}
			}
		}
		test.assertTrue(found == 1, "Graveyard must contain exactly one recipe chest");
		test.succeed();
	}

	@GameTest
	public void everyGuideRecipeHasVanillaRecipeBookUnlock(GameTestHelper test) {
		var server = test.getLevel().getServer();
		for (String name : GraveyardRecipeBook.RECIPES) {
			var recipe = ResourceKey.create(Registries.RECIPE, HbkMod.id(name));
			test.assertTrue(server.getRecipeManager().byKey(recipe).isPresent(),
					"Guide recipe must load: " + name);
			var advancement = server.getAdvancements().get(HbkMod.id("recipes/" + name));
			test.assertTrue(advancement != null && advancement.value().rewards().recipes().contains(recipe),
					"Guide recipe must have a vanilla recipe-book unlock: " + name);
		}
		test.succeed();
	}

	@GameTest(maxTicks = 40)
	public void collectingIngredientUnlocksRecipeInVanillaBook(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		var recipe = ResourceKey.create(Registries.RECIPE, HbkMod.id("buckwheat"));
		test.assertTrue(player.getInventory().add(new ItemStack(Blocks.DIRT)), "The player must receive dirt");
		player.getInventory().tick();
		test.runAfterDelay(2, () -> {
			test.assertTrue(player.getRecipeBook().contains(recipe),
					"Dirt must unlock the buckwheat recipe in the vanilla recipe book");
			player.getInventory().clearContent();
			test.succeed();
		});
	}
}
