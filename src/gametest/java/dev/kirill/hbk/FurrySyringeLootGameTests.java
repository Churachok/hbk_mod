package dev.kirill.hbk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.registry.ModLoot;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.fabricmc.fabric.impl.loot.LootUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FurrySyringeLootGameTests {
	private static final List<ResourceKey<LootTable>> DIRECT_TABLES = directTables();
	private static final ResourceKey<LootTable> GROVE_BONUS = key("hbk:chests/grove_street_bonus");
	private static final Set<ResourceKey<LootTable>> EXPLORATION_MAP_TABLES = Set.of(
			key("minecraft:chests/underwater_ruin_big"), key("minecraft:chests/underwater_ruin_small"));
	private static final List<String> UNCHANGED_NESTED_TABLES = List.of(
			"hbk:chests/grove_street_bonus",
			"minecraft:chests/trial_chambers/reward_common",
			"minecraft:chests/trial_chambers/reward_rare",
			"minecraft:chests/trial_chambers/reward_unique",
			"minecraft:chests/trial_chambers/reward_ominous_common",
			"minecraft:chests/trial_chambers/reward_ominous_rare",
			"minecraft:chests/trial_chambers/reward_ominous_unique");

	@GameTest
	public void allowedRegistryTablesKeepTheirOriginalLootAndAppendOneExactFifteenPercentPool(GameTestHelper test) {
		var params = chestParams(test);
		for (var key : DIRECT_TABLES) {
			var original = originalTable(test, key);
			var actual = registryTable(test, key);
			var originalJson = encode(test, original);
			var actualJson = encode(test, actual);
			if (!activeSource(test, key).isBuiltin()) {
				// GameTest enables trade_rebalance too. Its native experimental overrides
				// are DATA_PACK sources and must retain their own extra vanilla pools verbatim.
				test.assertTrue(actualJson.equals(originalJson), key + " datapack override must remain entirely unchanged");
				continue;
			}
			var pools = actualJson.getAsJsonArray("pools");
			test.assertTrue(pools.size() == originalJson.getAsJsonArray("pools").size() + 1,
					key + " must append exactly one bonus pool, without replacing any original pools");
			var bonus = pools.remove(pools.size() - 1).getAsJsonObject();
			test.assertTrue(actualJson.equals(originalJson), key + " must preserve every original pool, function and random sequence");
			assertSyringePool(test, key, bonus);
			// Ocean ruins locate real buried treasure and allocate fresh map IDs. Their
			// complete original definitions are checked above, without a world-generation search.
			if (EXPLORATION_MAP_TABLES.contains(key)) continue;
			for (long seed = 1; seed <= 8; seed++) {
				var expectedLoot = original.getRandomItems(params, seed);
				var actualLoot = actual.getRandomItems(params, seed).stream().filter(stack -> !stack.is(ModItems.FURRY_SYRINGE)).toList();
				test.assertTrue(expectedLoot.size() == actualLoot.size(), key + " must retain all original seeded loot");
				for (int slot = 0; slot < expectedLoot.size(); slot++) {
					test.assertTrue(ItemStack.matches(expectedLoot.get(slot), actualLoot.get(slot)),
							key + " must not change seeded original stack counts or components");
				}
			}
		}
		for (var id : UNCHANGED_NESTED_TABLES) {
			var key = key(id);
			test.assertTrue(encode(test, registryTable(test, key)).equals(encode(test, originalTable(test, key))),
					id + " must not gain another pool: nested calls must not multiply the syringe chance");
		}
		test.succeed();
	}

	@GameTest
	public void realChestLootIncludingNestedRewardsRollsAtMostOneSyringeAtAModerateRate(GameTestHelper test) {
		var tables = new ArrayList<>(DIRECT_TABLES);
		// Keep this a loot test rather than repeatedly searching chunks for treasure maps.
		// Both ocean-ruin tables still receive the exact registry-pool checks in the first test.
		tables.removeAll(EXPLORATION_MAP_TABLES);
		tables.add(GROVE_BONUS);
		var params = chestParams(test);
		var seeds = RandomSource.create(0x46555252594c4f4fL);
		int hits = 0;
		int rolls = 0;
		for (var key : tables) {
			var table = registryTable(test, key);
			boolean builtin = activeSource(test, key).isBuiltin();
			int tableHits = 0;
			for (int roll = 0; roll < 128; roll++) {
				long seed = seeds.nextLong();
				if (seed == 0) seed = 1; // Native seed zero selects non-deterministic loot instead.
				var loot = table.getRandomItems(params, seed);
				int count = loot.stream().filter(stack -> stack.is(ModItems.FURRY_SYRINGE)).mapToInt(ItemStack::getCount).sum();
				test.assertTrue(count == 0 || count == 1, key + " must never yield multiple syringes, even through nested reward pools");
				tableHits += count;
			}
			if (!builtin) {
				test.assertTrue(tableHits == 0, key + " datapack override must never receive the built-in syringe bonus");
				continue;
			}
			test.assertTrue(tableHits > 0 && tableHits < 45,
					key + " must offer a real but non-guaranteed syringe chance across fixed seeds: " + tableHits
							+ "; actual registry table=" + encode(test, table));
			hits += tableHits;
			rolls += 128;
		}
		// The sample and seeds are fixed; the broad margin catches wrong probability without a flaky statistical test.
		test.assertTrue(rolls > 0 && hits >= rolls * 0.12 && hits <= rolls * 0.18,
				"Real registry chest generation must stay close to 15% across all supported tables: " + hits + "/" + rolls);
		test.succeed();
	}

	@GameTest
	public void datapackOverridesAndUnlistedLootAreUntouchedByTheActualModifyEvent(GameTestHelper test) {
		for (var key : DIRECT_TABLES) {
			for (var source : List.of(LootTableSource.VANILLA, LootTableSource.MOD, LootTableSource.DATA_PACK, LootTableSource.REPLACED)) {
				var builder = diamondTable();
				LootTableEvents.MODIFY.invoker().modifyLootTable(key, builder, source, test.getLevel().registryAccess());
				var pools = encode(test, builder.build()).getAsJsonArray("pools");
				boolean builtin = source == LootTableSource.VANILLA || source == LootTableSource.MOD;
				test.assertTrue(pools.size() == (builtin ? 2 : 1),
						key + " must add one bonus only for built-in tables, never a datapack or replacement: " + source);
				if (builtin) assertSyringePool(test, key, pools.get(1).getAsJsonObject());
				else test.assertTrue(encode(test, builder.build()).equals(encode(test, diamondTable().build())),
						key + " must preserve the complete datapack or replacement definition");
			}
		}
		for (String id : List.of("minecraft:chests/spawn_bonus_chest", "minecraft:chests/village/village_armorer",
				"minecraft:chests/shipwreck_supply", "minecraft:chests/shipwreck_map", "minecraft:chests/ancient_city_ice_box",
				"minecraft:chests/jungle_temple_dispenser", "minecraft:chests/trial_chambers/supply",
				"minecraft:gameplay/fishing", "minecraft:entities/zombie", "minecraft:blocks/chest",
				"hbk:chests/parallel_mine_unlisted", "othermod:chests/simple_dungeon")) {
			assertEventLeavesTableAlone(test, id);
		}
		for (var id : UNCHANGED_NESTED_TABLES) assertEventLeavesTableAlone(test, id);
		test.succeed();
	}

	private static void assertEventLeavesTableAlone(GameTestHelper test, String id) {
		var builder = diamondTable();
		var expected = encode(test, diamondTable().build());
		LootTableEvents.MODIFY.invoker().modifyLootTable(key(id), builder, LootTableSource.VANILLA, test.getLevel().registryAccess());
		test.assertTrue(encode(test, builder.build()).equals(expected), id + " must stay outside the direct syringe allowlist");
	}

	private static LootTable.Builder diamondTable() {
		return LootTable.lootTable().setParamSet(LootContextParamSets.CHEST)
				.withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.DIAMOND)));
	}

	private static LootParams chestParams(GameTestHelper test) {
		return new LootParams.Builder(test.getLevel())
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(test.absolutePos(BlockPos.ZERO)))
				.create(LootContextParamSets.CHEST);
	}

	private static LootTable registryTable(GameTestHelper test, ResourceKey<LootTable> key) {
		var table = test.getLevel().getServer().reloadableRegistries().getLootTable(key);
		test.assertTrue(table != LootTable.EMPTY, "The real loaded loot table must exist: " + key);
		return table;
	}

	private static LootTable originalTable(GameTestHelper test, ResourceKey<LootTable> key) {
		try (var reader = activeResource(test, key).openAsReader()) {
			return LootTable.DIRECT_CODEC.parse(test.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE),
					JsonParser.parseReader(reader)).getOrThrow();
		} catch (IOException exception) {
			throw new AssertionError("Cannot read the active loot resource " + key, exception);
		}
	}

	private static Resource activeResource(GameTestHelper test, ResourceKey<LootTable> key) {
		var id = key.identifier();
		var resourceId = Identifier.fromNamespaceAndPath(id.getNamespace(), "loot_table/" + id.getPath() + ".json");
		return test.getLevel().getServer().getResourceManager().getResource(resourceId)
				.orElseThrow(() -> new AssertionError("Missing active loot resource " + resourceId));
	}

	private static LootTableSource activeSource(GameTestHelper test, ResourceKey<LootTable> key) {
		// Use the loader's real classification, including native experimental packs;
		// namespace alone cannot distinguish vanilla assets from datapack overrides.
		return LootUtil.determineSource(activeResource(test, key));
	}

	private static void assertSyringePool(GameTestHelper test, ResourceKey<LootTable> key, JsonObject bonus) {
		var condition = bonus.getAsJsonArray("conditions");
		var entries = bonus.getAsJsonArray("entries");
		test.assertTrue(condition != null && entries != null,
				key + " must end with the added conditional item pool; actual removed pool=" + bonus);
		test.assertTrue(bonus.get("rolls").getAsFloat() == 1.0f && condition.size() == 1
				&& condition.get(0).getAsJsonObject().get("condition").getAsString().equals("minecraft:random_chance")
				&& condition.get(0).getAsJsonObject().get("chance").getAsFloat() == ModLoot.FURRY_SYRINGE_CHANCE
				&& ModLoot.FURRY_SYRINGE_CHANCE == 0.15f && entries.size() == 1
				&& entries.get(0).getAsJsonObject().get("type").getAsString().equals("minecraft:item")
				&& entries.get(0).getAsJsonObject().get("name").getAsString().equals("hbk:furry_syringe")
				&& !bonus.has("functions") && !entries.get(0).getAsJsonObject().has("functions"),
				key + " must roll 15% once for a single default-count syringe, never a variable-sized stack");
	}

	private static JsonObject encode(GameTestHelper test, LootTable table) {
		return LootTable.DIRECT_CODEC.encodeStart(test.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), table)
				.getOrThrow().getAsJsonObject().deepCopy();
	}

	private static List<ResourceKey<LootTable>> directTables() {
		var result = new ArrayList<ResourceKey<LootTable>>();
		for (var path : List.of("simple_dungeon", "abandoned_mineshaft", "desert_pyramid", "jungle_temple",
				"stronghold_corridor", "stronghold_crossing", "stronghold_library", "woodland_mansion", "ancient_city",
				"buried_treasure", "shipwreck_treasure", "underwater_ruin_big", "underwater_ruin_small", "pillager_outpost",
				"nether_bridge", "bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure",
				"end_city_treasure", "ruined_portal", "trial_chambers/reward", "trial_chambers/reward_ominous")) {
			result.add(key("minecraft:chests/" + path));
		}
		for (var path : List.of("grove_street", "radioactive_ruin", "parallel_mine_primordial", "parallel_mine_celestial",
				"parallel_mine_columnar", "parallel_mine_cavernous", "parallel_mine_woven", "parallel_mine_sedimentary",
				"parallel_mine_labyrinthine", "parallel_mine_fragmented", "parallel_mine_inverse", "parallel_mine_stratified")) {
			result.add(key("hbk:chests/" + path));
		}
		return List.copyOf(result);
	}

	private static ResourceKey<LootTable> key(String id) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id));
	}
}
