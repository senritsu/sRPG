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
import java.util.HashSet;
import java.util.Map;

public class Settings {
	
	static String difficulty;
	
	static File dataFolder;
	
	static boolean mySQLenabled;
	
	static Configuration config;
	static Configuration advanced;
	static Configuration jobsettings;
	
	static HashMap<String,StructureActive> actives;
	static HashMap<String,StructurePassive> passives;
	static HashMap<String,StructureJob> jobs;
	static HashMap<String,StructureJob> mobs;
	static ArrayList<StructureJob> initialJobs;
	
	static HashMap<String,Configuration> localization;
	static String defaultLocale;
	
	static HashMap<String, HashMap<String, String>> JOB_ALIASES;
	static ArrayList<String> SKILLS = new ArrayList<String>(Arrays.asList(new String[] {"swords","axes","pickaxes","shovels","hoes","bow","ukemi","evasion", "focus"}));
	static ArrayList<String> TOOLS = new ArrayList<String>(Arrays.asList(new String[] {"swords","pickaxes","axes","shovels","hoes"}));
	static ArrayList<String> GRADES =  new ArrayList<String>(Arrays.asList(new String[] {"wood","stone","iron","gold","diamond"}));
	
	static Material[] TOOL_MATERIALS = {Material.WOOD_SWORD,Material.STONE_SWORD,Material.IRON_SWORD,Material.GOLD_SWORD, Material.DIAMOND_SWORD,
									    Material.WOOD_PICKAXE,Material.STONE_PICKAXE,Material.IRON_PICKAXE,Material.GOLD_PICKAXE,Material.DIAMOND_PICKAXE,
									    Material.WOOD_AXE,Material.STONE_AXE,Material.IRON_AXE,Material.GOLD_AXE,Material.DIAMOND_AXE,
									    Material.WOOD_SPADE,Material.STONE_SPADE,Material.IRON_SPADE,Material.GOLD_SPADE,Material.DIAMOND_SPADE,
									    Material.WOOD_HOE,Material.STONE_HOE,Material.IRON_HOE,Material.GOLD_HOE,Material.DIAMOND_HOE};
	static HashMap<Material,String> TOOL_MATERIAL_TO_STRING = new HashMap<Material,String>();
	static HashMap<Material,String> TOOL_MATERIAL_TO_TOOL_GROUP = new HashMap<Material,String>();
	static {
		// initialize Material to string mappings
		for (int i = 0; i < TOOLS.size(); i++) {
			int length = GRADES.size();
			for (int j = 0; j < length; j++) {
				TOOL_MATERIAL_TO_STRING.put(TOOL_MATERIALS[i*length+j], TOOLS.get(i)+"."+GRADES.get(j));
				TOOL_MATERIAL_TO_TOOL_GROUP.put(TOOL_MATERIALS[i*length+j], TOOLS.get(i));
			}
		}
		TOOL_MATERIAL_TO_TOOL_GROUP.put(Material.BOW,"bow");
	}
	static HashMap<Material,ArrayList<Material>> MULTIDROP_VALID_BLOCKS = new HashMap<Material, ArrayList<Material>>();
	static {
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
	}
	static ArrayList<Material> BLOCK_CLICK_BLACKLIST = new ArrayList<Material>(Arrays.asList(new Material[] {Material.BED,
															Material.BED_BLOCK,Material.DISPENSER,Material.FURNACE,Material.BURNING_FURNACE,Material.JUKEBOX,
															Material.NOTE_BLOCK,Material.STORAGE_MINECART,Material.WOOD_DOOR,
															Material.WOODEN_DOOR,Material.CHEST,Material.WORKBENCH,Material.TNT,
															Material.MINECART,Material.BOAT,Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.TRAP_DOOR}));
	
