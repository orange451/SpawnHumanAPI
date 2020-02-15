package spawnhuman.etc;

import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Multimap;

import net.minecraft.server.v1_15_R1.AttributeModifier;
import net.minecraft.server.v1_15_R1.AttributeModifier.Operation;
import net.minecraft.server.v1_15_R1.EnumItemSlot;
import net.minecraft.server.v1_15_R1.GenericAttributes;

public class DamageUtil {
	@SuppressWarnings("deprecation")
	public static double getBaseDamageFromItem(ItemStack itemStack) {

		try {
			double attackDamage = 0.0;
			net.minecraft.server.v1_15_R1.ItemStack craftItemStack = CraftItemStack.asNMSCopy(itemStack);
			net.minecraft.server.v1_15_R1.Item item = craftItemStack.getItem();
			if(item instanceof net.minecraft.server.v1_15_R1.ItemSword || item instanceof net.minecraft.server.v1_15_R1.ItemTool || item instanceof net.minecraft.server.v1_15_R1.ItemHoe) {
				Multimap<String, AttributeModifier> map = item.a(EnumItemSlot.MAINHAND);
				
				// Get attack damage attributs
				Collection<AttributeModifier> attributes = map.get(GenericAttributes.ATTACK_DAMAGE.getName());
				if(!attributes.isEmpty()) {

					// Addition
					for(AttributeModifier am: attributes) {
						if ( am.getName().equals("Weapon modifier") && am.getOperation().equals(Operation.ADDITION) )
							attackDamage += am.getAmount();
					}

					// Compute base multiplication
					double y = 1;
					for(AttributeModifier am: attributes) {
						if ( am.getName().equals("Weapon modifier") && am.getOperation().equals(Operation.MULTIPLY_BASE) )
							y += am.getAmount();
					}

					// Apply base multiplication
					attackDamage *= y;

					// Total Multiplication
					for(AttributeModifier am: attributes) {
						if ( am.getName().equals("Weapon modifier") && am.getOperation().equals(Operation.MULTIPLY_TOTAL) )
							attackDamage *= (1 + am.getAmount());
					}
				}
			}
			return Math.max(1, attackDamage);
		} catch(Exception e) {
			System.err.println("[SpawnHumanAPI] Failed to get weapon danage for itemstack: " + itemStack + ". Using fallback method.");
			//e.printStackTrace();
		}
		
		// Start fallback method.
		Material material = itemStack.getType();

		// Swords
		if ( material.equals(Material.WOODEN_SWORD) )
			return 3;
		if ( material.equals(Material.GOLDEN_SWORD) )
			return 3;
		if ( material.equals(Material.STONE_SWORD) )
			return 4;
		if ( material.equals(Material.IRON_SWORD) )
			return 5;
		if ( material.equals(Material.DIAMOND_SWORD) )
			return 6;


		// Swords
		if ( material.equals(Material.LEGACY_WOOD_SWORD) )
			return 3;
		if ( material.equals(Material.LEGACY_GOLD_SWORD) )
			return 3;
		if ( material.equals(Material.LEGACY_STONE_SWORD) )
			return 4;
		if ( material.equals(Material.LEGACY_IRON_SWORD) )
			return 5;
		if ( material.equals(Material.LEGACY_DIAMOND_SWORD) )
			return 6;

		// Axes
		if ( material.equals(Material.WOODEN_AXE) )
			return 4;
		if ( material.equals(Material.GOLDEN_AXE) )
			return 4;
		if ( material.equals(Material.STONE_AXE) )
			return 5;
		if ( material.equals(Material.IRON_AXE) )
			return 6;
		if ( material.equals(Material.DIAMOND_AXE) )
			return 7;

		// Axes
		if ( material.equals(Material.LEGACY_WOOD_AXE) )
			return 4;
		if ( material.equals(Material.LEGACY_GOLD_AXE) )
			return 4;
		if ( material.equals(Material.LEGACY_STONE_AXE) )
			return 5;
		if ( material.equals(Material.LEGACY_IRON_AXE) )
			return 6;
		if ( material.equals(Material.LEGACY_DIAMOND_AXE) )
			return 7;

		// Pickaxes
		if ( material.equals(Material.WOODEN_PICKAXE) )
			return 2;
		if ( material.equals(Material.GOLDEN_PICKAXE) )
			return 2;
		if ( material.equals(Material.STONE_PICKAXE) )
			return 3;
		if ( material.equals(Material.IRON_PICKAXE) )
			return 4;
		if ( material.equals(Material.DIAMOND_PICKAXE) )
			return 5;


		// Pickaxes
		if ( material.equals(Material.LEGACY_WOOD_PICKAXE) )
			return 2;
		if ( material.equals(Material.LEGACY_GOLD_PICKAXE) )
			return 2;
		if ( material.equals(Material.LEGACY_STONE_PICKAXE) )
			return 3;
		if ( material.equals(Material.LEGACY_IRON_PICKAXE) )
			return 4;
		if ( material.equals(Material.LEGACY_DIAMOND_PICKAXE) )
			return 5;

		return 1;
	}
}
