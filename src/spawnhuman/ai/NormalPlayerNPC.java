package spawnhuman.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import spawnhuman.EntityPlayerNPC;

public class NormalPlayerNPC extends EntityPlayerNPC {
	protected LivingEntity target;
	protected int ATTACK_TIMEOUT = 20;
	protected int RETARGET_TIMEOUT = 20;
	protected int ROAM_TIMEOUT = 20;
	protected int RECHECK_TARGET = 20;
	protected int TARGET_FAILED = 0;
	protected int NO_NEARBY_PLAYER_TICKS = 0;
	
	protected float WALK_SPEED = 1.25f;
	protected float RUN_SPEED = 2.0f;

	protected static final int TARGET_TIMEOUT_DISTANCE = 34;
	protected static final int TARGET_MAX_FAIL = 5;
	protected static final int PATHFIND_TIMEOUT = 256;
	protected static final double MINIMUM_TARGET_DISTANCE = 1.5;
	
	protected boolean hostileIfAttacked = true;
	
	public NormalPlayerNPC(String name, Location location) {
		super(name, location);
		
		this.getNavigator().getLocalParameters().useNewPathfinder(true);
		this.getNavigator().getLocalParameters().avoidWater(false);
		this.getNavigator().getLocalParameters().distanceMargin(3.0);
		this.getNavigator().getLocalParameters().range(200);
		this.getNavigator().getLocalParameters().stuckAction(new StuckAction() {
			@Override
			public boolean run(NPC arg0, Navigator arg1) {
				arg1.cancelNavigation();
				setTarget(null);
				return false;
			}
		});
	}
	
	public void setTarget( LivingEntity target ) {
		
		// Ugly fix for invis players.
		/*if ( this.TICKS_SINCE_LAST_ATTACK > 20 * 5 ) {
			this.spawn(this.getLocation());
		}*/
		
		// Set target
		this.target = target;
		this.RECHECK_TARGET = 10;
		this.TARGET_FAILED = 0;
		this.TICKS_SINCE_LAST_ATTACK = 0;
		this.TICKS = 0;
		getNavigator().cancelNavigation();
	}

	@Override
	public void step() {
		super.step();
		
		if ( !isSpawned() )
			return;
		
		ATTACK_TIMEOUT--;
		RETARGET_TIMEOUT--;
		ROAM_TIMEOUT--;
		RECHECK_TARGET--;
		TICKS_SINCE_LAST_ATTACK++;

		this.getNavigator().getLocalParameters().baseSpeed(this.getPlayer().isSprinting()?RUN_SPEED:WALK_SPEED);
		
		// Handle targeting
		if ( target != null ) {
			if ( target.isDead() ) {
				setTarget(null);
				return;
			}
			
			// Re-look for target
			if ( RECHECK_TARGET < 0 ) {
				RECHECK_TARGET = 20;
				if ( !this.canSee(target.getEyeLocation()) ) {
					TARGET_FAILED++;
				} else {
					TARGET_FAILED = 0;
				}
			}
			
			// Remove target, if we cannot see
			if ( TARGET_FAILED > TARGET_MAX_FAIL ) {
				setTarget(null);
				return;
			}
			
			Location desiredLoc = target.getLocation();
			
			// If target is in another world (teleport?) cancel.
			if ( desiredLoc.getWorld() != getPlayer().getWorld() ) {
				setTarget(null);
				return;
			}
			
			double dist = desiredLoc.distance(getLocation());
			
			if ( dist > TARGET_TIMEOUT_DISTANCE ) {
				setTarget(null);
			} else {
				
				boolean canSeeTarget = this.canSee(target.getEyeLocation());
				
				// Cancel if we're close
				if ( dist < MINIMUM_TARGET_DISTANCE && canSeeTarget ) {
					getNavigator().cancelNavigation();
				}
				
				// Move to target
				if ( RETARGET_TIMEOUT < 0 ) {
					RETARGET_TIMEOUT = 10;
					
					if ( !canSeeTarget || dist >= MINIMUM_TARGET_DISTANCE ) {
						this.IDLE_TICKS = 0; // Not idle...
						getNavigator().setTarget(target, true);
					}
				}
				
				// Handle running
				if ( dist > 7 && !this.getPlayer().isDead() && getNavigator().isNavigating() ) {
					getPlayer().setSprinting(true);
				}
				
				if ( dist < 2.5 || !getNavigator().isNavigating() ) {
					getPlayer().setSprinting(false);
				}
				
				// Handle attacking
				if ( ATTACK_TIMEOUT < 0 ) {
					ATTACK_TIMEOUT = 20 + (int)(Math.random()*5);
					if ( attack(target) ) {
						if ( this.isHoldingRangedWeapon() ) {
							ATTACK_TIMEOUT += 20;
						}
					}
				}
			}
		} else {
			getPlayer().setSprinting(false);
			
			// Walk around
			if ( ROAM_TIMEOUT < 0 ) {
				ROAM_TIMEOUT = 80 + (int)(Math.random()*120);
				int tries = 0;
				
				// Despawn if no players nearby
				if ( CAN_NATURAL_DESPAWN ) {
					Player nearest = getNearestPlayer();
					final int MAXDIST = 150;
					if ( target == null && (nearest == null || nearest.getLocation().distanceSquared(this.getLocation()) > MAXDIST*MAXDIST) ) {
						NO_NEARBY_PLAYER_TICKS++;
						
						// If not near player for at least 5 attempts (about 500 ticks), despawn.
						if ( NO_NEARBY_PLAYER_TICKS > 5 ) {
							this.despawn(DespawnReason.REMOVAL);
							return;
						}
					} else {
						NO_NEARBY_PLAYER_TICKS = 0;
					}
				}

				Block walkToBlock = null;
				
				// Find a block to walk to
				while ( tries < PATHFIND_TIMEOUT ) {
					final double t = 40;
					
					Player player = this.getPlayer();
					if ( player == null )
						break;
					
					Location base = player.getLocation();
					double xx = base.getX() + (Math.random()-Math.random())*t;
					double yy = base.getY() + (Math.random()-Math.random())*t;
					double zz = base.getZ() + (Math.random()-Math.random())*t;
					Block tb1 = new Location( base.getWorld(), xx, yy + 0, zz ).getBlock();
					Block tb2 = new Location( base.getWorld(), xx, yy + 1, zz ).getBlock();
					Block tb3 = new Location( base.getWorld(), xx, yy + 2, zz ).getBlock();
					
					// If the bottom block is solid, and top two are not, it's safe!
					if ( tb1.getType().isSolid() && !tb2.getType().isSolid() && !tb3.getType().isSolid() ) {
						walkToBlock = tb2;
					}
					
					// If any of them contain lava, not safe!
					if ( tb1.getType().equals(Material.LAVA)
							|| tb2.getType().equals(Material.LAVA)
							|| tb3.getType().equals(Material.LAVA) ) {
						walkToBlock = null;
					}
					
					// Break out of loop, if we still have a block!
					if ( walkToBlock != null )
						break;
					
					tries++;
				}
				
				// Walk to the block
				if ( walkToBlock != null && isSpawned() ) {
					this.getNavigator().setTarget(walkToBlock.getLocation().add(0.5,0.5,0.5));
					
					Location faceLoc = walkToBlock.getLocation();
					faceLoc.setY(this.getLocation().getY());
					this.faceLocation(faceLoc);
				}
			}
		}
	}
	
