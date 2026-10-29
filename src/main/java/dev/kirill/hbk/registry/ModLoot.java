package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public final class ModLoot {
	public static final float FURRY_SYRINGE_CHANCE = 0.15f;
	private static final Set<ResourceKey<LootTable>> FURRY_SYRINGE_CHESTS = Set.of(
			vanillaChest("simple_dungeon"), vanillaChest("abandoned_mineshaft"),
			vanillaChest("desert_pyramid"), vanillaChest("jungle_temple"),
			vanillaChest("stronghold_corridor"), vanillaChest("stronghold_crossing"),
			vanillaChest("stronghold_library"), vanillaChest("woodland_mansion"),
			vanillaChest("ancient_city"), vanillaChest("buried_treasure"),
			vanillaChest("shipwreck_treasure"), vanillaChest("underwater_ruin_big"),
			vanillaChest("underwater_ruin_small"), vanillaChest("pillager_outpost"),
			vanillaChest("nether_bridge"), vanillaChest("bastion_bridge"),
			vanillaChest("bastion_hoglin_stable"), vanillaChest("bastion_other"),
			vanillaChest("bastion_treasure"), vanillaChest("end_city_treasure"),
			vanillaChest("ruined_portal"), vanillaChest("trial_chambers/reward"),
			vanillaChest("trial_chambers/reward_ominous"),
			modChest("grove_street"), modChest("radioactive_ruin"),
			modChest("parallel_mine_primordial"), modChest("parallel_mine_celestial"),
			modChest("parallel_mine_columnar"), modChest("parallel_mine_cavernous"),
			modChest("parallel_mine_woven"), modChest("parallel_mine_sedimentary"),
			modChest("parallel_mine_labyrinthine"), modChest("parallel_mine_fragmented"),
			modChest("parallel_mine_inverse"), modChest("parallel_mine_stratified"));
	private static final ResourceKey<LootTable> GOSHA_LOOT = lootTable(HbkMod.id("entities/gosha"));
	private static final ResourceKey<LootTable> PILLAGER_LOOT = lootTable(
			Identifier.withDefaultNamespace("entities/pillager"));

	private ModLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (key.equals(GOSHA_LOOT)) {
				tableBuilder.withPool(goshasRagePotionPool(0.5f));
			} else if (key.equals(PILLAGER_LOOT)) {
				tableBuilder.withPool(goshasRagePotionPool(0.1f));
			}
			// Grove Street's bonus table already calls its main table once. Trial reward
			// sub-tables may be called repeatedly; only their top-level rewards roll here.
			if (source.isBuiltin() && FURRY_SYRINGE_CHESTS.contains(key)) {
				tableBuilder.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.when(LootItemRandomChanceCondition.randomChance(FURRY_SYRINGE_CHANCE))
						.add(LootItem.lootTableItem(ModItems.FURRY_SYRINGE)));
			}
		});
	}

	private static LootPool.Builder goshasRagePotionPool(float chance) {
		return LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1))
				.when(LootItemRandomChanceCondition.randomChance(chance))
				.add(LootItem.lootTableItem(ModItems.GOSHAS_RAGE_BOTTLE));
	}

	private static ResourceKey<LootTable> lootTable(Identifier id) {
		return ResourceKey.create(Registries.LOOT_TABLE, id);
	}

	private static ResourceKey<LootTable> vanillaChest(String path) {
		return lootTable(Identifier.withDefaultNamespace("chests/" + path));
	}

	private static ResourceKey<LootTable> modChest(String path) {
		return lootTable(HbkMod.id("chests/" + path));
	}
}
