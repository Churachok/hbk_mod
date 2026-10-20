package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import dev.kirill.hbk.block.entity.JagermeisterCauldronBlockEntity;
import dev.kirill.hbk.item.JagermeisterBottleItem;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.mixin.CauldronDispatcherAccessor;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModBlockEntities;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/** One sealed flower batch yields one seven-sip bottle for each water layer present when it finishes. */
public final class JagermeisterCauldronBlock extends LayeredCauldronBlock implements EntityBlock {
	public static final BooleanProperty READY = BooleanProperty.create("ready");
	public static final MapCodec<LayeredCauldronBlock> CODEC = simpleCodec(JagermeisterCauldronBlock::new);

	public JagermeisterCauldronBlock(BlockBehaviour.Properties properties) {
		super(Biome.Precipitation.RAIN, CauldronInteractions.WATER, properties);
		registerDefaultState(stateDefinition.any().setValue(LEVEL, 1).setValue(READY, false));
	}

	@Override
	public MapCodec<LayeredCauldronBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(READY);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new JagermeisterCauldronBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !ModBlockEntities.JAGERMEISTER_CAULDRON.equals(type)) return null;
		return (tickLevel, tickPos, tickState, entity) -> {
			if (entity instanceof JagermeisterCauldronBlockEntity brew) brew.reconcileReadyState();
		};
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Items.CAULDRON);
	}

	public static void registerInteractions() {
		var dispatcher = (CauldronDispatcherAccessor) (Object) CauldronInteractions.WATER;
		for (var item : JagermeisterIngredients.items()) {
			dispatcher.hbk$put(item, JagermeisterCauldronBlock::addFlower);
		}
	}

	private static InteractionResult addFlower(BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, ItemStack stack) {
		int index = JagermeisterIngredients.indexOf(stack);
		if (player.isSpectator() || index < 0
				|| !(state.is(Blocks.WATER_CAULDRON) || state.is(ModBlocks.JAGERMEISTER_CAULDRON))) {
			return InteractionResult.PASS;
		}
		if (state.is(ModBlocks.JAGERMEISTER_CAULDRON)) {
			if (!(level.getBlockEntity(pos) instanceof JagermeisterCauldronBlockEntity brew)) return InteractionResult.FAIL;
			if ((brew.getIngredientsMask() & JagermeisterIngredients.ingredientBit(index)) != 0) {
				if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.hbk.jagermeister.duplicate"));
				return InteractionResult.FAIL;
			}
		}
		if (!level.isClientSide()) {
			if (state.is(Blocks.WATER_CAULDRON)) {
				level.setBlockAndUpdate(pos, ModBlocks.JAGERMEISTER_CAULDRON.defaultBlockState()
						.setValue(LEVEL, state.getValue(LEVEL)));
			}
			if (!(level.getBlockEntity(pos) instanceof JagermeisterCauldronBlockEntity brew) || !brew.addIngredient(index)) {
				if (state.is(Blocks.WATER_CAULDRON)) level.setBlockAndUpdate(pos, state);
				return InteractionResult.FAIL;
			}
			var ingredient = stack.getItem();
			if (!player.hasInfiniteMaterials()) stack.shrink(1);
			player.getInventory().setChanged();
			player.awardStat(Stats.ITEM_USED.get(ingredient));
			player.awardStat(Stats.USE_CAULDRON);
			level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7f, 0.8f);
			level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator()) return InteractionResult.PASS;
		if (stack.is(ModItems.EMPTY_JAGERMEISTER_BOTTLE)) return fillBottle(state, level, pos, player, hand, stack);
		if (state.getValue(READY) && (stack.is(Items.WATER_BUCKET) || isWaterPotion(stack))) {
			return InteractionResult.FAIL;
		}
		if (stack.is(Items.WATER_BUCKET)) {
			if (state.getValue(LEVEL) == MAX_FILL_LEVEL) return InteractionResult.FAIL;
			// Keep the same block/BE: vanilla's water-bucket interaction would discard all flowers.
			return CauldronInteractions.emptyBucket(level, pos, player, hand, stack,
					state.setValue(LEVEL, MAX_FILL_LEVEL), SoundEvents.BUCKET_EMPTY);
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
		if (!state.getValue(READY)) super.handlePrecipitation(state, level, pos, precipitation);
	}

	@Override
	protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
		if (!state.getValue(READY)) super.receiveStalactiteDrip(state, level, pos, fluid);
	}

	private static boolean isWaterPotion(ItemStack stack) {
		var contents = stack.get(DataComponents.POTION_CONTENTS);
		return stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER);
	}

	public static InteractionResult tryFillBottle(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || player.isSpectator()) return InteractionResult.PASS;
		return fillBottle(context.getLevel().getBlockState(context.getClickedPos()), context.getLevel(),
				context.getClickedPos(), player, context.getHand(), context.getItemInHand());
	}

	private static InteractionResult fillBottle(BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, ItemStack stack) {
		if (!state.is(ModBlocks.JAGERMEISTER_CAULDRON) || !stack.is(ModItems.EMPTY_JAGERMEISTER_BOTTLE)) {
			return InteractionResult.PASS;
		}
		if (!(level.getBlockEntity(pos) instanceof JagermeisterCauldronBlockEntity brew)
				|| !state.getValue(READY) || !JagermeisterIngredients.isComplete(brew.getIngredientsMask())) {
			if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.hbk.jagermeister.not_ready"));
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			var sourceItem = stack.getItem();
			ItemStack filled = stack.transmuteCopy(ModItems.JAGERMEISTER_BOTTLE, 1);
			filled.set(ModDataComponents.JAGERMEISTER_SIPS, JagermeisterBottleItem.MAX_SIPS);
			filled.set(DataComponents.MAX_STACK_SIZE, ModItems.JAGERMEISTER_BOTTLE.getDefaultMaxStackSize());
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
			lowerFillLevel(state, level, pos);
			player.awardStat(Stats.ITEM_USED.get(sourceItem));
			player.awardStat(Stats.USE_CAULDRON);
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
