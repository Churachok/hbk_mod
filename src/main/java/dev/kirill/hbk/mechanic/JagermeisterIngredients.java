package dev.kirill.hbk.mechanic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** A fixed recipe order also serves as the save format and the client's ingredient checklist. */
public final class JagermeisterIngredients {
	private static final List<Item> ITEMS = List.of(
			Items.GOLDEN_DANDELION, Items.POPPY, Items.BLUE_ORCHID,
			Items.ALLIUM, Items.AZURE_BLUET, Items.RED_TULIP, Items.ORANGE_TULIP,
			Items.WHITE_TULIP, Items.PINK_TULIP, Items.OXEYE_DAISY, Items.CORNFLOWER,
			Items.LILY_OF_THE_VALLEY, Items.WITHER_ROSE, Items.TORCHFLOWER,
			Items.OPEN_EYEBLOSSOM, Items.CACTUS_FLOWER,
			Items.SPORE_BLOSSOM, Items.CHORUS_FLOWER, Items.PINK_PETALS, Items.WILDFLOWERS
	);
	public static final int TOTAL_INGREDIENTS = ITEMS.size();
	// Reserve removed dandelion (bit 0) and closed eyeblossom (bit 16) in the original save format.
	private static final int CLOSED_EYEBLOSSOM_SAVE_INDEX = 16;
	public static final int ALL_MASK = ((1 << (TOTAL_INGREDIENTS + 2)) - 1)
			& ~1 & ~(1 << CLOSED_EYEBLOSSOM_SAVE_INDEX);

	private JagermeisterIngredients() {
	}

	public static List<Item> items() {
		return ITEMS;
	}

	public static int indexOf(ItemStack stack) {
		if (stack.isEmpty()) return -1;
		for (int index = 0; index < ITEMS.size(); index++) {
			if (stack.is(ITEMS.get(index))) return index;
		}
		return -1;
	}

	public static int count(int mask) {
		return Integer.bitCount(mask & ALL_MASK);
	}

	public static int ingredientBit(int index) {
		java.util.Objects.checkIndex(index, TOTAL_INGREDIENTS);
		int savedIndex = index + 1;
		return 1 << (savedIndex >= CLOSED_EYEBLOSSOM_SAVE_INDEX ? savedIndex + 1 : savedIndex);
	}

	public static boolean isComplete(int mask) {
		return (mask & ALL_MASK) == ALL_MASK;
	}
}
