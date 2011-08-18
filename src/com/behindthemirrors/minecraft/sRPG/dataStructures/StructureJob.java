package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.MiscBukkit;


public class StructureJob implements Comparable<StructureJob> {
	
	public static HashMap<Integer,String> ranks;
	public static double xp_base;
	public static double xp_offset;
	public static double level_coefficient;
	public static double level_exponent;
	public static double tier_coefficient;
	public static double tier_exponent;
	
	public String signature;
	public String name;
	String description;
	String details;
	public Integer maximumLevel;
	public HashMap<StructureJob,Integer> prerequisites;
	Integer tier;
	public HashMap<String,Double> defaults;
	// TODO: convert bonuses to StructurePassive
	public HashMap<StructurePassive,EffectDescriptor> traits;
	public HashMap<Integer,HashMap<StructurePassive,EffectDescriptor>> passives;
	public HashMap<Integer,HashMap<StructureActive,EffectDescriptor>> actives;
	
	public StructureJob (String uniqueName, ConfigurationNode root) {
		signature = uniqueName;
		name = root.getString("name");
		
		description = root.getString("description");
		details = root.getString("details");
		
		tier = root.getInt("tier",1);
		
		defaults = new HashMap<String, Double>();
		for (String stat : Settings.jobsettings.getKeys("settings.defaults")) {
			if (stat.equals("maximum-level")) {
				maximumLevel = root.getInt("defaults."+stat,Settings.jobsettings.getInt("settings.defaults."+stat, 1));
			} else {
				defaults.put(stat, root.getDouble("defaults."+stat,Settings.jobsettings.getDouble("settings.defaults."+stat, 0.0)));
			}
		}
		// add additional defaults for monsters
		if (root.getKeys("defaults") != null) {
			for (String stat : root.getKeys("defaults")) {
				if (!defaults.containsKey(stat)) {
					defaults.put(stat, root.getDouble("defaults."+stat,1));
				}
			}
		}
		
		traits = new HashMap<StructurePassive, EffectDescriptor>();
		for (String trait : root.getStringList("traits",new ArrayList<String>())) {
			// TODO: make NPE safe
			EffectDescriptor descriptor = new EffectDescriptor(trait,0,maximumLevel);
			traits.put(Settings.passives.get(MiscBukkit.stripPotency(trait)),descriptor);
		}
		
		passives = new HashMap<Integer, HashMap<StructurePassive,EffectDescriptor>>();
		if (root.getKeys("passives") != null) {
			for (String levelString : root.getKeys("passives")) {
				Integer level = Integer.parseInt(levelString.substring(levelString.indexOf(" ")+1));
				passives.put(level, new HashMap<StructurePassive, EffectDescriptor>());
				for (String passive : root.getStringList("passives."+levelString,new ArrayList<String>())) {
					// TODO: make NPE safe
					EffectDescriptor descriptor = new EffectDescriptor(passive,0,maximumLevel);
					if (Settings.passives.containsKey(MiscBukkit.stripPotency(passive))) {
						passives.get(level).put(Settings.passives.get(MiscBukkit.stripPotency(passive)), descriptor);
					} else {
						SRPG.output("Job "+name+" tried to load passive "+passive+" which is not available");
					}
				}
			}
		}
		
		actives = new HashMap<Integer, HashMap<StructureActive,EffectDescriptor>>();
		if (root.getKeys("actives") != null) {
			for (String levelString : root.getKeys("actives")) {
				Integer level = Integer.parseInt(levelString.substring(levelString.indexOf(" ")+1));
				actives.put(level, new HashMap<StructureActive, EffectDescriptor>());
				for (String active : root.getStringList("actives."+levelString,new ArrayList<String>())) {
					// TODO: make NPE safe
					EffectDescriptor descriptor = new EffectDescriptor(active,0,maximumLevel);
					actives.get(level).put(Settings.actives.get(MiscBukkit.stripPotency(active)),descriptor);
				}
			}
		}
	}
	
	public HashMap<StructurePassive,EffectDescriptor> getPassives(Integer currentLevel) {
		HashMap<StructurePassive,EffectDescriptor> available = new HashMap<StructurePassive,EffectDescriptor>();
		ArrayList<String> replaced = new ArrayList<String>();
		for (int i = 1; i <= 10; i++) {
			if (passives.containsKey(i)) {
				HashMap<StructurePassive,EffectDescriptor> map = passives.get(i);
				available.putAll(map);
				for (StructurePassive passive : map.keySet()) {
					if (passive.replaces != null) {
						replaced.add(passive.replaces);
					}
				}
			}
		}
		Iterator<StructurePassive> iterator = available.keySet().iterator();
		while (iterator.hasNext()) {
			if (replaced.contains(iterator.next().signature)) {
				iterator.remove();
			}
		}
		return available;
	}
	
	public HashMap<StructureActive,EffectDescriptor> getActives(Integer currentLevel) {
		HashMap<StructureActive,EffectDescriptor> available = new HashMap<StructureActive,EffectDescriptor>();
		ArrayList<String> replaced = new ArrayList<String>();
		for (int i = 1; i <= 10; i++) {
			if (actives.containsKey(i)) {
				HashMap<StructureActive,EffectDescriptor> map = actives.get(i);
				available.putAll(map);
				for (StructureActive active : map.keySet()) {
					if (active.replaces != null) {
						replaced.add(active.replaces);
					}
				}
			}
		}
		Iterator<StructureActive> iterator = available.keySet().iterator();
		while (iterator.hasNext()) {
			if (replaced.contains(iterator.next().signature)) {
				iterator.remove();
			}
		}
		return available;
	}
	
	ArrayList<StructureJob> getParents() {
		HashSet<StructureJob> parents = new HashSet<StructureJob>();
		for (StructureJob prereq : prerequisites.keySet()) {
			parents.add(prereq);
			parents.addAll(prereq.getParents());
		}
		return new ArrayList<StructureJob>(parents);
	}
	
	public boolean prerequisitesMet(ProfilePlayer profile) {
		if (prerequisites == null) {
			return true;
		}
		boolean result = true;
		StructureJob job;
		Integer level;
		for (Map.Entry<StructureJob,Integer> entry : prerequisites.entrySet()) {
			job = entry.getKey();
			level = entry.getValue();
			if (!(profile.jobLevels.containsKey(job) && level <= profile.jobLevels.get(job))) {
				result = false;
				break;
			}
		}
		return result;
	}
	
	public Integer xpToNextLevel(Integer currentLevel) {
		return (int)Math.round(xp_base * level_coefficient * Math.pow(currentLevel.doubleValue(), level_exponent) * tier_coefficient * Math.pow(tier.doubleValue(),tier_exponent) + currentLevel.doubleValue() * xp_offset * tier.doubleValue());
	}

	@Override
	public int compareTo(StructureJob other) {
		return name.compareTo(other.name);
	}
	
	@Override
	public String toString() {
		return signature;
		
	}
	
}
