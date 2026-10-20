package dev.kirill.hbk.universe;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.function.Function;

/** Ten finite content sets shared by all addresses; no registration when a dimension is loaded. */
public final class UniverseBotany {
	public static final List<String> SPECIES = List.of("foldwood", "sailwood", "chordwood", "lanternwood", "knotwood",
			"siltwood", "loopwood", "shardwood", "pendantwood", "terracewood");
	public static final List<String> ROLES = List.of("log", "stripped_log", "planks", "stairs", "slab", "fence", "leaves", "soil", "plant", "sapling", "lamp");
	private static final Block[][] BLOCKS = new Block[10][ROLES.size()];
	private static final Item[][] CROPS = new Item[10][2];
	private UniverseBotany() {}
	public static Block block(int family, String role) { return BLOCKS[family][ROLES.indexOf(role)]; }
	public static Item fruit(int family) { return CROPS[family][0]; }
	public static Item ration(int family) { return CROPS[family][1]; }
	public static int family(Block block) {
		for (int f = 0; f < 10; f++) for (Block candidate : BLOCKS[f]) if (candidate == block) return f;
		return -1;
	}
	public static List<MobEffectInstance> benefits(int family) {
		return switch (family) {
			case 0 -> List.of(new MobEffectInstance(MobEffects.RESISTANCE, 800));
			case 1 -> List.of(new MobEffectInstance(MobEffects.SLOW_FALLING, 1200));
			case 2 -> List.of(new MobEffectInstance(MobEffects.JUMP_BOOST, 1200, 1));
			case 3 -> List.of(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600));
			case 4 -> List.of(new MobEffectInstance(MobEffects.HASTE, 1200));
			case 5 -> List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 2400));
			case 6 -> List.of(new MobEffectInstance(MobEffects.SPEED, 1200));
			case 7 -> List.of(new MobEffectInstance(MobEffects.REGENERATION, 200));
			case 8 -> List.of(new MobEffectInstance(MobEffects.LEVITATION, 100), new MobEffectInstance(MobEffects.SLOW_FALLING, 1200));
			default -> List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1800));
		};
	}
	public static void register() {
		for (int f = 0; f < 10; f++) {
			final int family = f;
			add(f, "log", p -> new RotatedPillarBlock(p), wood(Blocks.OAK_LOG, f).lightLevel(s -> family == 3 ? 3 : 0));
			add(f, "stripped_log", p -> new RotatedPillarBlock(p), wood(Blocks.STRIPPED_OAK_LOG, f));
			add(f, "planks", Block::new, wood(Blocks.OAK_PLANKS, f));
			add(f, "stairs", p -> new WoodStairs(block(family, "planks").defaultBlockState(), p), wood(Blocks.OAK_STAIRS, f));
			add(f, "slab", SlabBlock::new, wood(Blocks.OAK_SLAB, f));
			add(f, "fence", FenceBlock::new, wood(Blocks.OAK_FENCE, f));
			add(f, "leaves", p -> new TintedParticleLeavesBlock(0, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
					.lightLevel(s -> family == 3 ? 5 : 0));
			add(f, "soil", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).strength(.65f)
					.friction(f == 2 ? .5f : f == 7 ? .7f : .6f).speedFactor(f == 5 ? .85f : 1).jumpFactor(f == 9 ? 1.12f : 1)
					.lightLevel(s -> family == 3 ? 2 : 0));
			add(f, "plant", p -> new UniverseCropBlock(family, p), BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
					.noCollision().noOcclusion().lightLevel(s -> (family == 3 || family == 8) && s.getValue(UniverseCropBlock.AGE) == 3 ? 9 : 0));
			add(f, "sapling", p -> new UniverseSaplingBlock(family, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));
			add(f, "lamp", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SHROOMLIGHT).strength(1).lightLevel(s -> 14));
			CROPS[f][0] = item(SPECIES.get(f) + "_fruit", new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.25f).build()));
			CROPS[f][1] = ration(f, new Item.Properties().stacksTo(16)
					.food(new FoodProperties.Builder().nutrition(6).saturationModifier(.6f).alwaysEdible().build())
					.component(DataComponents.CONSUMABLE, Consumable.builder().onConsume(new ApplyStatusEffectsConsumeEffect(benefits(f))).build()));
			StrippableBlockRegistry.register(block(f, "log"), block(f, "stripped_log"));
			for (String role : List.of("log", "stripped_log", "planks", "stairs", "slab", "fence")) FlammableBlockRegistry.getDefaultInstance().add(block(f, role), 5, 20);
			FlammableBlockRegistry.getDefaultInstance().add(block(f, "leaves"), 30, 60);
			CompostableRegistry.INSTANCE.add(fruit(f), .65f);
			for (String role : List.of("plant", "leaves", "sapling")) CompostableRegistry.INSTANCE.add(block(f, role).asItem(), role.equals("plant") ? .65f : .3f);
		}
	}
	private static BlockBehaviour.Properties wood(Block source, int family) {
		return BlockBehaviour.Properties.ofFullCopy(source).strength(family == 2 || family == 4 ? 2.8f : 2, 3);
	}
	private static void add(int family, String role, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		String name = SPECIES.get(family) + "_" + role;
		var key = ResourceKey.create(Registries.BLOCK, HbkMod.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		BLOCKS[family][ROLES.indexOf(role)] = block;
		var itemKey = ResourceKey.create(Registries.ITEM, HbkMod.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}
	private static Item item(String name, Item.Properties properties) {
		var key = ResourceKey.create(Registries.ITEM, HbkMod.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key))); return item;
	}
	private static Item ration(int family, Item.Properties properties) {
		var key = ResourceKey.create(Registries.ITEM, HbkMod.id(SPECIES.get(family) + "_ration"));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)) {
			@Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
					java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
				super.appendHoverText(stack, context, display, tooltip, flag);
				tooltip.accept(net.minecraft.network.chat.Component.translatable("ecology.hbk." + SPECIES.get(family)).withStyle(net.minecraft.ChatFormatting.BLUE));
			}
		});
		return item;
	}
	public static List<String> recipes() {
		return SPECIES.stream().flatMap(s -> java.util.stream.Stream.of("planks", "slab", "stairs", "fence", "lamp", "ration", "sapling").map(r -> s + "_" + r)).toList();
	}
	private static final class WoodStairs extends StairBlock {
		private WoodStairs(net.minecraft.world.level.block.state.BlockState base, BlockBehaviour.Properties properties) { super(base, properties); }
	}
}
