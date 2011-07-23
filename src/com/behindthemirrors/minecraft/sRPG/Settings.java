package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Settings {
	
	static String difficulty;
	
	static File dataFolder;
	static Configuration config;
	static Configuration advanced;
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
	static HashMap<Material,Material> BLOCK_DROPS = new HashMap<Material, Material>();
	static HashMap<Material,int[]> BLOCK_DROP_AMOUNTS = new HashMap<Material, int[]>();
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
		// initialize block drop amounts and drop materials
		BLOCK_DROPS.put(Material.GRASS, Material.DIRT);
		BLOCK_DROPS.put(Material.STONE, Material.COBBLESTONE);
		BLOCK_DROPS.put(Material.COAL_ORE, Material.COAL);
		BLOCK_DROPS.put(Material.LAPIS_ORE, Material.getMaterial(351));
		BLOCK_DROP_AMOUNTS.put(Material.getMaterial(351), new int[] {4,4});
		BLOCK_DROPS.put(Material.DIAMOND_ORE, Material.DIAMOND);
		BLOCK_DROPS.put(Material.REDSTONE_ORE, Material.REDSTONE);
		BLOCK_DROP_AMOUNTS.put(Material.REDSTONE, new int[] {4,1});
		// initialize tool mining multidrop whitelist
		MULTIDROP_VALID_BLOCKS.put(Material.WOOD_PICKAXE,new ArrayList<Material>(Arrays.asList(new Material[] {Material.STONE,Material.COBBLESTONE,Material.MOSSY_COBBLESTONE,Material.SANDSTONE, Material.COAL_ORE})));
		MULTIDROP_VALID_BLOCKS.put(Material.STONE_PICKAXE,new ArrayList<Material>(Arrays.asList(new Material[] {Material.STONE,Material.COBBLESTONE,Material.MOSSY_COBBLESTONE,Material.SANDSTONE, Material.COAL_ORE,Material.LAPIS_ORE,Material.IRON_ORE})));
		MULTIDROP_VALID_BLOCKS.put(Material.IRON_PICKAXE,new ArrayList<Material>(Arrays.asList(new Material[] {Material.STONE,Material.COBBLESTONE,Material.MOSSY_COBBLESTONE,Material.SANDSTONE, Material.COAL_ORE,Material.LAPIS_ORE,Material.IRON_ORE,Material.DIAMOND_ORE,Material.GOLD_ORE,Material.REDSTONE_ORE})));
		MULTIDROP_VALID_BLOCKS.put(Material.DIAMOND_PICKAXE,new ArrayList<Material>(Arrays.asList(new Material[] {Material.STONE,Material.COBBLESTONE,Material.MOSSY_COBBLESTONE,Material.SANDSTONE, Material.COAL_ORE,Material.LAPIS_ORE,Material.IRON_ORE,Material.DIAMOND_ORE,Material.GOLD_ORE,Material.REDSTONE_ORE, Material.OBSIDIAN})));
		ArrayList<Material> log = new ArrayList<Material>();
		log.add(Material.LOG);
		MULTIDROP_VALID_BLOCKS.put(Material.WOOD_AXE,log);
		MULTIDROP_VALID_BLOCKS.put(Material.STONE_AXE,log);
		MULTIDROP_VALID_BLOCKS.put(Material.IRON_AXE,log);
		MULTIDROP_VALID_BLOCKS.put(Material.DIAMOND_AXE,log);
		// initialize color tag replacement map
		colorMap.put("[aqua]", ChatColor.AQUA.toString());
		colorMap.put("[black]", ChatColor.BLACK.toString());
		colorMap.put("[blue]", ChatColor.BLUE.toString());
		colorMap.put("[dark_aqua]", ChatColor.DARK_AQUA.toString());
		colorMap.put("[dark_blue]", ChatColor.DARK_BLUE.toString());
		colorMap.put("[dark_gray]", ChatColor.DARK_GRAY.toString());
		colorMap.put("[dark_green]", ChatColor.DARK_GREEN.toString());
		colorMap.put("[dark_purple]", ChatColor.DARK_PURPLE.toString());
		colorMap.put("[dark_red]", ChatColor.DARK_RED.toString());
		colorMap.put("[gold]", ChatColor.GOLD.toString());
		colorMap.put("[gray]", ChatColor.GRAY.toString());
		colorMap.put("[green]", ChatColor.GREEN.toString());
		colorMap.put("[light_purple]", ChatColor.LIGHT_PURPLE.toString());
		colorMap.put("[red]", ChatColor.RED.toString());
		colorMap.put("[white]", ChatColor.WHITE.toString());
		colorMap.put("[yellow]", ChatColor.YELLOW.toString());
	}
	
	public void load() {
		// get config data
		Boolean disable = false;
		File file = Utility.createDefaultFile(new File(dataFolder,"config.yml"),"basic configuration");
		if (file.exists()){
			config = new Configuration(file);
			config.load();
		} else {
			SRPG.output("Error loading config file");
			disable = true;
		}
		file = Utility.createDefaultFile(new File(dataFolder,"config_advanced.yml"),"advanced configuration");
		if (file.exists()){
			advanced = new Configuration(file);
			advanced.load();
		} else {
			SRPG.output("Error loading advanced config file");
			disable = true;
		}
		file = Utility.createDefaultFile(new File(dataFolder,"localization_EN.yml"),"localization data");
		if (!file.exists()) {
			SRPG.output("Error loading default localization file");
			disable = true;
		}
		if (!disable) {
			// read locale data
			defaultLocale = config.getString("settings.locales.default");
			localization = new HashMap<String,Configuration>();
			nameReplacements = new HashMap<String, HashMap<String,String>>();
			SKILLS_ALIASES = new HashMap<String, ArrayList<String>>();
			for (String locale : config.getStringList("settings.locales.available",new ArrayList<String>())) {
				file = new File(dataFolder,"localization_"+locale+".yml");
				if (file.exists()){
					localization.put(locale,new Configuration(file));
					localization.get(locale).load();
					
					// generate name replacement hashmap
					nameReplacements.put(locale,new HashMap<String, String>());
					for (String path : walk(localization.get(locale),"")) {
						String value = localization.get(locale).getString(path,"");
						if (!value.isEmpty()) {
							nameReplacements.get(locale).put(path,value);
						}
					}
					// update skill aliases
					SKILLS_ALIASES.put(locale,new ArrayList<String>());
					for (String skillname : SKILLS) {
						SKILLS_ALIASES.get(locale).add(localization.get(locale).getString("skills."+skillname).toLowerCase());
					}
				
				} else {
					disable = true;
					SRPG.output("Error loading localization file for locale " + locale);
				}
			}
		
			// update player locales if set locales are not available anymore
			for (Player player : SRPG.playerDataManager.players.keySet()) {
				PlayerData data = SRPG.playerDataManager.get(player);
				if (!Settings.localization.containsKey(data.locale)) {
					data.locale = defaultLocale;
					SRPG.playerDataManager.save(player,"locale");
					SRPG.output("changed locale for player "+data.name+" to default");
				}
			}
			
			// read config data
			SRPG.database.mySQLenabled = config.getBoolean("mySQL.enabled", false);
			if (SRPG.database.mySQLenabled) {
				SRPG.database.dbServer = config.getString("mySQL.server");
				SRPG.database.dbPort = config.getString("mySQL.port");
				SRPG.database.dbName = config.getString("mySQL.dbName");
				SRPG.database.dbUser = config.getString("mySQL.dbUser");
				SRPG.database.dbPass = config.getString("mySQL.dbPass");
				SRPG.database.dbTablePrefix = config.getString("mySQL.table_prefix");
			}
			
			// read xp settings
			PlayerData.xpToLevel = advanced.getInt("xp.to-levelup", 1000);
			// read skill settings
			PlayerData.focusBase = advanced.getInt("skills.costs.focus-base", 1);
			PlayerData.focusIncrease = advanced.getInt("skills.costs.focus-increase", 1);
			PlayerData.skillCosts = (ArrayList<Integer>)advanced.getIntList("skills.costs.generic", null);
			PlayerData.milestoneRequirements = new HashMap<Integer, String>();
			for (String milestone : new String[] {"novice","apprentice","expert","master"}) {
				PlayerData.milestoneRequirements.put(advanced.getInt("skills.milestones."+milestone, 0), milestone);
			}
			// read ability settings
			PlayerData.chargeMax = advanced.getInt("abilities.max-charges", 1);
			PlayerData.chargeTicks = advanced.getInt("abilities.blocks-to-charge", 1);
			PlayerData.abilityCosts = new HashMap<String, Integer>();
			for (String tool : Settings.TOOL_MATERIAL_TO_STRING.values()) {
				int cost = advanced.getInt("abilities.costs."+tool,-1);
				if (cost >= 0) {
					PlayerData.abilityCosts.put(tool, cost);
				}
			}
			
			// read combat settings
			// difficulty
			difficulty = config.getString("settings.combat.difficulty");
			// damage increase with depth
			DamageEventListener.increaseDamageWithDepth = config.getBoolean("settings.combat.dangerous-depths", false);
			
			DamageEventListener.depthTiers = new ArrayList<int[]>();
			for (int i = 0;i < advanced.getIntList("combat.damage.depth.thresholds", null).size();i++) {
				DamageEventListener.depthTiers.add(new int[] {advanced.getIntList("combat.damage.depth.thresholds", null).get(i),
															  advanced.getIntList("combat.damage.depth.damage-increase", null).get(i) });
			}
			// animal health and xp
			SpawnEventListener.healthTableCreatures = new HashMap<String, Integer>();
			DamageEventListener.xpTableCreatures = new HashMap<String, Integer>();
			for (String animal : Settings.ANIMALS) {
				SpawnEventListener.healthTableCreatures.put(animal, advanced.getInt("stats."+difficulty+".animals."+animal+".health", 1));
				DamageEventListener.xpTableCreatures.put(animal, advanced.getInt("stats."+difficulty+".animals."+animal+".xp", 0));
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
				SpawnEventListener.healthTableCreatures.put(monster, advanced.getInt("stats."+difficulty+".monsters."+monster+".health", 1));
				DamageEventListener.damageTableMonsters.put(monster, advanced.getInt("stats."+difficulty+".monsters."+monster+".damage", 1));
				DamageEventListener.xpTableCreatures.put(monster, advanced.getInt("stats."+difficulty+".monsters."+monster+".xp", 0));
			}
			
			// tool damage
			DamageEventListener.damageTableTools = new HashMap<String, Integer>();
			for (String tool : Settings.TOOL_MATERIAL_TO_STRING.values()) {
				if (!advanced.getBoolean("stats."+difficulty+".tools."+tool+".override", false)) {
					DamageEventListener.damageTableTools.put(tool, advanced.getInt("stats."+difficulty+".tools."+tool+".damage", 1));
				}
			}
			if (!advanced.getBoolean("stats."+difficulty+".tools.bow.override", false)) {
				DamageEventListener.damageTableTools.put("bow",advanced.getInt("stats."+difficulty+".tools.bow.damage", 1));
			}
			if (!advanced.getBoolean("stats."+difficulty+".tools.fists.override", false)) {
				DamageEventListener.damageTableTools.put("fists",advanced.getInt("stats."+difficulty+".tools.fists.damage", 1));
			}
			// critical hit settings
			DamageEventListener.critChance = advanced.getDouble("combat.crit-chance", 1.0);
			DamageEventListener.critMultiplier = advanced.getDouble("combat.crit-multiplier", 1.0);
			
			// block xp settings
			BlockEventListener.groupBlockMapping = new HashMap<String, ArrayList<Integer>>();
			BlockEventListener.xpValues = new HashMap<String, Integer>();
			BlockEventListener.xpChances = new HashMap<String, Double>();
			for (String group : Settings.advanced.getStringList("xp.blocks.xp-groups", null)) {
				if (group != Settings.advanced.getString("xp.blocks.default-group")) {
					BlockEventListener.groupBlockMapping.put(group, (ArrayList<Integer>)advanced.getIntList("xp.blocks."+group+".ids", null));
				}
				BlockEventListener.xpValues.put(group, advanced.getInt("xp.blocks."+group+".xp",0));
				BlockEventListener.xpChances.put(group, advanced.getDouble("xp.blocks."+group+".chance",1.0));
			}
			
			SRPG.output("Successfully loaded config");
		} else {
			SRPG.pm.disablePlugin(SRPG.plugin);
		}
	}
		
	private ArrayList<String> walk(Configuration config, String path) {
		ArrayList<String> list = new ArrayList<String>();
		List<String> index = config.getStringList((path=="")?"index":path+".index",null);
		if (index != null && !index.isEmpty()) {
			for (String entry : index) {
				list.addAll(walk(config, (path=="")?entry:path+"."+entry));
			}
		} else {
			list.add(path);
		}
		return list;
	}
}
