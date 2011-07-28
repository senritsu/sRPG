package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.util.config.ConfigurationNode;

public class StructureActive {

	String name;
	String description;
	String feedback;
	String broadcast;
	Double broadcastRange;
	Integer cost;
	Double cooldown;
	String replaces;
	HashMap<String,ConfigurationNode> effects;
	
	public StructureActive(ConfigurationNode node) {
		name = node.getString("name");
		description = node.getString("description");
		feedback = node.getString("feedback");
		broadcast = node.getString("broadcast");
		broadcastRange = node.getDouble("broadcast-range", 0.0);
		
		cost = node.getInt("cost",0);
		cooldown = node.getDouble("cooldown", 0);
		replaces = node.getString("replaces");
		for (String effect : node.getKeys("effects")) {
			effects.put(effect, node.getNode("effects."+effect));
		}
	}

}
