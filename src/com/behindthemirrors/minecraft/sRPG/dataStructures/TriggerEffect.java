package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;

import org.bukkit.util.config.ConfigurationNode;

public class TriggerEffect {
	
	public ArrayList<String> triggers;
	public ConfigurationNode node;
	public EffectDescriptor descriptor;
	
	public TriggerEffect(ConfigurationNode node, EffectDescriptor descriptor) {
		this.node = node;
		this.descriptor = descriptor;
		triggers = (ArrayList<String>) node.getStringList("triggers", new ArrayList<String>());
	}
	
	public String toString() {
		return "Trigger: "+node.getString("action")+" ["+triggers.toString()+"]";
	}
}
