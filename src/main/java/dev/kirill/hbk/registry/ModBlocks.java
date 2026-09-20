package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.block.StrangeChestBlock;
import dev.kirill.hbk.block.ToxicWaterBlock;
import dev.kirill.hbk.block.WastelandPlantBlock;
import dev.kirill.hbk.block.BloodySpermBlock;
import dev.kirill.hbk.block.SpermBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.ColorRGBA;

public final class ModBlocks {
	public static final Block INFECTED_DIRT = simple("infected_dirt", MapColor.DIRT, 0.6f, SoundType.GRAVEL);
	public static final Block RADIOACTIVE_STONE = simple("radioactive_stone", MapColor.STONE, 1.7f, SoundType.STONE);
	public static final Block ASH_SOIL = simple("ash_soil", MapColor.COLOR_GRAY, 0.5f, SoundType.SOUL_SOIL);
	public static final Block CRACKED_RADIOACTIVE_STONE = simple("cracked_radioactive_stone", MapColor.STONE, 1.4f, SoundType.STONE);
	public static final Block URANIUM_ORE = register("uranium_ore", properties -> new Block(properties
			.mapColor(MapColor.COLOR_LIGHT_GREEN).strength(3.5f).requiresCorrectToolForDrops()
			.sound(SoundType.STONE).lightLevel(state -> 7)));
	public static final Block SEDIMENT_MUD = simple("sediment_mud", MapColor.TERRACOTTA_BROWN, 0.5f, SoundType.MUD);
	public static final Block TOXIC_WATER = register("toxic_water", properties -> new ToxicWaterBlock(properties
			.mapColor(MapColor.COLOR_GREEN).noCollision().noOcclusion().liquid().replaceable()
			.strength(100.0f).sound(SoundType.EMPTY).lightLevel(state -> 3)));
	public static final Block SCORCHED_STONE = simple("scorched_stone", MapColor.COLOR_BLACK, 2.0f, SoundType.BASALT);
	public static final Block RADIOACTIVE_SAND = register("radioactive_sand", properties -> new ColoredFallingBlock(
			new ColorRGBA(0x9B8E59FF), properties.mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND)));
	public static final Block DEAD_GRASS = plant("dead_grass", false);
	public static final Block INFECTED_BUSH = plant("infected_bush", false);
	public static final Block GLOWING_MUSHROOM = plant("glowing_mushroom", true);
	public static final Block MOLDY_MOSS = register("moldy_moss", properties -> new CarpetBlock(properties
			.mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.MOSS_CARPET).noOcclusion()));
	public static final Block DRY_LOG = register("dry_log", properties -> new RotatedPillarBlock(properties
			.mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));

	public static final Block BRIGHT_GRASS = simple("bright_grass", MapColor.COLOR_LIGHT_GREEN, 0.6f, SoundType.GRASS);
	public static final Block ROTTEN_EARTH = simple("rotten_earth", MapColor.DIRT, 0.55f, SoundType.ROOTED_DIRT);
	public static final Block SPERM = register("sperm",
			properties -> new SpermBlock(properties.mapColor(MapColor.SNOW).noCollision().noOcclusion()
					.liquid().replaceable().strength(100.0f).sound(SoundType.SLIME_BLOCK)));
	public static final Block BLOODY_SPERM = register("bloody_sperm",
			properties -> new BloodySpermBlock(properties.mapColor(MapColor.COLOR_RED).noCollision().noOcclusion()
					.liquid().replaceable().strength(100.0f).sound(SoundType.HONEY_BLOCK)));
	public static final Block RUINED_CONCRETE = simple("ruined_concrete", MapColor.STONE, 1.8f, SoundType.STONE);
	public static final Block BUILDING_DEBRIS = simple("building_debris", MapColor.COLOR_BLACK, 1.35f, SoundType.TUFF);
	public static final Block CRIMSON_MONUMENT = simple("crimson_monument", MapColor.COLOR_RED, 2.2f, SoundType.NETHER_BRICKS);

	public static final Block STRANGE_CHEST = register("strange_chest", properties -> new StrangeChestBlock(properties
			.mapColor(MapColor.WOOD)
			.strength(2.5f)
			.sound(SoundType.WOOD)
			.noOcclusion()));

	private ModBlocks() {
	}

	private static Block simple(String name, MapColor color, float strength, SoundType sound) {
		return register(name, properties -> new Block(properties.mapColor(color).strength(strength).sound(sound)));
	}

	private static Block plant(String name, boolean glowing) {
		return register(name, properties -> new WastelandPlantBlock(properties
				.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS)
				.lightLevel(state -> glowing ? 10 : 0).offsetType(BlockBehaviour.OffsetType.XZ)));
	}

	private static Block register(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, HbkMod.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.of().setId(key)));
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered blocks for {}", HbkMod.MOD_ID);
	}
}
