package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class ModGameRules {
	public static final GameRule<Integer> KONATA_SHPERM_LIMIT = GameRuleBuilder.forInteger(10)
			.category(GameRuleCategory.DROPS)
			.minValue(0)
			.buildAndRegister(HbkMod.id("konata_shperm_limit"));

	private ModGameRules() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered game rules for {}", HbkMod.MOD_ID);
	}
}
