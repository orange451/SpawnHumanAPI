package spawnhuman;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.util.PlayerAnimation;
import spawnhuman.etc.DamageUtil;

public class EntityPlayerNPC {
	private NPC citizensNPC;
	private Inventory lastInventory;
	private Location lastLocation;
	
	protected int TICKS = 0;
	protected int IDLE_TICKS = 0;
	protected int DESPAWN_TIME = 2400;
	protected int TICKS_SINCE_LAST_ATTACK = 0;

	protected boolean CAN_NATURAL_DESPAWN = true;
	
	private String setSkinName;
	private String name;
	
	public EntityPlayerNPC(String name, Location location) {
		this.name = name;
		this.spawn(location);
		
		if ( !isSpawned() ) {
			despawn();
			return;
		}
		
		lastInventory = getPlayer().getInventory();
		lastLocation = getPlayer().getLocation();
		getPlayer().setLastDamage(0);
		getPlayer().setNoDamageTicks(0);
		citizensNPC.setProtected(false);
		
		onSpawn();
		
		// Apply skin
		Bukkit.getScheduler().scheduleSyncDelayedTask(SpawnHuman.plugin, new Runnable() {
			@Override
			public void run() {
				if ( !isSpawned() ) {
					return;
				}
				
				SkinnableEntity skinnable = (SkinnableEntity) getPlayer();
				skinnable.setSkinName(setSkinName, true);
			}
		});
		
		this.setSkinName(name);
	}
	
	/**
	 * Sets the skin name of the Entity. Must be called on or before spawning.
	 * @param skinName
	 */
	public void setSkinName(String skinName) {
		this.setSkinName = skinName;
	}

	/**
	 * Teleport the npc to a desired location in the world.
	 * @param location
	 * @param reason
	 */
	public void teleport(Location location, TeleportCause reason) {
		this.citizensNPC.teleport(location, reason);
	}
	
	/**
	 * Spawn the npc to a desired location in the world.
	 * @param location
	 */
	public void spawn(Location location, SpawnReason reason) {
		this.citizensNPC.spawn(location, reason);
		
		// Apply armor with delay (sometimes dissappears)
		Bukkit.getScheduler().scheduleSyncDelayedTask(SpawnHuman.plugin, new Runnable() {
			@Override
			public void run() {
				if ( citizensNPC == null )
					return;
				
				getPlayer().setLastDamage(0);
				getPlayer().setNoDamageTicks(0);
				citizensNPC.setProtected(false);
			}
		}, 10l);
	}
	
	/**
	 * Spawn the npc to a desired location in the world.
	 * @param location
	 */
	public void spawn(Location location) {
		if ( citizensNPC == null ) {
			final NPC npc = SpawnHuman.spawnNPC(this, this.name);
			npc.spawn(location);
			npc.teleport(location, TeleportCause.PLUGIN);
			citizensNPC = npc;
		}

		if ( this.citizensNPC.isSpawned() )
			this.citizensNPC.despawn(DespawnReason.PENDING_RESPAWN);
		this.spawn(location, SpawnReason.RESPAWN);
	}
	
	/**
	 * Despawn the npc from the world.
	 */
	public void despawn(DespawnReason reason) {
		this.citizensNPC.despawn(reason);
		SpawnHuman.despawnNPC(this);
		this.citizensNPC = null;
	}
	
	/**
	 * Despawn the npc from the world.
	 */
	public void despawn() {
		despawn(DespawnReason.REMOVAL);
	}
	
	/**
	 * Returns whether the npc has spawned in the world.
	 * @return
	 */
	public boolean isSpawned() {
		if ( getPlayer() == null )
			return false;
		
		return this.citizensNPC.isSpawned();
	}
	
	/**
	 * Destroy the player entity.
	 */
	public void destroy() {
		this.citizensNPC.destroy();
		this.citizensNPC = null;
		SpawnHuman.despawnNPC(this);
	}
	
