package com.behindthemirrors.minecraft.sRPG;

import java.util.logging.Logger;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;

import org.bukkit.plugin.Plugin;

// MAIN TODO LIST
// 
// - fix creeper damage for damage rebalancing
// - fix slime HP at spawn (as soon as bukkit allows it)
// - add active abilities
// - crafting skill and persistent item stats
// - gardening skill using the hoe
// - add all necessary chat commands and shortcuts
// - add help and detailed information pages
// 

public class SRPG extends JavaPlugin {
	static final String LOG_PREFIX = "[sRPG] ";
	static final String CHAT_PREFIX = "[sRPG] ";
	
	static Plugin plugin;
	static PluginManager pm;
	public static ProfileManager profileManager = new ProfileManager();
	static TimedEffectManager timedEffectManager = new TimedEffectManager();
	static CascadeQueueScheduler cascadeQueueScheduler = new CascadeQueueScheduler(); 
	static Database database;
	
	static Random generator = new Random();
	
	static final DamageEventListener damageListener = new DamageEventListener();
	static final SpawnEventListener spawnListener = new SpawnEventListener();
	static final CommandListener commandListener = new CommandListener();
	static final PlayerEventListener playerListener = new PlayerEventListener();
	static final BlockEventListener blockListener = new BlockEventListener();
	
	static PluginDescriptionFile pdfFile;
	
	static final Logger log = Logger.getLogger("Minecraft");
	static final Settings settings = new Settings();
	
	public void onEnable() {
		// set all instance related references
		plugin = this;
		pm = getServer().getPluginManager();
		Settings.dataFolder = getDataFolder();
		
		for (String command : new String[] {"srpg"}) {
			getCommand(command).setExecutor(commandListener);
		}
		
		// create plugins/SRPG/
		getDataFolder().mkdirs();
		
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, damageListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.ENTITY_REGAIN_HEALTH, damageListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, damageListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, spawnListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_TOGGLE_SNEAK, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
		
		// try to load settings, disable plugin on fail
		
		boolean disable = false;
		settings.load();
		if (!database.connect()) {
			output("disabling plugin");
			disable = true;
		}
		if (disable) {
			pm.disablePlugin(this);
		} else {
			pdfFile = this.getDescription();
			output(pdfFile.getName() + " v" + pdfFile.getVersion() + " has been enabled." );
			database.updateDatabase(pdfFile.getVersion());
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, timedEffectManager, 20, 20);
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, cascadeQueueScheduler, 1, 1);
		}
	}
	public void onDisable() {
		pdfFile = this.getDescription();
		for (World world : plugin.getServer().getWorlds()) {
			for (Player player : world.getPlayers()) {
				profileManager.save(player);
			}
		}
		log.info(LOG_PREFIX + pdfFile.getName() + " v" + pdfFile.getVersion() + " has been disabled." );
	}
	
    public static void output(String text) {
    	log.info(LOG_PREFIX + text);
    }
}
