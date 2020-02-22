package spawnhuman.etc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Multimap;

import net.minecraft.server.v1_15_R1.AttributeModifier;
import net.minecraft.server.v1_15_R1.AttributeModifier.Operation;
import net.minecraft.server.v1_15_R1.EnumItemSlot;
import net.minecraft.server.v1_15_R1.GenericAttributes;

public class DamageUtil {
	private final static Map<Material, Integer> WEAPON_DAMAGE = new HashMap<Material, Integer>() {
		private static final long serialVersionUID = 1L;
		{
			this.put(Material.WOODEN_SWORD, 3);
			this.put(Material.GOLDEN_SWORD, 3);
			this.put(Material.STONE_SWORD, 4);
			this.put(Material.IRON_SWORD, 5);
			this.put(Material.DIAMOND_SWORD, 6);

			this.put(Material.WOODEN_AXE, 4);
			this.put(Material.GOLDEN_AXE, 4);
			this.put(Material.STONE_AXE, 5);
			this.put(Material.IRON_AXE, 6);
			this.put(Material.DIAMOND_AXE, 7);

			this.put(Material.WOODEN_PICKAXE, 2);
			this.put(Material.GOLDEN_PICKAXE, 2);
			this.put(Material.STONE_PICKAXE, 3);
			this.put(Material.IRON_PICKAXE, 4);
			this.put(Material.DIAMOND_PICKAXE, 5);

			this.put(Material.WOODEN_SHOVEL, 2);
			this.put(Material.GOLDEN_SHOVEL, 2);
			this.put(Material.STONE_SHOVEL, 3);
			this.put(Material.IRON_SHOVEL, 4);
			this.put(Material.DIAMOND_SHOVEL, 5);

			this.put(Material.WOODEN_HOE, 2);
			this.put(Material.GOLDEN_HOE, 2);
			this.put(Material.STONE_HOE, 3);
			this.put(Material.IRON_HOE, 4);
			this.put(Material.DIAMOND_HOE, 5);
		}
	};
	
	private final static String CONST_WEAPON_MODIFIER = "Weapon modifier";
	
	public static double getBaseDamageFromItem(ItemStack itemStack) {

		try {
			double attackDamage = 0.0;
			net.minecraft.server.v1_15_R1.ItemStack craftItemStack = CraftItemStack.asNMSCopy(itemStack);
			net.minecraft.server.v1_15_R1.Item item = craftItemStack.getItem();
			
			if ( WEAPON_DAMAGE.containsKey(itemStack.getType()) ) {
				Multimap<String, AttributeModifier> map = item.a(EnumItemSlot.MAINHAND);
				
				// Get attack damage attributs
				Collection<AttributeModifier> attributes = map.get(GenericAttributes.ATTACK_DAMAGE.getName());
				if(!attributes.isEmpty()) {
	
					// Addition
					for(AttributeModifier am: attributes) {
						if ( am.getName().equals(CONST_WEAPON_MODIFIER) && am.getOperation().equals(Operation.ADDITION) )
							attackDamage += am.getAmount();
					}
	
					// Compute base multiplication
					double y = 1;
					for(AttributeModifier am: attributes) {
						if ( am.getName().equals(CONST_WEAPON_MODIFIER) && am.getOperation().equals(Operation.MULTIPLY_BASE) )
							y += am.getAmount();
					}
	
					// Apply base multiplication
					attackDamage *= y;
	
					// Total Multiplication
					for(AttributeModifier am: attributes) {
						if ( am.getName().equals(CONST_WEAPON_MODIFIER) && am.getOperation().equals(Operation.MULTIPLY_TOTAL) )
							attackDamage *= (1 + am.getAmount());
					}
				}
				return Math.max(1, attackDamage);
			}
		} catch(Exception e) {
			System.err.println("[SpawnHumanAPI] Failed to get weapon damage for itemstack: " + itemStack + ". Using fallback method.");
		}
		
		// Start fallback method.
		Material material = itemStack.getType();
		if ( WEAPON_DAMAGE.containsKey(material) )
			return WEAPON_DAMAGE.get(material);

		// Minimum damage is 1
		return 1;
	}
}
