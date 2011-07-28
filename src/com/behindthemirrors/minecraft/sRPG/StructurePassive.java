package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.util.config.ConfigurationNode;

public class StructurePassive {

	String name;
	String description;
	String replaces;
	HashMap<String,ConfigurationNode> effects;
	
	public StructurePassive(ConfigurationNode node) {
		name = node.getString("name");
		description = node.getString("description");
		replaces = node.getString("replaces");
		for (String effect : node.getKeys("effects")) {
			effects.put(effect, node.getNode("effects."+effect));
		}
	}

}
