package dev.kirill.hbk.block.entity;

import dev.kirill.hbk.block.JagermeisterCauldronBlock;
import dev.kirill.hbk.mechanic.JagermeisterIngredients;
import dev.kirill.hbk.registry.ModBlockEntities;
import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class JagermeisterCauldronBlockEntity extends BlockEntity {
	private int ingredientsMask;

	public JagermeisterCauldronBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.JAGERMEISTER_CAULDRON, pos, state);
	}

	public int getIngredientsMask() {
		return ingredientsMask;
	}

	public boolean addIngredient(int index) {
		if (index < 0 || index >= JagermeisterIngredients.TOTAL_INGREDIENTS
				|| (ingredientsMask & JagermeisterIngredients.ingredientBit(index)) != 0) return false;
		ingredientsMask |= JagermeisterIngredients.ingredientBit(index);
		setChanged();
		if (level != null && !level.isClientSide()) {
			BlockState state = level.getBlockState(worldPosition);
			if (state.is(ModBlocks.JAGERMEISTER_CAULDRON)) {
				BlockState updated = state.setValue(JagermeisterCauldronBlock.READY,
						JagermeisterIngredients.isComplete(ingredientsMask));
				if (!updated.equals(state)) level.setBlockAndUpdate(worldPosition, updated);
				// Ingredient updates must send the BE packet even while READY stays false.
				level.sendBlockUpdated(worldPosition, state, updated, Block.UPDATE_CLIENTS);
			}
		}
		return true;
	}

	/** Previously saved batches may now be complete without the removed flower. */
	public void reconcileReadyState() {
		if (level == null || level.isClientSide()) return;
		BlockState state = level.getBlockState(worldPosition);
		boolean ready = JagermeisterIngredients.isComplete(ingredientsMask);
		if (state.is(ModBlocks.JAGERMEISTER_CAULDRON) && state.getValue(JagermeisterCauldronBlock.READY) != ready) {
			level.setBlockAndUpdate(worldPosition, state.setValue(JagermeisterCauldronBlock.READY, ready));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("ingredients_mask", ingredientsMask);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ingredientsMask = input.getIntOr("ingredients_mask", 0) & JagermeisterIngredients.ALL_MASK;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