	@Override
	protected void onDamage(LivingEntity damager, double damage) {
		if ( !hostileIfAttacked )
			return;
		
		if ( TICKS_SINCE_LAST_ATTACK > 40 || target == null ) {
			if ( this.canSee(damager.getEyeLocation()) ) {
				setTarget(damager);
			} else {
				// TODO walk towards the direction of the attacker, but not target him until he can be seen.
			}
		}
	}
	
	protected List<LivingEntity> getNearbyLivingEntities(float r) {
		List<LivingEntity> ret = new ArrayList<LivingEntity>();
		List<Entity> nearby = getPlayer().getNearbyEntities(r, r, r);
		
		// Filter by living entites that ARE NOT US
		for (int i = 0; i < nearby.size(); i++) {
			if ( nearby.get(i) instanceof LivingEntity ) {
				LivingEntity nearEnt = (LivingEntity) nearby.get(i);
				
				// Must be alive
				if ( nearEnt.isDead() )
					continue;
				
				// Can't be ourself
				if ( nearEnt.equals(getPlayer()) )
					continue;
				
				// Must be in same world
				if ( nearEnt.getWorld() != getPlayer().getWorld() )
					continue;
				
				// Ignore players in creative mode
				if ( nearEnt instanceof Player && ((Player)nearEnt).getGameMode() == GameMode.CREATIVE )
					continue;
				
				ret.add((LivingEntity) nearby.get(i));
			}
		}
		
		// Sort based on distance
		Location mLoc = getLocation();
		Collections.sort(ret, new Comparator<LivingEntity>() {
			@Override
			public int compare(LivingEntity o1, LivingEntity o2) {
				Location l1 = o1.getLocation();
				Location l2 = o2.getLocation();
				if ( l1.getWorld() != l2.getWorld() ) {
					return 0;
				}
				
				double d1 = l1.distanceSquared(mLoc);
				double d2 = l2.distanceSquared(mLoc);
				return (int) Math.signum(d1-d2);
			}
		});
		
		return ret;
	}
	
	protected Player getNearestPlayer() {
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		Player[] pl = players.toArray(new Player[players.size()]);
		
		Player ret = null;
		double d = Double.MAX_VALUE;
		
		for (int i = 0; i < pl.length; i++) {
			Player p = pl[i];
			if ( !p.getLocation().getWorld().equals(this.getLocation().getWorld()) )
				continue;
			
			double t = p.getLocation().distanceSquared(this.getLocation());
			if ( t < d ) {
				d = t;
				ret = p;
			}
		}
		
		return ret;
	}
}
