package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.block.Block;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
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
	
	public static void resolve(StructureActive active, CombatInstance combat, ProfileNPC source, ProfileNPC target, Block block, EffectDescriptor descriptor) {
		if (active != null) {
			SRPG.output("resolving "+active.signature);
			for (String effect : active.effects.keySet()) {
				SRPG.output("activating active "+effect);
				ConfigurationNode node = active.effects.get(effect);
				
				if (effect.startsWith("apply-buff")) {
					if (node.getBoolean("self", true)) {
						ResolverEffects.applyBuff(source, node);
					}
					if (node.getBoolean("opponent", false)) {
						ResolverEffects.applyBuff(target, node);
					}
				} else if (effect.startsWith("direct-damage")) {
					if (node.getBoolean("self", false)) {
						ResolverEffects.directDamage(source,node, descriptor);
					}
					if (node.getBoolean("opponent", false)) {
						ResolverEffects.directDamage(target,node, descriptor);
					}
					
				} else if (effect.startsWith("block-change")) {
					if (block != null) {
						ResolverEffects.blockChange(source, block, node, descriptor);
					}
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
