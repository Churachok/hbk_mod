package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;

public final class ModToolMaterials {
	/** Netherite stats, with a separate harvest tier that permits uranium ore. */
	public static final ToolMaterial REDSTONE = new ToolMaterial(
			TagKey.create(Registries.BLOCK, HbkMod.id("incorrect_for_redstone_tool")),
			ToolMaterial.NETHERITE.durability(),
			ToolMaterial.NETHERITE.speed(),
			ToolMaterial.NETHERITE.attackDamageBonus(),
			ToolMaterial.NETHERITE.enchantmentValue(),
			ToolMaterial.NETHERITE.repairItems()
	);

	private ModToolMaterials() {
	}
}
