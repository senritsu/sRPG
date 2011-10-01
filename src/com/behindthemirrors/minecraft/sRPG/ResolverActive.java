package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.block.Block;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ArgumentsActive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ScheduledEffect;

public class ResolverActive {
	
	public static void resolve(ArgumentsActive arguments) {
		if (arguments.active != null) {
			for (String effect : arguments.active.effects.keySet()) {
				ConfigurationNode node = arguments.active.effects.get(effect);

				int delay = node.getInt("delay", 0);
				if (delay > 0) {
					SRPG.cascadeQueueScheduler.scheduleEffect(new ScheduledEffect(delay, effect, node, arguments));
				} else {
					resolveActiveEffect(effect, node, arguments);
				}
			}
		}
	}
	
	public static void resolveActiveEffect(String effect, ConfigurationNode node, ArgumentsActive arguments) {
		// TODO: maybe add a unified method of choosing which entity the effect is relative to, and if the direction is calculated at the moment of the effect cast, or at the moment of execution
		Block targetBlock = MiscGeometric.offset(arguments.source.entity.getLocation(), arguments.targetBlock, node);
		if (effect.startsWith("apply-buff")) {
			if (node.getBoolean("self", true) && arguments.active.validVs(arguments.source)) {
				ResolverEffects.applyBuff(arguments.source, arguments.source, node, arguments.descriptor);
			}
			if (node.getBoolean("target", false) && arguments.active.validVs(arguments.target)) {
				ResolverEffects.applyBuff(arguments.source, arguments.target, node, arguments.descriptor);
			}
		} else if (effect.startsWith("direct-damage")) {
			if (node.getBoolean("self", false) && arguments.active.validVs(arguments.source)) {
				ResolverEffects.directDamage(arguments.source,node, arguments.descriptor);
			}
			if (node.getBoolean("target", true) && arguments.active.validVs(arguments.target)) {
				ResolverEffects.directDamage(arguments.target,node, arguments.descriptor);
			}
			
		} else if (effect.startsWith("change-blocks")) {
			if (targetBlock != null) {
				ResolverEffects.blockChange(arguments.source, arguments.location, targetBlock, node, arguments.descriptor);
			}
		} else if (effect.startsWith("transmute-item")) {
			ResolverEffects.transmuteItem(arguments.source, node, arguments.descriptor);
		} else if (effect.startsWith("manipulate-item")) {
			if (node.getBoolean("self", false) && arguments.active.validVs(arguments.source)) {
				ResolverEffects.manipulateItem(arguments.source, arguments.source, node, arguments.descriptor);
			}
			if (node.getBoolean("target", true) && arguments.active.validVs(arguments.target)) {
				ResolverEffects.manipulateItem(arguments.source, arguments.target, node, arguments.descriptor);
			}
		}  else if (effect.startsWith("impulse")) {
			if (node.getBoolean("self", false) && arguments.active.validVs(arguments.source)) {
				ResolverEffects.impulse(arguments.source, arguments.location, node, arguments.descriptor);
			}
			if (node.getBoolean("target", true) && arguments.active.validVs(arguments.target)) {
				ResolverEffects.impulse(arguments.target, arguments.location, node, arguments.descriptor);
			}
		} else if (effect.startsWith("lightning")) {
			ResolverEffects.lightning(targetBlock,node,arguments.descriptor);
		}
	}
}
