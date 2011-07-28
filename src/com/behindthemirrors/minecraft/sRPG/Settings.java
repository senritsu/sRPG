package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;

public class Settings {
	
	static String difficulty;
	
	static File dataFolder;
	static Configuration config;
	static Configuration advanced;
	static Configuration jobsettings;
	static HashMap<String,Configuration> localization;
	static String defaultLocale;
	static ArrayList<String> ANIMALS = new ArrayList<String>(Arrays.asList(new String[] {"pig","sheep","chicken","cow","squid"}));
	static ArrayList<String> MONSTERS = new ArrayList<String>(Arrays.asList(new String[] {"zombie","spider","skeleton","creeper","slime","pigzombie","ghast","giant","wolf"}));
	static ArrayList<String> SKILLS = new ArrayList<String>(Arrays.asList(new String[] {"swords","axes","pickaxes","shovels","hoes","bow","ukemi","evasion", "focus"}));
	static HashMap<String,ArrayList<String>> SKILLS_ALIASES;
	static ArrayList<String> TOOLS = new ArrayList<String>(Arrays.asList(new String[] {"swords","pickaxes","axes","shovels","hoes"}));
	static ArrayList<String> GRADES =  new ArrayList<String>(Arrays.asList(new String[] {"wood","stone","iron","gold","diamond"}));
	
	static HashMap<Integer,String> SLIME_SIZES = new HashMap<Integer,String>();
	static Material[] TOOL_MATERIALS = {Material.WOOD_SWORD,Material.STONE_SWORD,Material.IRON_SWORD,Material.GOLD_SWORD, Material.DIAMOND_SWORD,
									    Material.WOOD_PICKAXE,Material.STONE_PICKAXE,Material.IRON_PICKAXE,Material.GOLD_PICKAXE,Material.DIAMOND_PICKAXE,
									    Material.WOOD_AXE,Material.STONE_AXE,Material.IRON_AXE,Material.GOLD_AXE,Material.DIAMOND_AXE,
									    Material.WOOD_SPADE,Material.STONE_SPADE,Material.IRON_SPADE,Material.GOLD_SPADE,Material.DIAMOND_SPADE,
									    Material.WOOD_HOE,Material.STONE_HOE,Material.IRON_HOE,Material.GOLD_HOE,Material.DIAMOND_HOE};
	static HashMap<Material,String> TOOL_MATERIAL_TO_STRING = new HashMap<Material,String>();
	static HashMap<Material,String> TOOL_MATERIAL_TO_TOOL_GROUP = new HashMap<Material,String>();
	static HashMap<Material,ArrayList<Material>> MULTIDROP_VALID_BLOCKS = new HashMap<Material, ArrayList<Material>>();
	static ArrayList<Material> BLOCK_CLICK_BLACKLIST = new ArrayList<Material>(Arrays.asList(new Material[] {Material.BED,
															Material.BED_BLOCK,Material.DISPENSER,Material.FURNACE,Material.BURNING_FURNACE,Material.JUKEBOX,
															Material.NOTE_BLOCK,Material.STORAGE_MINECART,Material.WOOD_DOOR,
															Material.WOODEN_DOOR,Material.CHEST,Material.WORKBENCH,Material.TNT,
															Material.MINECART,Material.BOAT,Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.TRAP_DOOR}));
	
	static HashMap<String,String> colorMap = new HashMap<String, String>();
	static HashMap<String, HashMap<String, String>> nameReplacements;
	
