package dev.kirill.hbk.universe;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.List;

public final class UniverseMaterials {
	public static final List<String> FAMILIES = List.of("primordial", "celestial", "columnar", "cavernous", "woven",
			"sedimentary", "labyrinthine", "fragmented", "inverse", "stratified");
	public static final List<String> ROLES = List.of("stone", "surface", "deep", "formation", "ore");
	public static final List<String> RESOURCES = List.of("mineral", "shard", "component");
	private static final Block[][] BLOCKS = new Block[10][5];
	private static final Block[] LOAM = new Block[10];
	private static final Item[][] ITEMS = new Item[10][3];
	public static Block NATURAL_WATER;
	private UniverseMaterials() {}
	public static void register() {
		var waterKey = ResourceKey.create(Registries.BLOCK, HbkMod.id("universe_water"));
		NATURAL_WATER = Registry.register(BuiltInRegistries.BLOCK, waterKey, new UniverseWaterBlock(BlockBehaviour.Properties.of()
				.setId(waterKey).mapColor(MapColor.WATER).replaceable().noCollision().strength(100).noLootTable().liquid()
				.sound(SoundType.EMPTY)));
		for (int f = 0; f < 10; f++) {
			var loamKey = ResourceKey.create(Registries.BLOCK, HbkMod.id(FAMILIES.get(f) + "_loam"));
			LOAM[f] = Registry.register(BuiltInRegistries.BLOCK, loamKey, new Block(BlockBehaviour.Properties.of()
					.setId(loamKey).mapColor(MapColor.DIRT).strength(.65f).sound(SoundType.ROOTED_DIRT)));
			var loamItemKey = ResourceKey.create(Registries.ITEM, loamKey.identifier());
			Registry.register(BuiltInRegistries.ITEM, loamItemKey, new BlockItem(LOAM[f], new Item.Properties().setId(loamItemKey).useBlockDescriptionPrefix()));
			for (int r = 0; r < 5; r++) {
				String name = FAMILIES.get(f) + "_" + ROLES.get(r);
				var key = ResourceKey.create(Registries.BLOCK, HbkMod.id(name));
				final int light = r == 3 ? (f == 1 ? 12 : f == 7 || f == 9 ? 7 : 3) : 0;
				var properties = BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE)
						.strength(r == 1 ? .8f + f * .08f : r == 2 ? 3.8f + f * .15f : 1.6f + f * .2f, 6)
						.sound(f == 7 || r == 3 ? SoundType.AMETHYST : f == 2 ? SoundType.BASALT : SoundType.STONE)
						.lightLevel(state -> light);
				if (r != 1) properties.requiresCorrectToolForDrops();
				Block block = Registry.register(BuiltInRegistries.BLOCK, key, new UniverseRockBlock(properties));
				BLOCKS[f][r] = block;
				var itemKey = ResourceKey.create(Registries.ITEM, HbkMod.id(name));
				Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
			}
			for (int r = 0; r < 3; r++) {
				var key = ResourceKey.create(Registries.ITEM, HbkMod.id(FAMILIES.get(f) + "_" + RESOURCES.get(r)));
				ITEMS[f][r] = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)
						.rarity(r == 2 ? Rarity.RARE : Rarity.COMMON)));
			}
		}
	}
	public static Block block(int family, int role) { return BLOCKS[family][role]; }
	public static Block loam(int family) { return LOAM[family]; }
	public static Item resource(int family, int role) { return ITEMS[family][role]; }
	public static BlockState state(int family, int role, int variant) {
		return block(family, role).defaultBlockState().setValue(UniverseRockBlock.VARIANT, variant);
	}
	public static List<String> recipes() {
		return FAMILIES.stream().flatMap(f -> java.util.stream.Stream.of(f + "_mineral", f + "_formation")).toList();
	}
}
