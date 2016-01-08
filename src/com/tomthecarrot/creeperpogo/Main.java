/*
 * CreeperPogo plugin for Bukkit/Minecraft.
 * by Thomas Suarez (tomthecarrot)
 */

package com.tomthecarrot.creeperpogo;

import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftCreeper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin implements Listener {
	final Logger logger = Logger.getLogger("Minecraft"); // Console window
	static Main plugin; // This plugin
	ShapedRecipe recipe; // The crafting recipe for a creeper pogo stick
	ItemStack creeperleg; // The item type for a creeper leg
	ItemStack creeperpogo; // The item type for a creeper pogo stick
	final int pogoUses = 7; // The constant amount of pogo stick uses (durability)
	final float chancePercentage = 50; // the chance (in percentage) that a creeper will drop a creeper leg
	boolean takeFallDamage = true; // after the player uses the creeper pogo stick, this is set to false. otherwise it should be true.
	
	@Override
	public void onDisable() {
		// If this plugin is disabled, alert the console:
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " Has Been Disabled!");
	}
	@Override
	public void onEnable() {
		// When this plugin is enabled, alert the console:
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " Has Been ENABLED!");
		getServer().getPluginManager().registerEvents(this, this);
		
		creeperpogo = new ItemStack(Material.STICK, 1);
		setName(creeperpogo, "Creeper Pogo Stick");
		creeperleg = new ItemStack(Material.STICK, 1);
		setName(creeperleg, "Creeper Leg");
		creeperpogo.setDurability((short) pogoUses);
		
		// The crafting recipe for a Creeper Pogo Stick:
		recipe = new ShapedRecipe(creeperpogo).shape("ccc", " s ", " s ").setIngredient('c', creeperleg.getData().getItemType() /*Material.STICK*/).setIngredient('s', Material.SULPHUR);
		getServer().addRecipe(recipe);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		// Get the current player object:
		Player p = (Player) sender;
		
		// Make sure the player is allowed to have a creeper pogo stick:
		if (!p.hasPermission("creeperpogo.cmd")) {
			p.sendMessage(ChatColor.RED + "You are not allowed to use a Creeper Pogo Stick!");
			return false;
    	}
		if (commandLabel.equals("pogo")) {
			if (args.length > 1) {
				p = getServer().getPlayer(args[1]);
			}
			addPogo(p);
		}
		if (commandLabel.equals("creeperleg")) {
			addLeg(p);
		}
		return false;
	}
	
	@EventHandler
	public void onRecipe(CraftItemEvent e) {
		ShapedRecipe r = (ShapedRecipe) e.getRecipe();
        if (r instanceof ShapedRecipe) {
            if (r == recipe) {// && hasItem(e.get)) {
            	e.setResult(Result.ALLOW);
            }
        }
    }
	
	@EventHandler
	public void onPlayerDamage(EntityDamageEvent event) {
		// Make sure that if the player has used the creeper pogo stick,
		// no fall damage or explosion damage is inflicted.
		// Then, propel the player upward.
		
		if (!takeFallDamage) {
			if (event.getEntity() instanceof Player) {
				Player p = (Player)event.getEntity();
				
				// Only if the player has the pogo stick in
				// their hand, cancel the player damage event:
			    if (hasPogo(p)) {
			        event.setCancelled(true);
			        Vector vector = p.getVelocity();
			        vector.setY(2);
			        p.setVelocity(vector);
			    }
			    
			    // Wait a little for the
			    // player damage to be cancelled:
			    BukkitScheduler scheduler = getServer().getScheduler();
			    scheduler.scheduleSyncDelayedTask(this, new Runnable() {
			    	@Override
			    	public void run() {
			    		takeFallDamage = true;
			    	}
			    }, 20L);
			}
		}
	}
	
	@EventHandler
	public void onPlayerUse(PlayerInteractEvent event) {
			final Player p = event.getPlayer();
			
			if (p.getItemInHand().getItemMeta() != null) {
				final ItemStack item = p.getItemInHand();
				
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
	        		if (item.getItemMeta().getDisplayName() != null) {
	        			if (item.getItemMeta().getDisplayName().equals("Creeper Pogo Stick")) {
	        				// Check to make sure that the player is allowed to use a creeper pogo stick:
		            		if (!p.hasPermission("creeperpogo")) {
		            			p.sendMessage(ChatColor.RED + "You are not allowed to use a Creeper Pogo Stick!");
		                	}
		            		else {
		            			// Make sure the player doesn't take fall damage:
		                		takeFallDamage = false;
		            			
		            			// Create a creeper explosion:
		            			p.getWorld().createExplosion(p.getLocation(), 4);
		                		
		            			// Decrease the durability of the item:
		                		short dur = item.getDurability();
		                		short minus = (short) 1;
		                		short newdur = (short) (dur-minus);
		                		item.setDurability(newdur);
		                		
		                		// If the item's durability has reached 0, remove the item from inventory:
		                		if (item.getDurability() == 0) {
		            	        	p.getInventory().remove(item);
		            	        	p.sendMessage(ChatColor.RED + "DONE!");
		            	        }
		            		}
		            	}
	        		}
	            }
	        }
	}
	
	public void addLeg(Player p) {
		p.getInventory().addItem(creeperleg);
		p.sendMessage(ChatColor.GREEN + "You got a Creeper Leg!");
	}
	
	public void addPogo(Player p) {
		p.getInventory().addItem(creeperpogo);
	}
	
	public ItemStack setName(ItemStack is, String name){
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        is.setItemMeta(m);
        return is;
    }
	
	public boolean hasPogo(Player p) {
		if (p.getInventory().getItemInHand().getItemMeta().getDisplayName().equals("Creeper Pogo Stick")) {
			return true;
		}
		return false;
		
		/*ItemStack[] inv = p.getInventory().getContents();
		for (ItemStack item:inv) {
			if (item != null) {
				if (item.getItemMeta().getDisplayName() != null) {
	    			if (item.getItemMeta().getDisplayName().equals("Creeper Pogo Stick")) {
	    				return true;
	    			}
				}
			}
		}
		return false;*/
	}
	
	@EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
		// When a creeper dies, create a Random chance float.
		// If the player is in luck with chance, make the creeper drop a creeper leg.
		
		if (event.getEntity().toString().equals("CraftCreeper")) {
			CraftCreeper c = (CraftCreeper) event.getEntity();
			if (c.getKiller() != null) {
				Player p = (Player) event.getEntity().getKiller();
				
				if (c != null && p != null) {
		        	Random r = new Random();
		        	float chance = r.nextFloat();
		        	
		        	if (chance <= chancePercentage/100 && p.hasPermission("creeperpogo")) {
		        		c.getWorld().dropItemNaturally(c.getLocation(), creeperleg);
		        	}
		        }
			}
		}
	}
}