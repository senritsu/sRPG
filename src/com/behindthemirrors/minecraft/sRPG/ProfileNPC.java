package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

public class ProfileNPC {

	LivingEntity entity;
	HashMap<String, Integer> effectCounters = new HashMap<String, Integer>();
	HashMap<Material, HashMap<Material,HashMap<String,Double>>> stats;
	
	public ProfileNPC () {
		stats = new HashMap<Material, HashMap<Material,HashMap<String,Double>>>();
		stats.put(null, new HashMap<Material,HashMap<String,Double>>());
		stats.get(null).put(null, new HashMap<String, Double>());
	}
	
	public void addEffect(String name, Integer duration) {
		if (!(effectCounters.containsKey(name) && effectCounters.get(name) > duration)) {
			effectCounters.put(name, duration);
			SRPG.timedEffectManager.add(this);
		}
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
		return value;
	}
	
}
