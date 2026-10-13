package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import dev.kirill.hbk.item.BeerBottleItem;
import dev.kirill.hbk.mixin.CauldronDispatcherAccessor;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModDataComponents;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Water and wheat are block-state data, so brewing survives saving, synchronization and chunk unloading. */
public final class BeerCauldronBlock extends LayeredCauldronBlock {
	public static final IntegerProperty WHEAT = IntegerProperty.create("wheat", 1, 3);
	public static final MapCodec<LayeredCauldronBlock> CODEC = simpleCodec(BeerCauldronBlock::new);

	public BeerCauldronBlock(BlockBehaviour.Properties properties) {
		super(Biome.Precipitation.RAIN, CauldronInteractions.WATER, properties);
		registerDefaultState(stateDefinition.any().setValue(LEVEL, 1).setValue(WHEAT, 1));
	}

	@Override
	public MapCodec<LayeredCauldronBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WHEAT);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Items.CAULDRON);
	}

	public static void registerInteractions() {
		((CauldronDispatcherAccessor) (Object) CauldronInteractions.WATER)
				.hbk$put(Items.WHEAT, BeerCauldronBlock::addWheat);
	}

	public static boolean isReady(BlockState state) {
		return state.is(ModBlocks.BEER_CAULDRON) && state.getValue(WHEAT) >= state.getValue(LEVEL);
	}

	private static InteractionResult addWheat(BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, ItemStack stack) {
		if (player.isSpectator() || !stack.is(Items.WHEAT)
				|| !(state.is(Blocks.WATER_CAULDRON) || state.is(ModBlocks.BEER_CAULDRON))) {
			return InteractionResult.PASS;
		}
		int wheat = state.is(ModBlocks.BEER_CAULDRON) ? state.getValue(WHEAT) : 0;
		int layers = state.getValue(LEVEL);
		if (wheat >= layers) return InteractionResult.FAIL;
		if (!level.isClientSide()) {
			if (!player.hasInfiniteMaterials()) stack.shrink(1);
			player.getInventory().setChanged();
			level.setBlockAndUpdate(pos, ModBlocks.BEER_CAULDRON.defaultBlockState()
					.setValue(LEVEL, layers).setValue(WHEAT, wheat + 1));
			player.awardStat(Stats.USE_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(Items.WHEAT));
			level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7f, 0.8f);
			level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator()) return InteractionResult.PASS;
		if (stack.is(ModItems.EMPTY_BEER_BOTTLE) || stack.is(ModItems.BEER_BOTTLE)) {
			return fillBottle(state, level, pos, player, hand, stack);
		}
		if (stack.is(ModItems.BROKEN_BEER_BOTTLE)) return InteractionResult.FAIL;
		if (stack.is(Items.WATER_BUCKET)) {
			if (state.getValue(LEVEL) == MAX_FILL_LEVEL) return InteractionResult.FAIL;
			// Vanilla replaces the whole block with pure water; here the existing grain must stay.
			return CauldronInteractions.emptyBucket(level, pos, player, hand, stack,
					state.setValue(LEVEL, MAX_FILL_LEVEL), SoundEvents.BUCKET_EMPTY);
		}
		// Water bottles, ordinary buckets, washing, rain and drips keep native cauldron behavior.
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	public static InteractionResult tryFillBottle(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || player.isSpectator()) return InteractionResult.PASS;
		return fillBottle(context.getLevel().getBlockState(context.getClickedPos()), context.getLevel(),
				context.getClickedPos(), player, context.getHand(), context.getItemInHand());
	}

	private static InteractionResult fillBottle(BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, ItemStack stack) {
		if (!state.is(ModBlocks.BEER_CAULDRON)
				|| !(stack.is(ModItems.EMPTY_BEER_BOTTLE) || stack.is(ModItems.BEER_BOTTLE))) {
			return InteractionResult.PASS;
		}
		int sips = BeerBottleItem.getSips(stack);
		if (sips >= BeerBottleItem.MAX_SIPS) return InteractionResult.PASS;
		if (!isReady(state)) {
			if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.hbk.beer_not_ready"));
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			int taken = Math.min(state.getValue(LEVEL), BeerBottleItem.MAX_SIPS - sips);
			int filledSips = sips + taken;
			ItemStack filled = stack.transmuteCopy(ModItems.BEER_BOTTLE, 1);
			filled.set(ModDataComponents.BEER_SIPS, filledSips);
			filled.set(DataComponents.MAX_STACK_SIZE, filledSips == BeerBottleItem.MAX_SIPS
					? ModItems.BEER_BOTTLE.getDefaultMaxStackSize() : 1);
			player.setItemInHand(hand, filled);
			int remaining = state.getValue(LEVEL) - taken;
			level.setBlockAndUpdate(pos, remaining == 0 ? Blocks.CAULDRON.defaultBlockState()
					: state.setValue(LEVEL, remaining).setValue(WHEAT, state.getValue(WHEAT) - taken));
			player.awardStat(Stats.USE_CAULDRON);
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
