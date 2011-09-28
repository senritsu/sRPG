package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.MiscBukkit;

public class StructureActive implements Comparable<StructureActive> {

	public String name;
	public String signature;
	String description;
	String feedback;
	String broadcast;
	Double broadcastRange;
	public Integer cost;
	public Integer range;
	Double cooldown;
	String replaces;
	public HashMap<String,ConfigurationNode> effects;
	boolean combat;
	
	public ArrayList<Material> validMaterials;
	public ArrayList<Material> versusMaterials;
	
	public StructureActive(String uniqueName, ConfigurationNode node) {
		signature = uniqueName;
		name = node.getString("name");
		description = node.getString("description");
		feedback = node.getString("feedback");
		broadcast = node.getString("broadcast");
		broadcastRange = node.getDouble("broadcast-range", 0.0);
		
		cost = node.getInt("cost",0);
		range = node.getInt("range",5);
		cooldown = node.getDouble("cooldown", 0);
		replaces = node.getString("replaces");
		effects = new HashMap<String, ConfigurationNode>();
		if (node.getKeys("effects") != null) {
			for (String effect : node.getKeys("effects")) {
				effects.put(effect, node.getNode("effects."+effect));
			}
		}
		validMaterials = MiscBukkit.parseMaterialList(node.getStringList("tools", new ArrayList<String>()));
		versusMaterials = MiscBukkit.parseMaterialList(node.getStringList("versus", new ArrayList<String>()));
		
		combat = node.getBoolean("combat", false);
	}

	public boolean validVs(ProfileNPC profile) {
		if (versusMaterials.isEmpty() || 
				(profile instanceof ProfilePlayer && versusMaterials.contains( ((ProfilePlayer)profile).player.getItemInHand().getType() ))) {
			return true;
		}
		return false;
	}
	
	public boolean validVs(Material material) {
		if (versusMaterials.isEmpty() || versusMaterials.contains(material)) {
			return true;
		}
		return false;
	}
	
	@Override
	public int compareTo(StructureActive other) {
		return name.compareTo(other.name);
	}
	
	@Override
	public String toString() {
		return signature+" "+validMaterials.toString();
	}
	
}
