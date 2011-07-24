package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.entity.LivingEntity;

public class ProfileNPC {

	LivingEntity entity;
	HashMap<String, Integer> effectCounters = new HashMap<String, Integer>();
	
	public void addEffect(String name, Integer duration) {
		if (!(effectCounters.containsKey(name) && effectCounters.get(name) > duration)) {
			effectCounters.put(name, duration);
			SRPG.timedEffectManager.add(this);
		}
	}

}
