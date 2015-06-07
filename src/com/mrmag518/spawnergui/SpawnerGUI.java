package com.mrmag518.spawnergui;

import com.mrmag518.spawnergui.files.Config;
import com.mrmag518.spawnergui.files.Database;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SpawnerGUI extends JavaPlugin {

	private Economy eco;
	private WorldGuardPlugin worldguard;
	public static final Set<String> openGUIs = new HashSet<>();
	public static SpawnerGUI i;

	@Override
	public void onEnable()
	{
		i = this;
		if(!getDataFolder().exists()) getDataFolder().mkdir();
		Config.init();
		Database.init();
		
		if(getServer().getPluginManager().getPlugin("Vault") != null)
		{
			RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
			if (rsp != null)
			{
				eco = rsp.getProvider();
			}
			else
			{
				Logger.getLogger("Minecraft").log(Level.WARNING, "[SpawnerGUI] Found no Vault supported economy plugin! Disabled economy support.");
			}
		}
		
		if(getServer().getPluginManager().getPlugin("WorldGuard") != null && Config.getConfig().getBoolean("Protection.WorldGuard"))
		{
			worldguard = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
			Logger.getLogger("Minecraft").log(Level.INFO, "[SpawnerGUI] WorldGuard hooked.");
		}

		getServer().getPluginManager().registerEvents(new Handler(), this);
	}

	@Override
	public void onDisable()
	{
		eatGUIs();
	}

	public void openGUI(CreatureSpawner spawner, Player player)
	{
		Spawnable type = Spawnable.from(spawner.getSpawnedType());
		GUIHandler gui = new GUIHandler("Spawner Type: " + type.getName(), 36, spawner);
		int j = 0;

		for(Spawnable e : Spawnable.values())
		{
			if(Config.getConfig().getBoolean("Settings.RemoveNoAccessEggs") && noAccess(player, e)) continue;
			double price = getPrice(e);
			String priceLine = price > 0.0 ? "§e" + price : "§aFree";
			String accessLine = noAccess(player, e) ? "§7Access: §cNo" : "§7Access: §aYes";
			
			priceLine += (player.hasPermission("spawnergui.eco.bypass." + e.getName().toLowerCase()) || player.hasPermission("spawnergui.eco.bypass.*")) && price > 0.0 ? " §a§o(Free for you)" : "";
			
			if (eco != null && Config.getConfig().getBoolean("Settings.ShowCostInLore")) {
				if(Config.getConfig().getBoolean("Settings.ShowAccessInLore")) {
					gui.setItem(j, e.getSpawnEgg(), "§6" + e.getName(), "§7Price: " + priceLine, accessLine);
				} else {
					gui.setItem(j, e.getSpawnEgg(), "§6" + e.getName(), "§7Price: " + priceLine);
				}
			} else {
				if(Config.getConfig().getBoolean("Settings.ShowAccessInLore")) {
					gui.setItem(j, e.getSpawnEgg(), "§6" + e.getName(), accessLine);
				} else {
					gui.setItem(j, e.getSpawnEgg(), "§6" + e.getName());
				}
			}
			j++;
		}
		
		if(Config.getConfig().getBoolean("Settings.ShowBalanceIcon")) {
			String s = eco != null ? "§aYour Balance: §e" + Math.round(eco.getBalance(player) * 100.0) / 100.0 : "§cEconomy is not enabled!";
			gui.setItem(35, new ItemStack(Material.SKULL_ITEM, 1, (byte)3), "§bBalance", s);
		}
		gui.open(player);
		openGUIs.add(player.getName());
	}
	
	public double getPrice(Spawnable type) {
		return Config.getConfig().getDouble("MobPrices." + type.getName());
	}
	
	public boolean noAccess(Player p, Spawnable type) {
		return !p.hasPermission("spawnergui.edit.*") && !p.hasPermission("spawnergui.edit." + type.getName().toLowerCase());
	}
	
	public void eatGUIs() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(openGUIs.contains(p.getName())) {
				p.getOpenInventory().close();
				p.sendMessage("§cThe GUI was forced to close due to a reload.");
			}
		}
	}
	
	private boolean canOpenAtLoc(Player p, Location loc) {
		if(worldguard != null && !p.isOp()) {
			RegionManager r = worldguard.getRegionManager(loc.getWorld());
			
			if(r != null) {
				ApplicableRegionSet regions = r.getApplicableRegions(loc);
				LocalPlayer lp = new BukkitPlayer(worldguard, p);
				return regions.canBuild(lp);
			}
		}
		return true;
	}

	public class Handler implements Listener {
		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void handleInteract(PlayerInteractEvent event) {
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Block b = event.getClickedBlock();
				Player p = event.getPlayer();
				
				if(b != null && b.getType() == Material.MOB_SPAWNER && p.hasPermission("spawnergui.open")) {
					event.setCancelled(true);
					
					if(!canOpenAtLoc(p, b.getLocation())) {
						p.sendMessage("§cThat spawner is protected and cannot be edited by you.");
						return;
					}
					
					if(Config.getConfig().getBoolean("Settings.SneakToOpen") && p.isSneaking()) {
						openGUI((CreatureSpawner)b.getState(), p);
					} else if(Config.getConfig().getBoolean("Settings.SneakToOpen") == false && !p.isSneaking()) {
						openGUI((CreatureSpawner)b.getState(), p);
					}
				}
			}
		}
		
		@EventHandler
		public void handleClick(GUIClickEvent event)
		{
			Player player = event.getPlayer();
			CreatureSpawner spawner = event.getSpawner();
			
			if(spawner.getBlock().getType() != Material.MOB_SPAWNER)
			{
				player.sendMessage(ChatColor.RED + "The spawner block is no longer valid! (" + ChatColor.GRAY + spawner.getBlock().getType().name().toLowerCase() + ChatColor.RED + ")");
				return;
			}

			assert event.getItem().getItemMeta().hasDisplayName();
			String clicked = ChatColor.stripColor(event.getItem().getItemMeta().getDisplayName().toLowerCase());
			Spawnable current = Spawnable.from(spawner.getSpawnedType());
			
			if(clicked.equalsIgnoreCase("balance")) {
				event.setWillClose(false);
			} else {
				for(Spawnable e : Spawnable.values()) {
					if(clicked.equalsIgnoreCase(e.getName().toLowerCase())) {
						player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

						if(!noAccess(player, e)) {
							if(eco != null && !player.hasPermission("spawnergui.eco.bypass.*")) {
								double price = player.hasPermission("spawnergui.eco.bypass." + clicked) ? 0.0 : getPrice(e);

								if(price > 0.0) {
									if(eco.has(player, price)) {
										player.sendMessage("§7Charged §f" + price + " §7of your balance.");
										eco.withdrawPlayer(player, price);
									} else {
										player.sendMessage("§cYou need at least §7" + price + " §cin balance to do this!");
										return;
									}
								}
							}
							spawner.setSpawnedType(e.getType());
							spawner.update(true);
							player.sendMessage("§9Spawner type changed from §7" + current.getName().toLowerCase() + " §9to §7" + clicked + "§9!");
							return;
						}
						player.sendMessage("§cYou are not allowed to change to that type!");
						break;
					}
				}
			}
		}
	}

}