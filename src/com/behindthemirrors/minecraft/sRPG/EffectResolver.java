package com.behindthemirrors.minecraft.sRPG;

import java.util.Map;

import org.bukkit.util.config.ConfigurationNode;

public class EffectResolver {
	
	static boolean debug = false;
	
	// trigger effects that do something per tick
	public static void tick(ProfileNPC data, StructurePassive effect, EffectDescriptor descriptor) {
		for (String name : effect.effects.keySet()) {
			if (name.startsWith("direct-damage")) {
				data.entity.damage(effect.effects.get(name).getInt("value", 0) * descriptor.potency);
			}
		}
	}
	
	// trigger static effects that influence combat
	// TODO: update
	public static void trigger(CombatInstance combat) {
		for (ProfileNPC profile : new ProfileNPC[] {combat.attacker,combat.defender}) {
			for (Map.Entry<StructurePassive,EffectDescriptor> entry : profile.effects.entrySet()) {
				StructurePassive passive = entry.getKey();
				for (String name : passive.effects.keySet()) {
					if (name.startsWith("set-combat-state")) {
						ConfigurationNode node = passive.effects.get(name);
						SRPG.output("combat: "+name);
						if ((profile == combat.attacker && !node.getBoolean("as-attacker", true)) || (profile == combat.defender && !node.getBoolean("as-defender", false))) {
							continue;
						}
						if (SRPG.generator.nextDouble() < passive.effects.get(name).getDouble("chance", 1.0)) {
							if (node.getBoolean("canceled", false)) {
								if (debug) {
									SRPG.output("canceled combat because target was invincible");
								}
								combat.cancel();
							} else if (node.getBoolean("crit", false)) {
								combat.crit = true;
							} else if (node.getBoolean("parry", false)) {
								combat.parry = true;
							} else if (node.getBoolean("evade", false)) {
								combat.evade = true;
							} else if (node.getBoolean("miss", false)) {
								combat.miss = true;
							}
						}
					}
				}
			}
		}
	}
}