	/**
	 * Return the player entity wrapping this npc.
	 * @return
	 */
	public Player getPlayer() {
		if ( this.citizensNPC == null )
			return null;
		return (Player)this.citizensNPC.getEntity();
	}
	
	/**
	 * Return the npc's current inventory.
	 * @return
	 */
	public Inventory getInventory() {
		return this.lastInventory;
	}
	
	/**
	 * Returns whether this npc can visibly see a specified location.
	 * @param location
	 * @return
	 */
	public boolean canSee( Location to ) {
		final Location from = getPlayer().getEyeLocation();
		final Vector fromVec = from.toVector();
		final Vector toVec = to.toVector();
		if ( fromVec.equals(toVec) )
			return false;
		
		final double maxDist = toVec.distance(fromVec);
		if ( maxDist < 0.25 )
			return false;
		
		RayTraceResult result = null;
		Vector dir = toVec.subtract(fromVec).normalize();
		if ( dir.lengthSquared() < 0.1 )
			return false;
		
		try {
			result = getLocation().getWorld().rayTraceBlocks(from, dir, maxDist);
			
			if ( result == null || result.getHitPosition() == null ) {
				return true;
			}
		}catch(Exception e) {
			//
		}
		
		return false;
	}
	
	/**
	 * Attack another living entity with the item it is currently holding. If the target is out of possible attack range, will not attack.
	 * @param entity
	 */
	public boolean attack(LivingEntity entity) {
		if ( !isSpawned() )
			return false;
		
		if ( entity.isDead() )
			return false;
		
		// Face target
		faceLocation(entity.getLocation());
		
		// Possible to attack
		boolean canAttack = canAttack(entity);
		if ( !canAttack )
			return false;
		
		// Check if holding ranged weapon
		boolean ranged = isHoldingRangedWeapon() && getInventory().contains(Material.ARROW);
		
		// Get base damage for this item
		ItemStack holding = this.getPlayer().getInventory().getItemInMainHand();
		
		// Check if we can damage
		EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(getPlayer(), entity, DamageCause.ENTITY_ATTACK, 1);
		Bukkit.getPluginManager().callEvent(e);
		
		// Cannot damage, event cancelled.
		if ( e.isCancelled() || e.getFinalDamage() <= 0 )
			return false;
		
		// No longer idle
		this.IDLE_TICKS = 0;
		this.TICKS_SINCE_LAST_ATTACK = 0;
		
		// Attack
		if ( !ranged ) {
			if ( !canSee(entity.getLocation()) )
				return false;
			
			// Get damage of weapon
			double baseDamage = DamageUtil.getBaseDamageFromItem(holding);
			
			// Attack w/ melee weapon
			PlayerAnimation.ARM_SWING.play(getPlayer());
			entity.damage(baseDamage, getPlayer());
			entity.setLastDamageCause(e);
		} else {
			
			// Range animation
			getPlayer().setSprinting(false);
			getNavigator().cancelNavigation();
			PlayerAnimation.START_USE_MAINHAND_ITEM.play(getPlayer());
			
			// Shoot arrow with delay
			Bukkit.getScheduler().scheduleSyncDelayedTask(SpawnHuman.plugin, new Runnable() {
				@Override
				public void run() {
					Player player = getPlayer();
					if ( player == null || player.isDead() )
						return;
					
					PlayerAnimation.START_USE_MAINHAND_ITEM.play(player);
					
					Location loc = getEyeLocation();
					float dist = (float) entity.getLocation().distance(player.getLocation());
					Vector dir = entity.getLocation().add(0, dist/16f, 0).subtract(player.getLocation()).toVector().normalize();
					Arrow arrow = loc.getWorld().spawnArrow(loc, dir, 1 + (dist/24f), 0.1f);
					arrow.setShooter(player);
					
					getInventory().removeItem(new ItemStack(Material.ARROW, 1));					
				}
				
			}, 10);
		}
		
		return true;
	}
	
