package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

public class StructureActive implements Comparable<StructureActive> {

	String name;
	String signature;
	String description;
	String feedback;
	String broadcast;
	Double broadcastRange;
	Integer cost;
	Double cooldown;
	String replaces;
	HashMap<String,ConfigurationNode> effects;
	
	public StructureActive(String uniqueName, ConfigurationNode node) {
		signature = uniqueName;
		name = node.getString("name");
		description = node.getString("description");
		feedback = node.getString("feedback");
		broadcast = node.getString("broadcast");
		broadcastRange = node.getDouble("broadcast-range", 0.0);
		
		cost = node.getInt("cost",0);
		cooldown = node.getDouble("cooldown", 0);
		replaces = node.getString("replaces");
		effects = new HashMap<String, ConfigurationNode>();
		for (String effect : node.getKeys("effects")) {
			effects.put(effect, node.getNode("effects."+effect));
		}
	}

	public boolean activate(Player player, Material material) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int compareTo(StructureActive other) {
		return name.compareTo(other.name);
	}
}