	public Settings() {
		// initialize Material to strig mappings
		for (int i = 0; i < TOOLS.size(); i++) {
			int length = GRADES.size();
			for (int j = 0; j < length; j++) {
				TOOL_MATERIAL_TO_STRING.put(TOOL_MATERIALS[i*length+j], TOOLS.get(i)+"."+GRADES.get(j));
				TOOL_MATERIAL_TO_TOOL_GROUP.put(TOOL_MATERIALS[i*length+j], TOOLS.get(i));
			}
		}
		TOOL_MATERIAL_TO_TOOL_GROUP.put(Material.BOW,"bow");
		// initialize slime int size to string mapping
		String[] sizes = {"small","normal","big","huge"};
		for (int i=1; i<5;i++) {
			SLIME_SIZES.put(i, sizes[i-1]);
		}
		// TODO: rework the multidrop implementation because it sucks
		// initialize tool mining multidrop whitelist
		ArrayList<Material> arraylist = new ArrayList<Material>();
		arraylist.add(Material.STONE);
		arraylist.add(Material.COBBLESTONE);
		arraylist.add(Material.MOSSY_COBBLESTONE);
		arraylist.add(Material.SANDSTONE);
		arraylist.add(Material.COAL_ORE);
		MULTIDROP_VALID_BLOCKS.put(Material.WOOD_PICKAXE,arraylist);
		
		arraylist = new ArrayList<Material>();
		arraylist.addAll(MULTIDROP_VALID_BLOCKS.get(Material.WOOD_PICKAXE));
		arraylist.add(Material.LAPIS_ORE);
		arraylist.add(Material.IRON_ORE);
		MULTIDROP_VALID_BLOCKS.put(Material.STONE_PICKAXE,arraylist);
		
		arraylist = new ArrayList<Material>();
		arraylist.addAll(MULTIDROP_VALID_BLOCKS.get(Material.STONE_PICKAXE));
		arraylist.add(Material.DIAMOND_ORE);
		arraylist.add(Material.GOLD_ORE);
		arraylist.add(Material.REDSTONE_ORE);
		MULTIDROP_VALID_BLOCKS.put(Material.IRON_PICKAXE,arraylist);
		
		arraylist = new ArrayList<Material>();
		arraylist.addAll(MULTIDROP_VALID_BLOCKS.get(Material.IRON_PICKAXE));
		arraylist.add(Material.OBSIDIAN);
		MULTIDROP_VALID_BLOCKS.put(Material.DIAMOND_PICKAXE,arraylist);
		
		ArrayList<Material> log = new ArrayList<Material>();
		log.add(Material.LOG);
		MULTIDROP_VALID_BLOCKS.put(Material.WOOD_AXE,log);
		MULTIDROP_VALID_BLOCKS.put(Material.STONE_AXE,log);
		MULTIDROP_VALID_BLOCKS.put(Material.IRON_AXE,log);
		MULTIDROP_VALID_BLOCKS.put(Material.DIAMOND_AXE,log);
		// initialize valid blocks for rare shovel drop
		
		// initialize color tag replacement map
		String[] colorStrings = new String[] {
				"[aqua]","[black]","[blue]","[dark_aqua]",
				"[dark_blue]","[dark_gray]","[dark_green]","[dark_purple]",
				"[dark_red]","[gold]","[gray]","[green]",
				"[light_purple]","[red]","[white]","[yellow]"
				};
		ChatColor[] chatColors = new ChatColor[] {
				ChatColor.AQUA,ChatColor.BLACK,ChatColor.BLUE,ChatColor.DARK_AQUA,
				ChatColor.DARK_BLUE,ChatColor.DARK_GRAY,ChatColor.DARK_GREEN,ChatColor.DARK_PURPLE,
				ChatColor.DARK_RED,ChatColor.GOLD,ChatColor.GRAY,ChatColor.GREEN,
				ChatColor.LIGHT_PURPLE,ChatColor.RED,ChatColor.WHITE,ChatColor.YELLOW,
				};
		for (int i=0;i<colorStrings.length;i++) {
			colorMap.put(colorStrings[i], chatColors[i].toString());
		}
	}
	
	Configuration openConfig(File folder, String name, String description, String defaultFileName) {
		File file = Utility.createDefaultFile(new File(folder, name+".yml"),description,defaultFileName+".yml");
		if (file.exists()){
			Configuration configuration = new Configuration(file);
			// TODO: add try/catch for .yml parsing errors
			configuration.load();
			return configuration;
		} else {
			SRPG.output("Error loading "+description+" ("+name+".yml)");
			return null;
		}
	}
	
