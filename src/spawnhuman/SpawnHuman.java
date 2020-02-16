package spawnhuman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import highwayman.BanditSpawner;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCDataStore;
import net.citizensnpcs.api.npc.NPCRegistry;
import spawnhuman.ai.HostilePlayerNPC;

public class SpawnHuman extends JavaPlugin {
	public static JavaPlugin plugin;
	protected static NPCRegistry npcs;
	protected static List<EntityPlayerNPC> currentNPCs;
	
	public SpawnHuman() {
		plugin = this;
	}
	
	protected static NPC spawnNPC(EntityPlayerNPC enpc, String name) {
		final NPC npc = npcs.createNPC(EntityType.PLAYER, name);
		npc.setProtected(false);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				synchronized(currentNPCs) {
					currentNPCs.add(enpc);
				}
			}
		});
		
		return npc;
	}
	
	public static EntityPlayerNPC matchNPC(LivingEntity entity) {
		NPC npc = npcs.getNPC(entity);

		for (int i = 0; i < currentNPCs.size(); i++) {
			if ( i >= currentNPCs.size() )
				continue;
			
			EntityPlayerNPC t = currentNPCs.get(i);
			if ( t == null )
				continue;
			
			if ( npc.equals(t.getNPC()) ) {
				return t;
			}
		}
		
		return null;
	}
	
	public synchronized static EntityPlayerNPC[] npcs() {
		return currentNPCs.toArray(new EntityPlayerNPC[currentNPCs.size()]);
	}
	
	protected static void despawnNPC(EntityPlayerNPC npc) {
		NPC cit = npc.getNPC();
		if ( cit != null )
			npcs.deregister(cit);
		
		currentNPCs.remove(npc);
	}
	
	@Override
	public void onEnable() {
		
		npcs = CitizensAPI.createNamedNPCRegistry("SpawnHuman", new NPCDataStore() {
			@Override
			public void clearData(NPC arg0) {
				//
			}

			@Override
			public int createUniqueNPCId(NPCRegistry arg0) {
				return (int) (Math.random()*Integer.MAX_VALUE);
			}

			@Override
			public void loadInto(NPCRegistry arg0) {
				//
			}

			@Override
			public void reloadFromSource() {
				//
			}

			@Override
			public void saveToDisk() {
				//
			}

			@Override
			public void saveToDiskImmediate() {
				//
			}

			@Override
			public void store(NPC arg0) {
				//
			}

			@Override
			public void storeAll(NPCRegistry arg0) {
				//
			}
		});
		
		currentNPCs = Collections.synchronizedList(new ArrayList<EntityPlayerNPC>());
		
		// Call npc step function
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
				synchronized(currentNPCs) {
					for (int i = 0; i < currentNPCs.size(); i++) {
						if ( i >= currentNPCs.size() )
							continue;
						
						EntityPlayerNPC npc = currentNPCs.get(i);
						if ( npc == null )
							continue;
						
						if ( !npc.isSpawned() )
							continue;
						
						try {
							npc.step();
						}catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, 1, 1);
		
		this.getCommand("npcamt").setExecutor(new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] arg3) {
				int amt = currentNPCs.size();
				sender.sendMessage("There are currently " + amt + " npcs alive.");
				return true;
			}
		});
		
		this.getCommand("npctest").setExecutor(new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] arg3) {
				if ( !(sender instanceof Player) )
					return false;
				
				if ( !sender.isOp() )
					return false;
				
				Player p = (Player)sender;
				EntityPlayerNPC npc = new HostilePlayerNPC(ChatColor.RED + "Mean Steve" + ChatColor.WHITE, p.getLocation().add(Math.random()-Math.random(), 0, Math.random()-Math.random()));
				npc.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
				npc.setSkinName("notch");

				return true;
			}
		});

		Bukkit.getPluginManager().registerEvents(new SpawnHumanEventListener(), this);
		
		new BanditSpawner();
	}
	
	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(plugin);
		
		ArrayList<EntityPlayerNPC> spawnednpcs = new ArrayList<EntityPlayerNPC>();
		
		currentNPCs.forEach((npc)-> {
			if ( npc.isSpawned() ) {
				spawnednpcs.add(npc);
			}
		});
		
		for (int i = 0; i < spawnednpcs.size(); i++) {
			spawnednpcs.get(i).destroy();
		}
		npcs.deregisterAll();
		spawnednpcs.clear();
	}
}
