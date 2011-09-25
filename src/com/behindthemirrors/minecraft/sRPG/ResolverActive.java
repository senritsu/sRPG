package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ScheduledEffect;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureActive;


public class ResolverActive {
	
	public static void resolve(String name, CombatInstance combat, EffectDescriptor descriptor) {
		resolve(Settings.actives.get(name), combat, combat.attacker, combat.defender, null, descriptor);
	}
	
	public static void resolve(StructureActive active, CombatInstance combat, EffectDescriptor descriptor) {
		resolve(active, combat, combat.attacker, combat.defender, null, descriptor);
	}
	
	public static void resolve(String name, ProfileNPC source, ProfileNPC target, EffectDescriptor descriptor) {
		resolve(Settings.actives.get(name),null, source,target, null, descriptor);
	}
	
	public static void resolve(StructureActive active, ProfileNPC source, ProfileNPC target, EffectDescriptor descriptor) {
		resolve(active, null, source, target, null, descriptor);
	}
	
	public static void resolveActiveEffect(String effect, ConfigurationNode node, StructureActive active, EffectDescriptor descriptor, CombatInstance combat, ProfileNPC source, ProfileNPC target, Block block) {
		resolveActiveEffect(effect, node, active, descriptor, combat, source, target, block, source.entity.getLocation());
	}
	
	public static void resolveActiveEffect(String effect, ConfigurationNode node, StructureActive active, EffectDescriptor descriptor, CombatInstance combat, ProfileNPC source, ProfileNPC target, Block block, Location location) {
		// TODO: maybe add a unified method of choosing which entity the effect is relative to, and if the direction is calculated at the moment of the effect cast, or at the moment of execution
		if (effect.startsWith("apply-buff")) {
			if (node.getBoolean("self", true) && active.validVs(source)) {
				ResolverEffects.applyBuff(source, node);
			}
			if (node.getBoolean("target", false) && active.validVs(target)) {
				ResolverEffects.applyBuff(target, node);
			}
		} else if (effect.startsWith("direct-damage")) {
			if (node.getBoolean("self", false) && active.validVs(source)) {
				ResolverEffects.directDamage(source,node, descriptor);
			}
			if (node.getBoolean("target", false) && active.validVs(target)) {
				ResolverEffects.directDamage(target,node, descriptor);
			}
			
		} else if (effect.startsWith("change-blocks")) {
			if (block != null) {
				ResolverEffects.blockChange(source, location, block, node, descriptor);
			}
		} else if (effect.startsWith("transmute-item")) {
			ResolverEffects.transmuteItem(source, node, descriptor);
		} else if (effect.startsWith("impulse")) {
			if (node.getBoolean("self", false) && active.validVs(source)) {
				ResolverEffects.impulse(source, location, node, descriptor);
			}
			if (node.getBoolean("target", false) && active.validVs(target)) {
				ResolverEffects.impulse(target, location, node, descriptor);
			}
		} else if (effect.startsWith("lightning")) {
			ResolverEffects.lightning(block,node,descriptor);
		}
	}
	
	public static void resolve(StructureActive active, CombatInstance combat, ProfileNPC source, ProfileNPC target, Block block, EffectDescriptor descriptor) {
		if (active != null) {
			for (String effect : active.effects.keySet()) {
				ConfigurationNode node = active.effects.get(effect);

				int delay = node.getInt("delay", 0);
				Block offsetBlock = MiscGeometric.offset(source.entity.getLocation(), block, node);
				if (delay > 0) {
					SRPG.cascadeQueueScheduler.scheduleEffect(new ScheduledEffect(delay, effect, node, active, descriptor, combat, source, target, offsetBlock));
				} else {
					resolveActiveEffect(effect, node, active, descriptor, combat, source, target, offsetBlock);
				}
			}
		}
	}
	
	public static void resolve(String name, ProfileNPC source, Block block, EffectDescriptor descriptor) {
		resolve(Settings.actives.get(name), null, source, null, block, descriptor);
	}
	
	public static void resolve(StructureActive active, ProfileNPC source, Block block, EffectDescriptor descriptor) {
		resolve(active, null, source, null, block, descriptor);
	}
}
