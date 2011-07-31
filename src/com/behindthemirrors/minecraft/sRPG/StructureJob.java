package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.util.config.ConfigurationNode;

public class StructureJob implements Comparable<StructureJob> {
	
	static HashMap<Integer,String> ranks;
	static double xp_base;
	static double xp_offset;
	static double level_coefficient;
	static double level_exponent;
	static double tier_coefficient;
	static double tier_exponent;
	
	String signature;
	String name;
	String description;
	String details;
	Integer maximumLevel;
	HashMap<String,Integer> prerequisites;
	Integer tier;
	HashMap<String,Double> baseStats;
	HashMap<String,ConfigurationNode> bonuses;
	HashMap<Integer,ArrayList<StructurePassive>> passives;
	HashMap<Integer,ArrayList<StructureActive>> actives;
	
	public StructureJob (String uniqueName, ConfigurationNode root) {
		signature = uniqueName;
		name = root.getString("name");
		
		description = root.getString("description");
		details = root.getString("details");
		
		tier = root.getInt("tier",1);
		
		baseStats = new HashMap<String, Double>();
		for (String stat : Settings.jobsettings.getKeys("settings.defaults")) {
			if (stat.equals("maximum-level")) {
				maximumLevel = root.getInt("maximum-level",Settings.jobsettings.getInt("settings.defaults.maximum-level", 1));
			} else {
				baseStats.put(stat, root.getDouble("defaults."+stat,Settings.jobsettings.getDouble("settings.defaults."+stat, 0.0)));
			}
		}
		
		bonuses = new HashMap<String, ConfigurationNode>();
		if (root.getKeys("bonuses") != null) {
			for (String bonus : root.getKeys("bonuses")) {
				bonuses.put(bonus, root.getNode("bonuses."+bonus));
			}
		}
		
		passives = new HashMap<Integer, ArrayList<StructurePassive>>();
		if (root.getKeys("passives") != null) {
			for (String levelString : root.getKeys("passives")) {
				Integer level = Integer.parseInt(levelString.substring(levelString.indexOf(" ")));
				passives.put(level, new ArrayList<StructurePassive>());
				for (String passive : root.getKeys("passives."+levelString)) {
					passives.get(level).add(new StructurePassive(passive,root.getNode("passives."+levelString+"."+passive)));
				}
			}
		}
		
		actives = new HashMap<Integer, ArrayList<StructureActive>>();
		if (root.getKeys("actives") != null) {
			for (String levelString : root.getKeys("actives")) {
				Integer level = Integer.parseInt(levelString.substring(levelString.indexOf(" ")));
				actives.put(level, new ArrayList<StructureActive>());
				for (String active : root.getKeys("actives."+levelString)) {
					actives.get(level).add(new StructureActive(active,root.getNode("actives."+levelString+"."+active)));
				}
			}
		}
	}
	
	ArrayList<StructurePassive> getPassives(Integer currentLevel) {
		ArrayList<StructurePassive> available = new ArrayList<StructurePassive>();
		ArrayList<String> replaced = new ArrayList<String>();
		for (int i = 1; i <= 10; i++) {
			if (passives.containsKey(i)) {
				ArrayList<StructurePassive> list = passives.get(i);
				available.addAll(list);
				for (StructurePassive passive : list) {
					if (passive.replaces != null) {
						replaced.add(passive.replaces);
					}
				}
			}
		}
		Iterator<StructurePassive> iterator = available.iterator();
		while (iterator.hasNext()) {
			if (replaced.contains(iterator.next().signature)) {
				iterator.remove();
			}
		}
		return available;
	}
	
	ArrayList<StructureActive> getActives(Integer currentLevel) {
		ArrayList<StructureActive> available = new ArrayList<StructureActive>();
		ArrayList<String> replaced = new ArrayList<String>();
		for (int i = 1; i <= 10; i++) {
			if (actives.containsKey(i)) {
				ArrayList<StructureActive> list = actives.get(i);
				available.addAll(list);
				for (StructureActive active : list) {
					if (active.replaces != null) {
						replaced.add(active.replaces);
					}
				}
			}
		}
		Iterator<StructureActive> iterator = available.iterator();
		while (iterator.hasNext()) {
			if (replaced.contains(iterator.next().signature)) {
				iterator.remove();
			}
		}
		return available;
	}
	
	boolean prerequisitesMet(ProfilePlayer profile) {
		if (prerequisites == null) {
			return true;
		}
		boolean result = true;
		String job;
		Integer level;
		for (Map.Entry<String,Integer> entry : prerequisites.entrySet()) {
			job = entry.getKey();
			level = entry.getValue();
			if (!(profile.jobLevels.containsKey(job) && level <= profile.jobLevels.get(job))) {
				result = false;
				break;
			}
		}
		return result;
	}
	
	Integer xpToNextLevel(Integer currentLevel) {
		return (int)Math.round(xp_base * level_coefficient * Math.pow(currentLevel.doubleValue(), level_exponent) * tier_coefficient * Math.pow(tier.doubleValue(),tier_exponent) + currentLevel.doubleValue() * xp_offset * tier.doubleValue());
	}

	@Override
	public int compareTo(StructureJob other) {
		return name.compareTo(other.name);
	}
	
}
