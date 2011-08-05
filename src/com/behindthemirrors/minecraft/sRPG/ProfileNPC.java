package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.config.ConfigurationNode;

public class ProfileNPC {

	LivingEntity entity;
	HashMap<StructurePassive,EffectDescriptor> effects = new HashMap<StructurePassive, EffectDescriptor>();
	ArrayList<HashMap<Material, HashMap<Material,HashMap<String,Double>>>> stats; // general stats at index 0, timed stats at index 1
	
	StructureJob currentJob;
	HashMap<StructureJob,Integer> jobLevels;

	// TODO: add triggered effects
	boolean timedStatsDirty = true;
	
	public ProfileNPC () {
		stats.add(new HashMap<Material, HashMap<Material,HashMap<String,Double>>>());
		stats.get(0).put(null, new HashMap<Material,HashMap<String,Double>>());
		stats.get(0).get(null).put(null, new HashMap<String, Double>());
	}
	
	public void addEffect(StructurePassive effect, EffectDescriptor descriptor) {
		if (effects.containsKey(effect)) {
			EffectDescriptor existent = effects.get(effect);
			if (existent.level < descriptor.level && existent.duration > descriptor.duration) {
				descriptor.duration = existent.duration;
			}
		} else {
			SRPG.timedEffectManager.add(this);
		}
		effects.put(effect, descriptor);
	}

	public int getStat(String name, Integer def) {
		return (int)getStat(name, (double)def);
	}
	
	public double getStat(String name) {
		return getStat(name, 0.0, null, null);
	}
	
	public double getStat(String name, Double def) {
		return getStat(name,def,null, null);
	}
	
	public double getStat(String name, Material handMaterial) {
		return getStat(name, 0.0, handMaterial, null);
	}
	
	public double getStat(String name, Material handMaterial, Material targetMaterial) {
		return getStat(name, 0.0, handMaterial, targetMaterial);
	}
	
	public int getStat(String name, Integer def, Material handMaterial, Material targetMaterial) {
		return (int)getStat(name, (double)def, handMaterial, targetMaterial);
	}
	
	public double getStat(String name, Double def, Material handMaterial, Material targetMaterial) {
		double value = 0.0;
		boolean found = false;
		for (HashMap<Material, HashMap<Material,HashMap<String,Double>>> map : stats) {
			if (handMaterial != null && map.containsKey(handMaterial)) {
				if (map.get(handMaterial).get(null).containsKey(name)) {
					value += map.get(handMaterial).get(null).get(name);
					found = true;
				}
				if (targetMaterial != null && map.get(handMaterial).containsKey(targetMaterial) && map.get(handMaterial).get(targetMaterial).containsKey(name)) {
					value += map.get(handMaterial).get(targetMaterial).get(name);
					found = true;
				}
			}
			if (targetMaterial != null && map.get(null).containsKey(targetMaterial) && map.get(null).get(targetMaterial).containsKey(name)) {
				value += map.get(null).get(targetMaterial).get(name);
				found = true;
			}
			if (map.get(null).get(null).containsKey(name)) {
				value += map.get(null).get(null).get(name);
				found = true;
			}
		}
		if (!found) {
			value = def;
		}
		return value;
	}
	
	public void recalculate() {
		stats.get(0).clear();
		stats.get(0).put(null, new HashMap<Material, HashMap<String,Double>>());
		stats.get(0).get(null).put(null, new HashMap<String, Double>());
		if (currentJob != null) {
			stats.get(0).get(null).get(null).putAll(currentJob.defaults);
			
			HashMap<StructurePassive,EffectDescriptor> current = new HashMap<StructurePassive, EffectDescriptor>();
			
			for (EffectDescriptor descriptor : currentJob.getPassives(jobLevels.get(currentJob)).values()) {
				descriptor.level = jobLevels.get(currentJob);
			}
			for (EffectDescriptor descriptor : currentJob.traits.values()) {
				descriptor.level = jobLevels.get(currentJob);
			}
			
			current.putAll(currentJob.getPassives(jobLevels.get(currentJob)));
			current.putAll(currentJob.traits);
			
			addCollection(current);
		}
		recalculateBuffs();
	}
	
	public void recalculateBuffs() {
		if (!timedStatsDirty) {
			stats.get(1).clear();
			stats.get(1).put(null, new HashMap<Material, HashMap<String,Double>>());
			stats.get(1).get(null).put(null, new HashMap<String, Double>());
			addCollection(effects,stats.get(1));
			timedStatsDirty = false;
		}
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives) {
		addCollection(passives, 0, stats.get(0));
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives, HashMap<Material, HashMap<Material,HashMap<String,Double>>> statTarget) {
		addCollection(passives, 0, statTarget);
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives, Integer inheritance) {
		addCollection(passives, inheritance, stats.get(0));
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives, Integer inheritance, HashMap<Material, HashMap<Material,HashMap<String,Double>>> statTarget) {
		Double inheritancefactor = Settings.jobsettings.getDouble("settings.inheritance."+(new String[] {"","both","parent","mastered"})[inheritance], 1);
		for (Map.Entry<StructurePassive,EffectDescriptor> entry : passives.entrySet()) {
			StructurePassive passive = entry.getKey();
			EffectDescriptor descriptor = entry.getValue();
			SRPG.output("adding "+passive.signature+" with potency "+descriptor.potency);
			for (Map.Entry<String, ConfigurationNode> detailsEntry : passive.effects.entrySet()) {
				String effect = detailsEntry.getKey();
				ConfigurationNode node = detailsEntry.getValue();
				if (inheritance > 0 && !node.getBoolean("inherited", false)) {
					continue;
				}
				if (effect.startsWith("boost")) {
					ArrayList<String> levelbased = (ArrayList<String>) node.getStringList("level-based", new ArrayList<String>());
					String name = node.getString("name");
					Double value = node.getDouble("value", 0.0) * inheritancefactor * descriptor.potency;
					if (levelbased.contains("value")) {
						value *= (double)(descriptor.level == null ? 0 : descriptor.level) / descriptor.maxlevel;
					}
					// parse tools node
					ArrayList<Material> tools = Utility.parseMaterialList(node.getStringList("tools", new ArrayList<String>()));
					// parse versus node
					ArrayList<Material> versus = Utility.parseMaterialList(node.getStringList("versus", new ArrayList<String>()));
					for (Material tool : tools) {
						if (!statTarget.containsKey(tool)) {
							statTarget.put(tool, new HashMap<Material, HashMap<String,Double>>());
						}
						for (Material vs : versus) {
							if (!statTarget.get(tool).containsKey(vs)) {
								statTarget.get(tool).put(vs,new HashMap<String, Double>());
							}
							statTarget.get(tool).get(vs).put(
									name, statTarget.get(tool).get(vs).containsKey(name) ? 
									statTarget.get(tool).get(vs).get(name) + value : 
									value);
						}
					}
				}
			}
		}
	}
	
}
