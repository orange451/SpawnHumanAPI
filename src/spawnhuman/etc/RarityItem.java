package spawnhuman.etc;

import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.inventory.ItemStack;

public class RarityItem {
	private ItemStack stack;
	private int rarity;
	
	/**
	 * Constructor for Rarity Item. This class lets you define an itemstack with a rarity amount.
	 * See {@link #pick(RarityItem[])} for information on how to select Rarity Items.
	 * @param itemStack
	 * @param rarity
	 */
	public RarityItem(ItemStack itemStack, int rarity) {
		this.stack = itemStack;
		this.rarity = rarity;
	}
	
	public ItemStack getItemStack() {
		return this.stack;
	}

	public int getRarity() {
		return this.rarity;
	}
	
	/**
	 * Randomly select an ItemStack from an array of RarityItems.
	 * @param items
	 * @return
	 */
	public static ItemStack pick(RarityItem[] items) {
		return pick( items, false );
	}
	
	/**
	 * Randomly select an ItemStack from an array of Rarity Items.
	 * If RandomDurability is true, the returned item will have a random durability percentage.
	 * @param items
	 * @param randomDurability
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static ItemStack pick(RarityItem[] items, boolean randomDurability) {
		// Get list of items
		ArrayList<ItemStack> t = new ArrayList<ItemStack>();
		for (int i = 0; i < items.length; i++) {
			RarityItem rarityItem = items[i];
			ItemStack original = rarityItem.getItemStack();
			
			int r = Math.max(rarityItem.rarity, 1);
			for (int j = 0; j < r; j++) {
				ItemStack newItem = original.clone();
				if ( randomDurability ) {
					newItem.setDurability((short) (Math.random()*original.getType().getMaxDurability()));
				}
				
				t.add(newItem);
			}
		}
		
		// Randomly sort list
		Collections.shuffle(t);
		
		// Return random item
		return t.get((int) (Math.random() * t.size()));
	}
}