	/**
	 * Returns the navigator object, used to pathfind to desired locations.
	 * @return
	 */
	public Navigator getNavigator() {
		return citizensNPC.getNavigator();
	}
	
	/**
	 * Returns whether this npc is able to attack a living entity with the item it is currently holding.
	 * <br>
	 * Will return false if out-of-range.
	 * @param entity
	 * @return
	 */
	public boolean canAttack(LivingEntity entity) {
		
		// Calculate min range
		float range = (float) 3.25;
		if ( isHoldingRangedWeapon() )
			range = 55;
		
		// Cannot attack if out of range
		float dist = (float) entity.getLocation().distanceSquared(this.getPlayer().getLocation());
		if ( dist > range * range )
			return false;
		
		// Can attack!
		return true;
	}
	
	/**
	 * Look at a specific location.
	 * @param location
	 */
	public void faceLocation(Location location) {
		this.citizensNPC.faceLocation(location);
	}

	/**
	 * Returns the citizens-npc backed npc.
	 * @return
	 */
	protected NPC getNPC() {
		return this.citizensNPC;
	}
	
	/**
	 * Returns whether this npc is holding a ranged weapon.
	 * @return
	 */
	protected boolean isHoldingRangedWeapon() {
		ItemStack holding = this.getPlayer().getInventory().getItemInMainHand();
		return holding.getType().equals(Material.BOW)
				|| holding.getType().equals(Material.CROSSBOW)
				|| holding.getType().equals(Material.TRIDENT);
	}

	/**
	 * Returns the current location of the npc entity.
	 * @return
	 */
	public Location getLocation() {
		if ( lastLocation == null )
			return null;
		return lastLocation.clone();
	}
	
	/**
	 * Returns the current eye-location of the npc entity.
	 * @return
	 */
	public Location getEyeLocation() {
		Location from = getPlayer().getEyeLocation();
		if ( from.getYaw() == Double.NaN )
			from.setYaw(0);
		if ( from.getPitch() == Double.NaN )
			from.setPitch(0);
		
		return from;
	}
	
	/**
	 * Step function called by the human API at 20hz.
	 */
	protected void step() {
		if ( getPlayer() == null )
			return;
		
		if ( getPlayer().isDead() )
			getPlayer().setSprinting(false);
		
		this.lastInventory = getPlayer().getInventory();
		this.lastLocation = getPlayer().getLocation();
		
		// Increment ticks
		this.TICKS++;
		this.IDLE_TICKS++;
		this.TICKS_SINCE_LAST_ATTACK++;
		
		// Handle despawning
		if ( CAN_NATURAL_DESPAWN ) {
			if ( IDLE_TICKS > DESPAWN_TIME ) {
				this.despawn(DespawnReason.REMOVAL);
				return;
			}
		}

		// Push other NPCS (other non-npc's already push, mimic this!)
		Location loc1 = getLocation();
		EntityPlayerNPC[] npcs = SpawnHuman.npcs();
		for (int i = 0; i < npcs.length; i++) {
			EntityPlayerNPC npc = npcs[i];
			
			// Cant push ourself
			if ( npc.equals(this) )
				continue;
			
			// Get other entity's locations
			Location loc2 = npc.getLocation();
			if ( loc2 == null )
				continue;

			// If we're too far away vertically, cannot push
			if ( Math.abs(loc2.getY() - loc1.getY()) > 1 )
				continue;
			
			loc2.setY(loc1.getY());
			
			// If we're too far away in distance, cannot push
			double radius = 0.9;
			double dist = loc2.distance(loc1);
			if ( dist > radius || dist <= 0 )
				continue;
			
			// Calculate push direction
			Vector dir = loc2.toVector().subtract(loc1.toVector()).normalize();
			dir.multiply(0.1);
			
			// Push
			npc.getPlayer().setVelocity(npc.getPlayer().getVelocity().add(dir));
		}
	}

	protected void onDamage(LivingEntity damager, double damage) {
		//
	}
	
	protected void onSpawn() {
		//
	}
}