	public void load() {
		// get config data
		Boolean disable = false;
		config = openConfig(dataFolder,"config","basic configuration","config");
		advanced = openConfig(dataFolder,"config_advanced","advanced configuration","config_advanced");
		
		if (config == null || advanced == null) {
			disable = true;
		} else {
			ConfigurationNode node;
			
			// read locale data
			ArrayList<String> availableLocales = (ArrayList<String>)config.getStringList("settings.locales.available",new ArrayList<String>());
			defaultLocale = config.getString("settings.locales.default");
			if (!availableLocales.contains(defaultLocale)) {
				availableLocales.add(defaultLocale);
			}
			localization = new HashMap<String,Configuration>();
			nameReplacements = new HashMap<String, HashMap<String,String>>();
			SKILLS_ALIASES = new HashMap<String, ArrayList<String>>();
			
			// create plugin default locale file
			for (String name : new String[] {"EN"}) {
				Utility.createDefaultFile(new File(new File(dataFolder,"locales"),name+".yml"), "'"+name+"' locale settings", "locale"+name+".yml");
			}
			// create plugin default difficulties
			for (String name : new String[] {"default","original"}) {
				Utility.createDefaultFile(new File(new File(dataFolder,"difficulties"),name+".yml"), "'"+name+"' difficulty settings", "difficulty_"+name+".yml");
			}
			
			for (String locale : availableLocales) {
				File file = new File(new File(dataFolder,"locales"),locale+".yml");
				// plugin default locale
				if (!file.exists()){
					SRPG.output("Error loading locale '"+locale+"', initializing from EN");
					// create copy of EN for specified locale if no file is present
					file = Utility.createDefaultFile(new File(new File(dataFolder,"locales"),locale+".yml"), "'"+locale+"' locale settings", "locale_EN.yml");
				}
				// disable plugin if file could not be created or opened
				if (!file.exists()) {
					disable = true;
				} else {
					localization.put(locale,new Configuration(file));
					// TODO: add try/catch for .yml parsing errors
					localization.get(locale).load(); 
					
					// update skill aliases
					SKILLS_ALIASES.put(locale,new ArrayList<String>());
					for (String skillname : SKILLS) {
						SKILLS_ALIASES.get(locale).add(localization.get(locale).getString("skills."+skillname).toLowerCase());
					}
				}
			}
		
			// update player locales if set locales are not available anymore
			for (LivingEntity entity : SRPG.profileManager.profiles.keySet()) {
				if (!(entity instanceof Player)) {
					continue;
				}
				Player player = (Player)entity;
				ProfilePlayer profile = SRPG.profileManager.get(player);
				if (!Settings.localization.containsKey(profile.locale)) {
					profile.locale = defaultLocale;
					SRPG.profileManager.save(player,"locale");
					SRPG.output("changed locale for player "+profile.name+" to default");
				}
			}
			
			// read config data
			node = config.getNode("mySQL");
			SRPG.database.mySQLenabled = node.getBoolean("enabled", false);
			if (SRPG.database.mySQLenabled) {
				SRPG.database.dbServer = node.getString("server");
				SRPG.database.dbPort = node.getString("port");
				SRPG.database.dbName = node.getString("dbName");
				SRPG.database.dbUser = node.getString("dbUser");
				SRPG.database.dbPass = node.getString("dbPass");
				SRPG.database.dbTablePrefix = node.getString("table_prefix");
			}
			
			// read xp settings
			ProfilePlayer.xpToLevel = advanced.getInt("xp.to-levelup", 1000);
			// read skill settings
			ProfilePlayer.focusBase = advanced.getInt("skills.costs.focus-base", 1);
			ProfilePlayer.focusIncrease = advanced.getInt("skills.costs.focus-increase", 1);
			ProfilePlayer.skillCosts = (ArrayList<Integer>)advanced.getIntList("skills.costs.generic", null);
			ProfilePlayer.milestoneRequirements = new HashMap<Integer, String>();
			for (String milestone : new String[] {"novice","apprentice","expert","master"}) {
				ProfilePlayer.milestoneRequirements.put(advanced.getInt("skills.milestones."+milestone, 0), milestone);
			}
			// read ability settings
			ProfilePlayer.chargeMax = advanced.getInt("abilities.max-charges", 1);
			ProfilePlayer.chargeTicks = advanced.getInt("abilities.blocks-to-charge", 1);
			ProfilePlayer.abilityCosts = new HashMap<String, Integer>();
			for (String tool : Settings.TOOL_MATERIAL_TO_STRING.values()) {
				int cost = advanced.getInt("abilities.costs."+tool,-1);
				if (cost >= 0) {
					ProfilePlayer.abilityCosts.put(tool, cost);
				}
			}
			
			// read difficulty/combat settings
			difficulty = config.getString("settings.combat.difficulty");
			File file = new File(new File(dataFolder,"difficulties"),difficulty+".yml");
			if (!file.exists()){
				SRPG.output("Error loading settings for difficulty '"+difficulty+"', initializing from default");
				// create copy of default settings for specified difficulty name if no file is present
				file = Utility.createDefaultFile(new File(new File(dataFolder,"difficulties"),difficulty+".yml"), "'"+difficulty+"' difficulty settings", "difficulty_default.yml");
			}
			// disable plugin if file could not be created or opened
			if (!file.exists()) {
				disable = true;
			} else {
				Configuration difficultyConfig = new Configuration(file);
				// TODO: add try/catch for .yml parsing errors
				difficultyConfig.load();
				
				
				// damage increase with depth
				DamageEventListener.increaseDamageWithDepth = config.getBoolean("settings.combat.dangerous-depths", false);
				
				DamageEventListener.depthTiers = new ArrayList<int[]>();
				ArrayList<Integer> thresholds = (ArrayList<Integer>) difficultyConfig.getIntList("settings.dangerous-depths.thresholds", null);
				ArrayList<Integer> damageIncreases = (ArrayList<Integer>) difficultyConfig.getIntList("settings.dangerous-depths.damage-increases", null);
				for (int i = 0;i < thresholds.size();i++) {
					DamageEventListener.depthTiers.add(new int[] {thresholds.get(i),
																  damageIncreases.get(i)});
				}
				// animal health and xp
				SpawnEventListener.healthTableCreatures = new HashMap<String, Integer>();
				DamageEventListener.xpTableCreatures = new HashMap<String, Integer>();
				for (String animal : Settings.ANIMALS) {
					node = difficultyConfig.getNode("stats.animals."+animal);
					SpawnEventListener.healthTableCreatures.put(animal, node.getInt("health", 1));
					DamageEventListener.xpTableCreatures.put(animal, node.getInt("xp", 0));
				}
				// monster health, damage and xp
				DamageEventListener.damageTableMonsters = new HashMap<String, Integer>();
				ArrayList<String> monsters = new ArrayList<String>();
				for (String monster : Settings.MONSTERS) {
					if (monster.equals("slime")) {
						for (int i=1;i<5;i++) {
							monsters.add(monster + "." + Settings.SLIME_SIZES.get(i));
						}
					} else if (monster.equals("wolf")){
						for (String state : new String[] {".wild",".tamed"}) {
							monsters.add(monster + state);
						}
					} else {
						monsters.add(monster);
					}
				}
				for (String monster : monsters) {
					node = difficultyConfig.getNode("stats.monsters."+monster);
					SpawnEventListener.healthTableCreatures.put(monster, node.getInt("health", 1));
					DamageEventListener.damageTableMonsters.put(monster, node.getInt("damage", 1));
					DamageEventListener.xpTableCreatures.put(monster, node.getInt("xp", 0));
				}
				
				// tool damage
				DamageEventListener.damageTableTools = new HashMap<String, Integer>();
				node = difficultyConfig.getNode("stats.tools");
				for (String tool : Settings.TOOL_MATERIAL_TO_STRING.values()) {
					if (!node.getBoolean(tool+".override", false)) {
						DamageEventListener.damageTableTools.put(tool, node.getInt(tool+".damage", 1));
					}
				}
				if (!node.getBoolean("bow.override", false)) {
					DamageEventListener.damageTableTools.put("bow",node.getInt("bow.damage", 1));
				}
				if (!node.getBoolean("fists.override", false)) {
					DamageEventListener.damageTableTools.put("fists",node.getInt("fists.damage", 1));
				}
				// critical hit and miss settings
				node = difficultyConfig.getNode("settings.combat");
				CombatInstance.defaultCritChance = node.getDouble("crit-chance", 0.0);
				CombatInstance.defaultCritMultiplier = node.getDouble("crit-multiplier", 2.0);
				CombatInstance.defaultMissChance = node.getDouble("miss-chance", 0.0);
				CombatInstance.defaultMissMultiplier = node.getDouble("miss-multiplier", 0.0);
			}
			
			jobsettings = openConfig(dataFolder,"job_settings","class configuration","job_settings");
			Configuration skillDefinitions = openConfig(new File(dataFolder,"definitions"), "passive", "skill definitions","definitions_passive");
			Configuration abilityDefinitions = openConfig(new File(dataFolder,"definitions"), "active", "skill definitions","definitions_active");
			Configuration jobDefinitions = openConfig(new File(dataFolder,"definitions"), "jobs", "job definitions","definitions_jobs");
			if (jobsettings == null || skillDefinitions == null || abilityDefinitions == null || jobDefinitions == null) {
				disable = true;
			} else {
				// skill settings
				
				// ability settings
				
				// job settings
			}
			
			// block xp settings
			BlockEventListener.groupBlockMapping = new HashMap<String, ArrayList<Integer>>();
			BlockEventListener.xpValues = new HashMap<String, Integer>();
			BlockEventListener.xpChances = new HashMap<String, Double>();
			for (Map.Entry<String, ConfigurationNode> group : Settings.advanced.getNodes("xp.blocks.groups").entrySet()) {
				String name = group.getKey();
				node = group.getValue();
				BlockEventListener.groupBlockMapping.put(name, (ArrayList<Integer>)node.getIntList("ids", null));
				BlockEventListener.xpValues.put(name, node.getInt("xp",0));
				BlockEventListener.xpChances.put(name, node.getDouble("chance",1.0));
			}
		} 
		// disable plugin if anything went wrong while loading configuration
		if (disable) {
			SRPG.output("disabling plugin");
			SRPG.pm.disablePlugin(SRPG.plugin);
		} else {
			SRPG.output("Successfully loaded config");
		}
	}
}
