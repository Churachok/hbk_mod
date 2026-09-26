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
import dev.kirill.hbk.item.ProgenitorTransformationItem;
import dev.kirill.hbk.item.CondensedMilkItem;
import dev.kirill.hbk.item.BandageItem;
import dev.kirill.hbk.item.BuckwheatItem;
import dev.kirill.hbk.item.FlyingCarpetItem;
import dev.kirill.hbk.item.GoldenCrownItem;
import dev.kirill.hbk.item.GoshasRageBottleItem;
import dev.kirill.hbk.item.RationItem;
import dev.kirill.hbk.item.ShovelSwordItem;
import dev.kirill.hbk.item.SickleAndHammerItem;
import dev.kirill.hbk.item.StewItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.ItemStack;
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
	public static final ResourceKey<CreativeModeTab> HBK_CREATIVE_TAB_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			HbkMod.id("hbk")
	);

	private static final ResourceKey<JukeboxSong> HBKAU_JUKEBOX_SONG = ResourceKey.create(
			Registries.JUKEBOX_SONG,
			HbkMod.id("hbkau")
	);

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
	public static final Item SPERM = block("sperm", ModBlocks.SPERM);
	public static final Item BLOODY_SPERM = block("bloody_sperm", ModBlocks.BLOODY_SPERM);
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

	public static final Item IMPROVED_OAK_BOAT = improvedBoat("improved_oak_boat", ModEntityTypes.IMPROVED_OAK_BOAT);
	public static final Item IMPROVED_SPRUCE_BOAT = improvedBoat("improved_spruce_boat", ModEntityTypes.IMPROVED_SPRUCE_BOAT);
	public static final Item IMPROVED_BIRCH_BOAT = improvedBoat("improved_birch_boat", ModEntityTypes.IMPROVED_BIRCH_BOAT);
	public static final Item IMPROVED_JUNGLE_BOAT = improvedBoat("improved_jungle_boat", ModEntityTypes.IMPROVED_JUNGLE_BOAT);
	public static final Item IMPROVED_ACACIA_BOAT = improvedBoat("improved_acacia_boat", ModEntityTypes.IMPROVED_ACACIA_BOAT);
	public static final Item IMPROVED_CHERRY_BOAT = improvedBoat("improved_cherry_boat", ModEntityTypes.IMPROVED_CHERRY_BOAT);
	public static final Item IMPROVED_DARK_OAK_BOAT = improvedBoat("improved_dark_oak_boat", ModEntityTypes.IMPROVED_DARK_OAK_BOAT);
	public static final Item IMPROVED_PALE_OAK_BOAT = improvedBoat("improved_pale_oak_boat", ModEntityTypes.IMPROVED_PALE_OAK_BOAT);
	public static final Item IMPROVED_MANGROVE_BOAT = improvedBoat("improved_mangrove_boat", ModEntityTypes.IMPROVED_MANGROVE_BOAT);
	public static final Item IMPROVED_BAMBOO_RAFT = improvedBoat("improved_bamboo_raft", ModEntityTypes.IMPROVED_BAMBOO_RAFT);

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
	public static final Item PROGENITOR_TRANSFORMATION = register(
			"progenitor_transformation",
			ProgenitorTransformationItem::new,
			new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)
	);
	public static final Item FOUNDING_PENIS = register(
			"founding_penis",
			ProgenitorTransformationItem::new,
			new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)
	);

	public static final Item BALALAIKA_PICKAXE = register(
			"balalaika_pickaxe",
			Item::new,
			new Item.Properties().pickaxe(ToolMaterial.IRON, 1.0f, -2.8f)
	);

	public static final Item MUSIC_DISC_HBKAU = register(
			"music_disc_hbkau",
			Item::new,
			new Item.Properties()
					.stacksTo(1)
					.rarity(net.minecraft.world.item.Rarity.UNCOMMON)
					.jukeboxPlayable(HBKAU_JUKEBOX_SONG)
	);

	public static final Item REDSTONE_PICKAXE = register(
			"redstone_pickaxe",
			Item::new,
			new Item.Properties().pickaxe(ModToolMaterials.REDSTONE, 1.0f, -2.8f).fireResistant()
	);

	public static final Item CURRANT_TINCTURE = register(
			"currant_tincture",
			CurrantTinctureItem::new,
			new Item.Properties()
					.stacksTo(1)
					.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
					.usingConvertsTo(Items.GLASS_BOTTLE)
	);
	public static final Item GOSHAS_RAGE_BOTTLE = register(
			"goshas_rage_bottle",
			GoshasRageBottleItem::new,
			new Item.Properties()
					.stacksTo(16)
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

	public static final Item STRANGE_CHEST = block("strange_chest", ModBlocks.STRANGE_CHEST);

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

	public static final Item MAD_LIBERAL_SPAWN_EGG = register(
			"mad_liberal_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.MAD_LIBERAL)
	);

	public static final Item KIRILL_DOOM_SPAWN_EGG = register(
			"kirill_doom_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.KIRILL_DOOM)
	);

	public static final Item NURSE_SPAWN_EGG = register(
			"nurse_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.NURSE)
	);

	public static final Item ANTON_SPAWN_EGG = register(
			"anton_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.ANTON));

	public static final Item DENIS_SPAWN_EGG = register(
			"denis_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.DENIS));

	public static final Item GOSHA_SPAWN_EGG = register(
			"gosha_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.GOSHA));

	public static final Item GRISHA_SPAWN_EGG = register(
			"grisha_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.GRISHA));

	public static final Item LESHA_SPAWN_EGG = register(
			"lesha_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.LESHA));

	public static final Item SASHA_SPAWN_EGG = register(
			"sasha_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.SASHA));

	public static final Item VLAD_SPAWN_EGG = register(
			"vlad_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.VLAD));

	public static final Item LEX_SPAWN_EGG = register(
			"lex_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.LEX));

	public static final Item PINK_FURRY_WOLF_SPAWN_EGG = register(
			"pink_furry_wolf_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.PINK_FURRY_WOLF));

	public static final Item CATGIRL_SPAWN_EGG = register(
			"catgirl_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntityTypes.CATGIRL));

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, HbkMod.id(name));
		T item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	private static Item block(String name, net.minecraft.world.level.block.Block block) {
		return register(name, properties -> new BlockItem(block, properties),
				new Item.Properties().useBlockDescriptionPrefix());
	}

	private static Item improvedBoat(String name, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.vehicle.boat.AbstractBoat> type) {
		return register(name, properties -> new BoatItem(type, properties), new Item.Properties().stacksTo(1));
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

	private static ItemStack createHbkCreativeTabIcon() {
		ItemStack icon = new ItemStack(Items.PAPER);
		icon.set(DataComponents.ITEM_MODEL, HbkMod.id("hbk_logo"));
		return icon;
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, HBK_CREATIVE_TAB_KEY,
				FabricCreativeModeTab.builder()
						.title(Component.translatable("itemGroup.hbk"))
						.icon(ModItems::createHbkCreativeTabIcon)
						.displayItems((parameters, output) -> BuiltInRegistries.ITEM.stream()
								.filter(item -> HbkMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()))
								.forEach(item -> output.accept(item)))
						.build());

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
			output.accept(SPERM);
			output.accept(BLOODY_SPERM);
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
			output.accept(MAD_LIBERAL_SPAWN_EGG);
			output.accept(KIRILL_DOOM_SPAWN_EGG);
			output.accept(NURSE_SPAWN_EGG);
			output.accept(ANTON_SPAWN_EGG);
			output.accept(DENIS_SPAWN_EGG);
			output.accept(GOSHA_SPAWN_EGG);
			output.accept(GRISHA_SPAWN_EGG);
			output.accept(LESHA_SPAWN_EGG);
			output.accept(SASHA_SPAWN_EGG);
			output.accept(VLAD_SPAWN_EGG);
			output.accept(LEX_SPAWN_EGG);
			output.accept(PINK_FURRY_WOLF_SPAWN_EGG);
			output.accept(CATGIRL_SPAWN_EGG);
		});

		ResourceKey<CreativeModeTab> foodAndDrinks = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.withDefaultNamespace("food_and_drinks")
		);
		CreativeModeTabEvents.modifyOutputEvent(foodAndDrinks).register(output -> {
			output.accept(BUCKWHEAT);
			output.accept(CURRANT_TINCTURE);
			output.accept(GOSHAS_RAGE_BOTTLE);
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
			output.accept(IMPROVED_OAK_BOAT);
			output.accept(IMPROVED_SPRUCE_BOAT);
			output.accept(IMPROVED_BIRCH_BOAT);
			output.accept(IMPROVED_JUNGLE_BOAT);
			output.accept(IMPROVED_ACACIA_BOAT);
			output.accept(IMPROVED_CHERRY_BOAT);
			output.accept(IMPROVED_DARK_OAK_BOAT);
			output.accept(IMPROVED_PALE_OAK_BOAT);
			output.accept(IMPROVED_MANGROVE_BOAT);
			output.accept(IMPROVED_BAMBOO_RAFT);
			output.accept(BALALAIKA_PICKAXE);
			output.accept(MUSIC_DISC_HBKAU);
			output.accept(REDSTONE_PICKAXE);
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
			output.accept(FOUNDING_PENIS);
			output.accept(STRANGE_CHEST);
			output.accept(SICKLE_AND_HAMMER);
			output.accept(BANDAGE);
		});
	}
}
