package com.behindthemirrors.minecraft.sRPG.dataStructures;

import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.ResolverActive;

public class ScheduledEffect {
	
	String effect;
	ConfigurationNode node;
	ArgumentsActive arguments;
	public int ticksToActivation;
	
	public ScheduledEffect(Integer delay, String effect, ConfigurationNode node, ArgumentsActive arguments) {
		this.effect = effect;
		this.node = node;
		this.arguments = arguments;
		ticksToActivation = delay;
	}
	
	public void activate() {
		ResolverActive.resolveActiveEffect(effect, node, arguments);
	}
	
	@Override
	public String toString() {
		return effect+" ("+ticksToActivation+")";
	}
}
