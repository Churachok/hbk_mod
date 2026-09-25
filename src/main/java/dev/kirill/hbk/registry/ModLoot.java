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

public final class ModLoot {
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
}
