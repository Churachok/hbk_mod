package dev.kirill.hbk.universe;

import com.google.gson.Gson;
import dev.kirill.hbk.HbkMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registration and the recipe catalogue share one generated, versioned manifest. */
public final class UniverseAlloys {
	public record Reward(String id, String role, String model, String ru, String en, String base) {}
	public record Alloy(int first, int second, String id, String ru, String en, List<Reward> rewards) {}
	public static final List<Alloy> CATALOG = readCatalog();
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final Map<String, Block> DEVICES = new LinkedHashMap<>();
	public static BlockEntityType<AlloyDeviceBlockEntity> DEVICE_TYPE;
	public static EntityType<CargoMinecart> CARGO_MINECART;
	public static Block ROPE_LADDER;
	public static final TagKey<Item> BAG_CONTENTS = TagKey.create(Registries.ITEM, HbkMod.id("sample_bag_contents"));
	private UniverseAlloys() {}
	private static List<Alloy> readCatalog() {
		try (var stream = Objects.requireNonNull(UniverseAlloys.class.getResourceAsStream("/hbk/alloys.json"));
			 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			var values = List.of(new Gson().fromJson(reader, Alloy[].class));
			if (values.size() != 45 || values.stream().map(a -> a.first + ":" + a.second).distinct().count() != 45)
				throw new IllegalStateException("Invalid alloy catalogue");
			return values;
		} catch (Exception e) { throw new IllegalStateException("Cannot load HBK alloys", e); }
	}
	public static Item item(String id) { return Objects.requireNonNull(ITEMS.get(id), id); }
	public static Block device(String id) { return Objects.requireNonNull(DEVICES.get(id), id); }
	public static boolean is(ItemStack stack, String id) { return ITEMS.containsKey(id) && stack.is(ITEMS.get(id)); }
	public static Alloy alloyForReward(String id) {
		return CATALOG.stream().filter(a -> a.rewards.stream().anyMatch(r -> r.id.equals(id))).findFirst().orElseThrow();
	}
	public static List<String> recipes() {
		return CATALOG.stream().flatMap(a -> java.util.stream.Stream.concat(
				java.util.stream.Stream.of(a.id + "_sample", a.id + "_blasting", a.id + "_block", a.id + "_from_block"),
				a.rewards.stream().map(r -> r.id + "_smithing"))).toList();
	}
	public static void register() {
		var entityKey = ResourceKey.create(Registries.ENTITY_TYPE, HbkMod.id("cargo_minecart"));
		CARGO_MINECART = Registry.register(BuiltInRegistries.ENTITY_TYPE, entityKey,
				EntityType.Builder.<CargoMinecart>of(CargoMinecart::new, MobCategory.MISC).sized(.98f, .7f).clientTrackingRange(8).build(entityKey));
		for (Alloy alloy : CATALOG) {
			add(alloy.id + "_sample", new Item.Properties(), p -> new AlloyMaterialItem(alloy, true, p));
			add(alloy.id + "_ingot", new Item.Properties().fireResistant(), p -> new AlloyMaterialItem(alloy, false, p));
			String storage = alloy.id + "_block";
			var blockKey = ResourceKey.create(Registries.BLOCK, HbkMod.id(storage));
			var block = Registry.register(BuiltInRegistries.BLOCK, blockKey, new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(blockKey)));
			add(storage, new Item.Properties().useBlockDescriptionPrefix(), p -> new BlockItem(block, p));
			var repair = TagKey.create(Registries.ITEM, HbkMod.id("repairs_" + alloy.id));
			var baseTool = ToolMaterial.NETHERITE;
			var tool = new ToolMaterial(baseTool.incorrectBlocksForDrops(), baseTool.durability(), baseTool.speed(), baseTool.attackDamageBonus(), baseTool.enchantmentValue(), repair);
			var baseArmor = ArmorMaterials.NETHERITE;
			var armor = new ArmorMaterial(baseArmor.durability(), baseArmor.defense(), baseArmor.enchantmentValue(), baseArmor.equipSound(),
					baseArmor.toughness(), baseArmor.knockbackResistance(), repair, ResourceKey.create(EquipmentAssets.ROOT_ID, HbkMod.id(alloy.id)));
			for (Reward r : alloy.rewards) {
				if (r.role.equals("device")) {
					var key = ResourceKey.create(Registries.BLOCK, HbkMod.id(r.id));
					var p = BlockBehaviour.Properties.ofFullCopy(r.id.equals("phase_glass") ? Blocks.TINTED_GLASS : Blocks.IRON_BLOCK).setId(key).strength(4, 6);
					if (r.id.equals("growth_lamp")) p.lightLevel(s -> 15);
					if (r.id.equals("phase_glass")) p.noOcclusion();
					var device = Registry.register(BuiltInRegistries.BLOCK, key, new AlloyDeviceBlock(r.id, p));
					DEVICES.put(r.id, device);
					add(r.id, new Item.Properties().fireResistant().useBlockDescriptionPrefix(), q -> new AlloyDeviceItem(device, r.id, q));
					continue;
				}
				var p = new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON);
				switch (r.role) {
					case "sword" -> p.sword(tool, 3, -2.4f);
					case "pickaxe" -> p.pickaxe(tool, 1, -2.8f);
					case "axe" -> p.axe(tool, 5, -3);
					case "shovel" -> p.shovel(tool, 1.5f, -3);
					case "hoe" -> p.hoe(tool, -3, 0);
					case "helmet", "chestplate", "leggings", "boots" -> p.humanoidArmor(armor, switch (r.role) {
						case "helmet" -> ArmorType.HELMET; case "chestplate" -> ArmorType.CHESTPLATE;
						case "leggings" -> ArmorType.LEGGINGS; default -> ArmorType.BOOTS;
					});
					case "shield" -> p.durability(672).repairable(repair).equippableUnswappable(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
							.component(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("shield"))
							.component(DataComponents.BANNER_PATTERNS, net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY)
							.component(DataComponents.BREAK_SOUND, net.minecraft.sounds.SoundEvents.SHIELD_BREAK)
							.delayedComponent(DataComponents.BLOCKS_ATTACKS, lookup -> new net.minecraft.world.item.component.BlocksAttacks(.25f, 1,
									List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90, Optional.empty(), 0, 1)),
									new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3, 1, 1),
									Optional.of(lookup.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
									Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK), Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)));
					case "crossbow" -> p.durability(930).repairable(repair).enchantable(1)
							.component(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("crossbow"))
							.component(DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.EMPTY);
					case "minecart" -> p.stacksTo(1);
					default -> {
						if (Set.of("return_capsule", "shaped_charge").contains(r.id)) p.stacksTo(16);
						else p.durability(512).repairable(repair);
					}
				}
				if (r.id.equals("blast_shield")) p.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder().add(
						net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
						new net.minecraft.world.entity.ai.attributes.AttributeModifier(HbkMod.id("blast_shield"), .65, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.HAND).build());
				if (r.id.equals("diving_boots")) p.attributes(armor.createAttributes(ArmorType.BOOTS).withModifierAdded(
						net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY,
						new net.minecraft.world.entity.ai.attributes.AttributeModifier(HbkMod.id("diving_boots"), .8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.FEET));
				if (Set.of("compass", "recovery_compass", "clock", "bundle").contains(r.model))
					p.component(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace(r.id.equals("ore_compass") ? "compass" : r.model));
				add(r.id, p, q -> r.role.equals("crossbow") ? new ImpulseCrossbow(q) : r.role.equals("minecart")
						? new MinecartItem(CARGO_MINECART, q) : new AlloyItem(r.id, q));
			}
		}
		DEVICE_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, HbkMod.id("alloy_device"),
				new BlockEntityType<>(AlloyDeviceBlockEntity::new, new HashSet<>(DEVICES.values())));
		var ladderKey = ResourceKey.create(Registries.BLOCK, HbkMod.id("alloy_rope_ladder"));
		ROPE_LADDER = Registry.register(BuiltInRegistries.BLOCK, ladderKey, new AlloyRopeLadder(BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER).setId(ladderKey).noLootTable()));
		AlloyActions.register();
	}
	private static Item add(String id, Item.Properties properties, java.util.function.Function<Item.Properties, Item> factory) {
		var key = ResourceKey.create(Registries.ITEM, HbkMod.id(id));
		Item value = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.put(id, value); return value;
	}
}