	static HashMap<String,String> colorMap = new HashMap<String, String>();
	static {
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
	
	static HashMap<String, HashMap<String, String>> nameReplacements;

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
			JOB_ALIASES = new HashMap<String, HashMap<String,String>>();
			
			// create plugin default locale file
			for (String name : new String[] {"EN"}) {
				Utility.createDefaultFile(new File(new File(dataFolder,"locales"),name+".yml"), "'"+name+"' locale settings", "locale"+name+".yml");
			}
			// create plugin default difficulties
			for (String name : new String[] {"default","original"}) {
				Utility.createDefaultFile(new File(new File(dataFolder,"difficulties"),name+".yml"), "'"+name+"' difficulty settings", "difficulty_"+name+".yml");
				Utility.createDefaultFile(new File(new File(dataFolder,"difficulties"),name+"_mobs.yml"), "'"+name+"' mob settings", "definitions_mobs_"+name+".yml");
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
					JOB_ALIASES.put(locale,new HashMap<String,String>());
					if (localization.get(locale).getKeys("jobs") != null) {
						for (String name : localization.get(locale).getKeys("jobs")) {
							JOB_ALIASES.get(locale).put(localization.get(locale).getString("jobs."+name).toLowerCase(),name);
						}
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
			mySQLenabled = node.getBoolean("enabled", false);
			Database db = new Database();
			if (mySQLenabled) {
				db.server = node.getString("server");
				db.port = node.getString("port");
				db.name = node.getString("dbName");
				db.user = node.getString("dbUser");
				db.pass = node.getString("dbPass");
			}
			db.tablePrefix = node.getString("table_prefix");
			SRPG.database = db;
			
			// read ability settings
			ProfilePlayer.chargeMax = advanced.getInt("abilities.max-charges", 1);
			ProfilePlayer.chargeTicks = advanced.getInt("abilities.blocks-to-charge", 1);
			
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
				SpawnEventListener.dangerousDepths = config.getBoolean("settings.combat.dangerous-depths", false);
				
				SpawnEventListener.depthTiers = new ArrayList<int[]>();
				ArrayList<Integer> thresholds = (ArrayList<Integer>) difficultyConfig.getIntList("settings.dangerous-depths.thresholds", null);
				ArrayList<Integer> levelincrease = (ArrayList<Integer>) difficultyConfig.getIntList("settings.dangerous-depths.level-increase", null);
				if (thresholds.size() == levelincrease.size()) {
					for (int i = 0;i < thresholds.size();i++) {
						SpawnEventListener.depthTiers.add(new int[] {thresholds.get(i),levelincrease.get(i)});
					}
				} else {
					SRPG.output("Warning: Invalid depth settings in difficulty config");
				}
				
				// tool damage
				CombatInstance.damageTableTools = new HashMap<String, Integer>();
				node = difficultyConfig.getNode("stats.tools");
				for (String tool : Settings.TOOL_MATERIAL_TO_STRING.values()) {
					if (!node.getBoolean(tool+".override", false)) {
						CombatInstance.damageTableTools.put(tool, node.getInt(tool+".damage", 1));
					}
				}
				if (!node.getBoolean("bow.override", false)) {
					CombatInstance.damageTableTools.put("bow",node.getInt("bow.damage", 1));
				}
			}
			
			jobsettings = openConfig(dataFolder,"job_settings","class configuration","job_settings");
			Configuration passiveDefinitions = openConfig(new File(dataFolder,"definitions"), "passive", "skill definitions","definitions_passive");
			Configuration activeDefinitions = openConfig(new File(dataFolder,"definitions"), "active", "ability definitions","definitions_active");
			Configuration jobDefinitions = openConfig(new File(dataFolder,"definitions"), "jobs", "job definitions","definitions_jobs");
			Configuration mobDefinitions = openConfig(new File(dataFolder,"difficulties"), difficulty+"_mobs", "'"+difficulty+"'mob definitions","definitions_mobs_default");
			if (jobsettings == null || passiveDefinitions == null || activeDefinitions == null || jobDefinitions == null || mobDefinitions == null) {
				disable = true;
			} else {
				// load job xp formula
				StructureJob.xp_base = jobsettings.getDouble("settings.xp.base", 1000);
				StructureJob.xp_offset = jobsettings.getDouble("settings.xp.offset", 0);
				StructureJob.level_coefficient = jobsettings.getDouble("settings.xp.level-coefficient", 1);
				StructureJob.level_exponent = jobsettings.getDouble("settings.xp.level-exponent", 1);
				StructureJob.tier_coefficient = jobsettings.getDouble("settings.xp.tier-coefficient", 1);
				StructureJob.tier_exponent = jobsettings.getDouble("settings.xp.tier-exponent", 1);
				// load job prefixes
				StructureJob.ranks = new HashMap<Integer, String>();
				if (jobsettings.getKeys("job-prefixes") != null) {
					for (String prefix : jobsettings.getKeys("job-prefixes")) {
						StructureJob.ranks.put(Integer.parseInt(prefix.substring(prefix.indexOf(" ")+1)), jobsettings.getString("job-prefixes."+prefix));
					}
				}
				// load skill definitions
				passives = new HashMap<String, StructurePassive>();
				for (String name : passiveDefinitions.getKeys()) {
					passives.put(name, new StructurePassive(name,passiveDefinitions.getNode(name)));
				}
				SRPG.output("loaded "+(new Integer(passives.size())).toString()+" "+Utility.parseSingularPlural(localization.get(defaultLocale).getString("terminology.passive"),passives.size()));
				
				// load ability definitions
				actives = new HashMap<String, StructureActive>();
				for (String name : activeDefinitions.getKeys()) {
					actives.put(name, new StructureActive(name,activeDefinitions.getNode(name)));
				}
				SRPG.output("loaded "+(new Integer(actives.size())).toString()+" "+Utility.parseSingularPlural(localization.get(defaultLocale).getString("terminology.active"),actives.size()));
				
				// load job definitions
				jobs = new HashMap<String, StructureJob>();
				for (String name : jobDefinitions.getKeys()) {
					if (jobsettings.getKeys("tree").contains(name) && jobDefinitions.getBoolean(name+".enabled", true)) {
						jobs.put(name, new StructureJob(name,jobDefinitions.getNode(name)));
						
						// load job prerequisites from jobtree
						jobs.get(name).prerequisites = new HashMap<StructureJob, Integer>();
						if (jobsettings.getKeys("tree."+name+".prerequisites")!= null) {
							for (String prereq : jobsettings.getKeys("tree."+name+".prerequisites")) {
								jobs.get(name).prerequisites.put(jobs.get(prereq), jobsettings.getInt("tree."+name+".prerequisites."+prereq, 1));
							}
						} 
					}
				}
				// disable all jobs with missing prerequisites
				ArrayList<String> deactivate = new ArrayList<String>();
				for (String name : jobs.keySet()) {
					for (StructureJob job : jobs.get(name).prerequisites.keySet()) {
						if (!jobs.containsKey(job.signature)) {
							deactivate.add(name);
							deactivate.addAll(Utility.getChildren(jobs, name));
							break;
						}
					}
				}
				deactivate = new ArrayList<String>(new HashSet<String>(deactivate));
				for (String name : deactivate) {
					jobs.remove(name);
				}
				// populate default job list
				initialJobs = new ArrayList<StructureJob>();
				for (StructureJob job : jobs.values()) {
					if (job.prerequisites.isEmpty()){
						initialJobs.add(job);
					}
				}
				
				// load mobs
				mobs = new HashMap<String, StructureJob>();
				for (String name : mobDefinitions.getKeys()) {
					mobs.put(name, new StructureJob(name,mobDefinitions.getNode(name)));
				}
				
				// status report
				SRPG.output("loaded "+(new Integer(jobs.size())).toString()+" "+Utility.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"),jobs.size()));
				if (deactivate.size() > 0) {
					SRPG.output((new Integer(deactivate.size())).toString()+" "+Utility.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"),deactivate.size())+" could not be loaded due to missing prerequisites");
				}
				if (jobs.isEmpty()) {
					SRPG.output(Utility.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"), 1)+" tree is empty!");
					disable = true;
				} else if (initialJobs.isEmpty()) {
					SRPG.output("No "+Utility.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"), 1)+" without prerequisites available in the job tree!");
					disable = true;
				}
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
