package dev.kirill.hbk.block.entity;

import dev.kirill.hbk.menu.TimeFurnaceMenu;
import dev.kirill.hbk.registry.ModBlockEntities;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class TimeFurnaceBlockEntity extends BlockEntity implements ExtendedMenuProvider<String> {
	private String dimensionCode = "";

	public TimeFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TIME_FURNACE, pos, state);
	}

	public static boolean isValidCode(String code) {
		return dev.kirill.hbk.universe.UniverseCode.valid(code);
	}

	public String dimensionCode() { return dimensionCode; }

	public void setDimensionCode(String code) {
		if (!isValidCode(code)) return;
		dimensionCode = code;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("dimension_code", dimensionCode);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String code = input.getStringOr("dimension_code", "");
		dimensionCode = isValidCode(code) ? code : "";
	}

	@Override
	public Component getDisplayName() { return Component.translatable("block.hbk.time_furnace"); }

	@Override
	public String getScreenOpeningData(ServerPlayer player) { return dimensionCode; }

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new TimeFurnaceMenu(id, this);
	}
}
