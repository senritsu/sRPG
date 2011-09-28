package com.behindthemirrors.minecraft.sRPG;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;

import org.bukkit.plugin.Plugin;

import com.behindthemirrors.minecraft.sRPG.listeners.BlockEventListener;
import com.behindthemirrors.minecraft.sRPG.listeners.CommandListener;
import com.behindthemirrors.minecraft.sRPG.listeners.DamageEventListener;
import com.behindthemirrors.minecraft.sRPG.listeners.PlayerEventListener;
import com.behindthemirrors.minecraft.sRPG.listeners.SpawnEventListener;

// MAIN TODO LIST
// 
// - crafting skill and persistent item stats
// - gardening skill using the hoe
// - add all necessary chat commands and shortcuts
// - add help and detailed information pages
// 

public class SRPG extends JavaPlugin {
	static final String LOG_PREFIX = "[sRPG] ";
	static final String CHAT_PREFIX = "[sRPG] ";
	static final String DEBUG_PREFIX = "[sDEBUG]";
	
	public static boolean debug = false;
	public static ArrayList<String> debugmodes = new ArrayList<String>();
	
	public static Plugin plugin;
	static PluginManager pm;
	public static ProfileManager profileManager = new ProfileManager();
	public static TimedEffectManager timedEffectManager = new TimedEffectManager();
	public static CascadeQueue cascadeQueueScheduler = new CascadeQueue(); 
	public static Database database;
	
	public static Random generator = new Random();
	
	static final DamageEventListener damageListener = new DamageEventListener();
	static final SpawnEventListener spawnListener = new SpawnEventListener();
	static final CommandListener commandListener = new CommandListener();
	static final PlayerEventListener playerListener = new PlayerEventListener();
	static final BlockEventListener blockListener = new BlockEventListener();
	
	static PluginDescriptionFile pdfFile;
	
	static final Logger log = Logger.getLogger("Minecraft");
	public static final Settings settings = new Settings();
	
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
			pm.registerEvent(Event.Type.ENTITY_DAMAGE, damageListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.ENTITY_REGAIN_HEALTH, damageListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.ENTITY_DEATH, damageListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.CREATURE_SPAWN, spawnListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, playerListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.PLAYER_TOGGLE_SNEAK, playerListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Monitor, this);
			pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.BLOCK_CANBUILD, blockListener, Priority.Highest, this);
			pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Highest, this);
			
			pdfFile = this.getDescription();
			output(pdfFile.getName() + " v" + pdfFile.getVersion() + " has been enabled." );
			database.updateDatabase(pdfFile.getVersion());
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, timedEffectManager, 20, 20);
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, cascadeQueueScheduler, 1, 1);
			spawnListener.addExistingCreatures();
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
    
    public static void dout(String text, String mode) {
    	if (debug && (mode == null || debugmodes.contains(mode))) {
    		log.info(DEBUG_PREFIX + text + (mode == null?"":" [from '"+mode+"']"));
    	}
    }
    
    public static void dout(String text) {
    	dout(text,null);
    }
}
