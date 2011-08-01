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
	HashMap<Material, HashMap<Material,HashMap<String,Double>>> stats;
	HashMap<Material, HashMap<Material,HashMap<String,Double>>> timedStats;
	// TODO: add triggered effects
	boolean timedStatsDirty = true;
	
	public ProfileNPC () {
		stats = new HashMap<Material, HashMap<Material,HashMap<String,Double>>>();
		stats.put(null, new HashMap<Material,HashMap<String,Double>>());
		stats.get(null).put(null, new HashMap<String, Double>());
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
		SRPG.output("trying to get stat: "+name+",in hand: "+handMaterial+", target: "+targetMaterial);
		double value = 0.0;
		boolean found = false;
		if (handMaterial != null && stats.containsKey(handMaterial)) {
			if (stats.get(handMaterial).get(null).containsKey(name)) {
				value += stats.get(handMaterial).get(null).get(name);
				found = true;
			}
			if (targetMaterial != null && stats.get(handMaterial).containsKey(targetMaterial) && stats.get(handMaterial).get(targetMaterial).containsKey(name)) {
				value += stats.get(handMaterial).get(targetMaterial).get(name);
				found = true;
			}
		}
		if (targetMaterial != null && stats.get(null).containsKey(targetMaterial) && stats.get(null).get(targetMaterial).containsKey(name)) {
			value += stats.get(null).get(targetMaterial).get(name);
			found = true;
		}
		if (stats.get(null).get(null).containsKey(name)) {
			value += stats.get(null).get(null).get(name);
			found = true;
		}
		if (!found) {
			value = def;
		}
		SRPG.output("returning: "+value);
		return value;
	}
	
	public void recalculate() {
		recalculateBuffs();
	}
	
	public void recalculateBuffs() {
		if (!timedStatsDirty) {
			timedStats.clear();
			timedStats.put(null, new HashMap<Material, HashMap<String,Double>>());
			timedStats.get(null).put(null, new HashMap<String, Double>());
			addCollection(effects,timedStats);
			timedStatsDirty = false;
		}
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives) {
		addCollection(passives, 0, stats);
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives, HashMap<Material, HashMap<Material,HashMap<String,Double>>> statTarget) {
		addCollection(passives, 0, statTarget);
	}
	
	public void addCollection(HashMap<StructurePassive,EffectDescriptor> passives, Integer inheritance) {
		addCollection(passives, inheritance, stats);
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
					String name = node.getString("name");
					Double value = node.getDouble("value", 0.0) * inheritancefactor * descriptor.potency;
					if (node.getBoolean("level-based", false)) {
						value *= (double)descriptor.level / descriptor.maxlevel;
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
