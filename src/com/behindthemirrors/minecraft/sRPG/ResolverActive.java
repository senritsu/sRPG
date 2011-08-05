package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.block.Block;
import org.bukkit.util.config.ConfigurationNode;

public class ResolverActive {
	static void resolve(String name, ProfileNPC source, ProfileNPC target, EffectDescriptor descriptor) {
		StructureActive active = Settings.actives.get(name);
		if (active != null) {
			for (String effect : active.effects.keySet()) {
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
					
				}
			}
		}
	}
	
	static void resolve(String name, ProfileNPC source, Block target) {
		
	}
}
