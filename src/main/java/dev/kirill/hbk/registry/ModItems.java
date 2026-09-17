package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.item.CurrantTinctureItem;
import dev.kirill.hbk.item.AttackingMemberItem;
import dev.kirill.hbk.item.ArmoredMemberItem;
import dev.kirill.hbk.item.ColossalMemberItem;
import dev.kirill.hbk.item.JawMemberItem;
import dev.kirill.hbk.item.CarrierMemberItem;
import dev.kirill.hbk.item.BeastlikeMemberItem;
import dev.kirill.hbk.item.HammerFighterMemberItem;
import dev.kirill.hbk.item.FemaleVaginaItem;
import dev.kirill.hbk.item.CondensedMilkItem;
import dev.kirill.hbk.item.BandageItem;
import dev.kirill.hbk.item.BuckwheatItem;
import dev.kirill.hbk.item.FlyingCarpetItem;
import dev.kirill.hbk.item.GoldenCrownItem;
import dev.kirill.hbk.item.RationItem;
import dev.kirill.hbk.item.ShovelSwordItem;
import dev.kirill.hbk.item.SickleAndHammerItem;
import dev.kirill.hbk.item.StewItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Function;
import java.util.List;
import java.util.Optional;

public class ModItems {
	public static final Item INFECTED_DIRT = block("infected_dirt", ModBlocks.INFECTED_DIRT);
	public static final Item RADIOACTIVE_STONE = block("radioactive_stone", ModBlocks.RADIOACTIVE_STONE);
	public static final Item ASH_SOIL = block("ash_soil", ModBlocks.ASH_SOIL);
	public static final Item CRACKED_RADIOACTIVE_STONE = block("cracked_radioactive_stone", ModBlocks.CRACKED_RADIOACTIVE_STONE);
	public static final Item URANIUM_ORE = block("uranium_ore", ModBlocks.URANIUM_ORE);
	public static final Item URANIUM_235 = register(
			"uranium_235",
			Item::new,
			new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.RARE)
	);
	public static final Item SEDIMENT_MUD = block("sediment_mud", ModBlocks.SEDIMENT_MUD);
	public static final Item TOXIC_WATER = block("toxic_water", ModBlocks.TOXIC_WATER);
	public static final Item SCORCHED_STONE = block("scorched_stone", ModBlocks.SCORCHED_STONE);
	public static final Item RADIOACTIVE_SAND = block("radioactive_sand", ModBlocks.RADIOACTIVE_SAND);
	public static final Item DEAD_GRASS = block("dead_grass", ModBlocks.DEAD_GRASS);
	public static final Item INFECTED_BUSH = block("infected_bush", ModBlocks.INFECTED_BUSH);
	public static final Item MOLDY_MOSS = block("moldy_moss", ModBlocks.MOLDY_MOSS);
	public static final Item GLOWING_MUSHROOM = block("glowing_mushroom", ModBlocks.GLOWING_MUSHROOM);
	public static final Item DRY_LOG = block("dry_log", ModBlocks.DRY_LOG);
	public static final Item BRIGHT_GRASS = block("bright_grass", ModBlocks.BRIGHT_GRASS);
	public static final Item ROTTEN_EARTH = block("rotten_earth", ModBlocks.ROTTEN_EARTH);
	public static final Item WHITE_VISCOUS_LIQUID = block("white_viscous_liquid", ModBlocks.WHITE_VISCOUS_LIQUID);
	public static final Item BLOODY_VISCOUS_LIQUID = block("bloody_viscous_liquid", ModBlocks.BLOODY_VISCOUS_LIQUID);
	public static final Item RUINED_CONCRETE = block("ruined_concrete", ModBlocks.RUINED_CONCRETE);
	public static final Item BUILDING_DEBRIS = block("building_debris", ModBlocks.BUILDING_DEBRIS);
	public static final Item CRIMSON_MONUMENT = block("crimson_monument", ModBlocks.CRIMSON_MONUMENT);
	public static final Item BUCKWHEAT = register(
			"buckwheat",
			BuckwheatItem::new,
			new Item.Properties().food(new FoodProperties.Builder()
					.nutrition(20)
					.saturationModifier(1.0f)
					.alwaysEdible()
					.build())
	);

	public static final Item STEW = register(
			"stew",
			StewItem::new,
			new Item.Properties()
					.stacksTo(16)
					.food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f).alwaysEdible().build())
					.usingConvertsTo(Items.IRON_NUGGET)
	);

	public static final Item BANDAGE = register(
			"bandage",
			BandageItem::new,
			new Item.Properties().stacksTo(16)
	);

	public static final Item FLYING_CARPET = register(
			"flying_carpet",
			FlyingCarpetItem::new,
			new Item.Properties().stacksTo(1)
	);

	public static final Item SICKLE_AND_HAMMER = register(
			"sickle_and_hammer",
			SickleAndHammerItem::new,
			new Item.Properties().axe(ToolMaterial.IRON, 5.0f, -3.2f)
	);

	public static final Item CONDENSED_MILK = register(
			"condensed_milk",
			CondensedMilkItem::new,
			new Item.Properties()
					.stacksTo(16)
					.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build())
					.usingConvertsTo(Items.IRON_NUGGET)
	);

	public static final Item WESTERN_CHESTPLATE = register(
			"western_chestplate",
			Item::new,
			new Item.Properties().humanoidArmor(ArmorMaterials.IRON, ArmorType.CHESTPLATE)
	);

	public static final Item URANIUM_HELMET = armor("uranium_helmet", ArmorType.HELMET);
	public static final Item URANIUM_CHESTPLATE = armor("uranium_chestplate", ArmorType.CHESTPLATE);
	public static final Item URANIUM_LEGGINGS = armor("uranium_leggings", ArmorType.LEGGINGS);
	public static final Item URANIUM_BOOTS = armor("uranium_boots", ArmorType.BOOTS);

	public static final Item ATTACKING_MEMBER = register(
			"attacking_member",
			AttackingMemberItem::new,
			new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
	);
	public static final Item ARMORED_MEMBER = register(
			"armored_member",
			ArmoredMemberItem::new,
			memberShieldProperties()
	);
	public static final Item COLOSSAL_MEMBER = register(
			"colossal_member",
			ColossalMemberItem::new,
			new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)
	);
	public static final Item JAW_MEMBER = register(
			"jaw_member",
			JawMemberItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 12.0f, -2.4f).rarity(net.minecraft.world.item.Rarity.RARE)
	);
	public static final Item CARRIER_MEMBER = register(
			"carrier_member",
			CarrierMemberItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 4.0f, -2.4f).rarity(net.minecraft.world.item.Rarity.RARE)
	);
	public static final Item BEASTLIKE_MEMBER = register(
			"beastlike_member",
			BeastlikeMemberItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 12.0f, -2.4f).rarity(net.minecraft.world.item.Rarity.RARE)
	);
	public static final Item HAMMER_FIGHTER_MEMBER = register(
			"hammer_fighter_member",
			HammerFighterMemberItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 9.0f, -2.4f).rarity(net.minecraft.world.item.Rarity.RARE)
	);
	public static final Item FEMALE_VAGINA = register(
			"female_vagina",
			FemaleVaginaItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 3.0f, -2.4f).rarity(net.minecraft.world.item.Rarity.RARE)
	);

	public static final Item BALALAIKA_PICKAXE = register(
			"balalaika_pickaxe",
			Item::new,
			new Item.Properties().pickaxe(ToolMaterial.IRON, 1.0f, -2.8f)
	);

	public static final Item CURRANT_TINCTURE = register(
			"currant_tincture",
			CurrantTinctureItem::new,
			new Item.Properties()
					.stacksTo(1)
					.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
					.usingConvertsTo(Items.GLASS_BOTTLE)
	);

	public static final Item RATION = register("ration", RationItem::new, new Item.Properties().stacksTo(16));

	public static final Item SUSHKA = register(
			"sushka",
			Item::new,
			new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build())
	);

	public static final Item GOLDEN_CROWN = register(
			"golden_crown",
			GoldenCrownItem::new,
			new Item.Properties().stacksTo(16)
	);

	public static final Item SHOVEL_SWORD = register(
			"shovel_sword",
			ShovelSwordItem::new,
			new Item.Properties().shovel(ToolMaterial.IRON, 1.5f, -3.0f)
	);

	public static final Item STRANGE_CHEST = register(
			"strange_chest",
			properties -> new BlockItem(ModBlocks.STRANGE_CHEST, properties),
			new Item.Properties()
	);

	public static final Item STALIN_SPAWN_EGG = register(
			"stalin_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.STALIN)
	);

	public static final Item CJ_SPAWN_EGG = register(
			"cj_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.CJ)
	);

	public static final Item NURSE_SPAWN_EGG = register(
			"nurse_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.NURSE)
	);

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, HbkMod.id(name));
		T item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	private static Item block(String name, net.minecraft.world.level.block.Block block) {
		return register(name, properties -> new BlockItem(block, properties), new Item.Properties());
	}

	private static Item armor(String name, ArmorType type) {
		return register(name, Item::new, new Item.Properties()
				.humanoidArmor(ModArmorMaterials.URANIUM, type)
				.fireResistant()
				.rarity(net.minecraft.world.item.Rarity.EPIC));
	}

	private static Item.Properties memberShieldProperties() {
		return new Item.Properties()
				.sword(ToolMaterial.IRON, 7.0f, -2.4f)
				.durability(336)
				.rarity(net.minecraft.world.item.Rarity.RARE)
				.delayedComponent(DataComponents.BLOCKS_ATTACKS, provider -> new BlocksAttacks(
						0.25f,
						1.0f,
						List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0.0f, 1.0f)),
						new BlocksAttacks.ItemDamageFunction(3.0f, 1.0f, 1.0f),
						Optional.of(provider.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
						Optional.of(SoundEvents.SHIELD_BLOCK),
						Optional.of(SoundEvents.SHIELD_BREAK)
				));
	}

	public static boolean isMemberWeapon(net.minecraft.world.item.ItemStack stack) {
		return stack.is(ATTACKING_MEMBER)
				|| stack.is(ARMORED_MEMBER)
				|| stack.is(COLOSSAL_MEMBER)
				|| stack.is(JAW_MEMBER)
				|| stack.is(CARRIER_MEMBER)
				|| stack.is(BEASTLIKE_MEMBER)
				|| stack.is(HAMMER_FIGHTER_MEMBER)
				|| stack.is(FEMALE_VAGINA);
	}

	public static boolean usesSpecialLeftClick(net.minecraft.world.item.ItemStack stack) {
		return stack.is(ATTACKING_MEMBER) || stack.is(COLOSSAL_MEMBER);
	}

	public static void register() {
		ResourceKey<CreativeModeTab> naturalBlocks = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.withDefaultNamespace("natural_blocks")
		);
		CreativeModeTabEvents.modifyOutputEvent(naturalBlocks).register(output -> {
			output.accept(INFECTED_DIRT);
			output.accept(RADIOACTIVE_STONE);
			output.accept(ASH_SOIL);
			output.accept(CRACKED_RADIOACTIVE_STONE);
			output.accept(URANIUM_ORE);
			output.accept(URANIUM_235);
			output.accept(SEDIMENT_MUD);
			output.accept(TOXIC_WATER);
			output.accept(SCORCHED_STONE);
			output.accept(RADIOACTIVE_SAND);
			output.accept(DEAD_GRASS);
			output.accept(INFECTED_BUSH);
			output.accept(MOLDY_MOSS);
			output.accept(GLOWING_MUSHROOM);
			output.accept(DRY_LOG);
			output.accept(BRIGHT_GRASS);
			output.accept(ROTTEN_EARTH);
			output.accept(WHITE_VISCOUS_LIQUID);
			output.accept(BLOODY_VISCOUS_LIQUID);
			output.accept(RUINED_CONCRETE);
			output.accept(BUILDING_DEBRIS);
			output.accept(CRIMSON_MONUMENT);
		});
		ResourceKey<CreativeModeTab> spawnEggs = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.withDefaultNamespace("spawn_eggs")
		);
		CreativeModeTabEvents.modifyOutputEvent(spawnEggs).register(output -> {
			output.accept(STALIN_SPAWN_EGG);
			output.accept(CJ_SPAWN_EGG);
			output.accept(NURSE_SPAWN_EGG);
		});

		ResourceKey<CreativeModeTab> foodAndDrinks = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.withDefaultNamespace("food_and_drinks")
		);
		CreativeModeTabEvents.modifyOutputEvent(foodAndDrinks).register(output -> {
			output.accept(BUCKWHEAT);
			output.accept(CURRANT_TINCTURE);
			output.accept(RATION);
			output.accept(SUSHKA);
			output.accept(GOLDEN_CROWN);
			output.accept(CONDENSED_MILK);
			output.accept(STEW);
		});

		ResourceKey<CreativeModeTab> toolsAndUtilities = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.withDefaultNamespace("tools_and_utilities")
		);
		CreativeModeTabEvents.modifyOutputEvent(toolsAndUtilities).register(output -> {
			output.accept(FLYING_CARPET);
			output.accept(BALALAIKA_PICKAXE);
			output.accept(SHOVEL_SWORD);
			output.accept(WESTERN_CHESTPLATE);
			output.accept(URANIUM_HELMET);
			output.accept(URANIUM_CHESTPLATE);
			output.accept(URANIUM_LEGGINGS);
			output.accept(URANIUM_BOOTS);
			output.accept(ATTACKING_MEMBER);
			output.accept(ARMORED_MEMBER);
			output.accept(COLOSSAL_MEMBER);
			output.accept(JAW_MEMBER);
			output.accept(CARRIER_MEMBER);
			output.accept(BEASTLIKE_MEMBER);
			output.accept(HAMMER_FIGHTER_MEMBER);
			output.accept(FEMALE_VAGINA);
			output.accept(STRANGE_CHEST);
			output.accept(SICKLE_AND_HAMMER);
			output.accept(BANDAGE);
		});
	}
}
