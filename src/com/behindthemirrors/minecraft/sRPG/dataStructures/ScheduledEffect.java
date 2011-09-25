package com.behindthemirrors.minecraft.sRPG.dataStructures;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.ResolverActive;

public class ScheduledEffect {
	
	String effect;
	ConfigurationNode node;
	StructureActive active;
	EffectDescriptor descriptor;
	CombatInstance combat;
	ProfileNPC source;
	Location location;
	ProfileNPC target;
	Block block;
	public int ticksToActivation;
	
	public ScheduledEffect(Integer delay, String effect, ConfigurationNode node, StructureActive active, EffectDescriptor descriptor, CombatInstance combat, ProfileNPC source, ProfileNPC target, Block block) {
		this.effect = effect;
		this.node = node;
		this.active = active;
		this.descriptor = descriptor;
		this.combat = combat;
		this.source = source;
		this.target = target;
		this.block = block;
		this.location = source.entity.getLocation();
		ticksToActivation = delay;
	}
	
	public void activate() {
		ResolverActive.resolveActiveEffect(effect, node, active, descriptor, combat, source, target, block, location);
	}
	
	@Override
	public String toString() {
		return effect+" ("+ticksToActivation+")";
	}
}
