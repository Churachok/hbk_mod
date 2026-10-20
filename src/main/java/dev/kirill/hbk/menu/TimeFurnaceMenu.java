package dev.kirill.hbk.menu;

import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** The original save action remains separate from travelling to the selected address. */
public final class TimeFurnaceMenu extends AbstractContainerMenu {
	private final TimeFurnaceBlockEntity furnace;
	private final String initialCode;

	public TimeFurnaceMenu(int id, Inventory inventory, String code) {
		super(ModMenuTypes.TIME_FURNACE, id);
		furnace = null;
		initialCode = code;
	}

	public TimeFurnaceMenu(int id, TimeFurnaceBlockEntity furnace) {
		super(ModMenuTypes.TIME_FURNACE, id);
		this.furnace = furnace;
		initialCode = furnace.dimensionCode();
	}

	public String initialCode() { return initialCode; }

	public boolean saveCode(Player player, String code) {
		if (furnace == null || player.isSpectator() || !stillValid(player)
				|| !TimeFurnaceBlockEntity.isValidCode(code)) return false;
		furnace.setDimensionCode(code);
		return true;
	}

	public boolean travel(net.minecraft.server.level.ServerPlayer player, String code, boolean home) {
		if (furnace == null || player.isSpectator() || !stillValid(player)) return false;
		if (home) return dev.kirill.hbk.universe.UniverseManager.returnHome(player);
		return saveCode(player, code) && dev.kirill.hbk.universe.UniverseManager.travel(player,
				new dev.kirill.hbk.universe.UniverseCode(code));
	}

	@Override
	public boolean stillValid(Player player) {
		return furnace == null || (furnace.getLevel() == player.level() && !furnace.isRemoved()
				&& furnace.getLevel().getBlockEntity(furnace.getBlockPos()) == furnace
				&& stillValid(ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()), player, ModBlocks.TIME_FURNACE));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
