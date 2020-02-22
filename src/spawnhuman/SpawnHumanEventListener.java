package spawnhuman;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

public class SpawnHumanEventListener implements Listener {
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		// Check if its an npc
		List<MetadataValue> metadata = event.getEntity().getMetadata("NPC");
		if ( metadata.size() == 0 )
			return;
		
		// Get the npc
		EntityPlayerNPC npc = SpawnHuman.matchNPC(event.getEntity());
		if ( npc == null )
			return;
		
		// Add its inventory into the drop table
		ItemStack[] contents = npc.getInventory().getContents();
		for (int i = 0; i< contents.length; i++) {
			ItemStack item = contents[i];
			if ( item == null || item.getType().equals(Material.AIR) )
				continue;
			
			event.getDrops().add(item);
		}
		
		// Despawn the npc
		Bukkit.getScheduler().scheduleSyncDelayedTask(SpawnHuman.plugin, ()->{
			SpawnHuman.despawnNPC(npc);
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		// Check if its an npc
		List<MetadataValue> metadata = event.getEntity().getMetadata("NPC");
		if ( metadata.size() == 0 )
			return;
		
		// Damager needs to be living
		if ( !(event.getDamager() instanceof LivingEntity) )
			return;
		LivingEntity damager = (LivingEntity) event.getDamager();
		
		// Damaged also needs to be entity
		if ( !(event.getEntity() instanceof LivingEntity) )
			return;
		LivingEntity damaged = (LivingEntity) event.getEntity();
		
		// Get the npc
		EntityPlayerNPC npc = SpawnHuman.matchNPC(damaged);
		if ( npc == null )
			return;
		
		npc.onDamage(damager, event.getFinalDamage());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		//
	}
}
