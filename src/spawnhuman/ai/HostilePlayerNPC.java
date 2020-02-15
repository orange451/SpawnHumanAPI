package spawnhuman.ai;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import spawnhuman.EntityPlayerNPC;
import spawnhuman.SpawnHuman;

public class HostilePlayerNPC extends NormalPlayerNPC {
	public boolean canAttackSameNPCs = true;
	public boolean canAttackPassiveMobs = true;
	
	public HostilePlayerNPC(String name, Location location) {
		super(name, location);
	}

	@Override
	public void step() {
		super.step();
		
		if ( !isSpawned() )
			return;
		
		// Handle targeting
		if ( target == null ) {
			List<LivingEntity> nearby = getNearbyLivingEntities(16);
			
			// New Target
			if ( nearby.size() > 0 ) {
				for (int i = 0; i<nearby.size(); i++) {
					LivingEntity temp = nearby.get(i);
					if ( temp instanceof Creeper )
						continue;
					
					if ( !this.canSee(temp.getLocation()) )
						continue;
					
					boolean isMonster = temp instanceof Monster;
					boolean isPlayer = temp instanceof Player;
					
					// Only attack monsters and players
					if ( !canAttackPassiveMobs && (!isMonster && !isPlayer) )
						continue;
					
					// Ignore same NPC type
					if ( !canAttackSameNPCs && temp.getMetadata("NPC").size() > 0 ) {
						EntityPlayerNPC npc = SpawnHuman.matchNPC(temp);
						if ( npc == null )
							continue;
						
						if ( npc.getClass().equals(this.getClass()) || npc.getClass().isAssignableFrom(this.getClass()) ) {
							continue;
						}
					}
					
					target = temp;
					break;
				}
			}
		}
	}
}
