package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class ResolverPassive {
	
	public static boolean debug = false;
	
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
			HashMap<StructurePassive,EffectDescriptor> map = new HashMap<StructurePassive, EffectDescriptor>();
			map.putAll(profile.effects);
			map.putAll(profile.passives);
			for (Map.Entry<StructurePassive,EffectDescriptor> entry : map.entrySet()) {
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				for (String name : passive.effects.keySet()) {
					if (name.startsWith("set-combat-state")) {
						ConfigurationNode node = passive.effects.get(name);
						if (!((profile == combat.attacker && !node.getBoolean("as-attacker", true)) || (profile == combat.defender && !node.getBoolean("as-defender", false)))) {
							ResolverEffects.setCombatState(combat,node);;
						}
					} else if (name.startsWith("trigger")) {
						ConfigurationNode node = passive.effects.get(name);
						SRPG.output("trying to trigger active "+node.getString("action"));
						if (SRPG.generator.nextDouble() <= node.getDouble("chance", 1.0)) {
							if (profile == combat.attacker && node.getStringList("conditions", new ArrayList<String>()).contains("combat-offensive")) {
								ResolverActive.resolve(node.getString("action"), combat.attacker, combat.defender, descriptor);
							} else if (profile == combat.defender && node.getStringList("conditions", new ArrayList<String>()).contains("combat-defensive")) {
								ResolverActive.resolve(node.getString("action"), combat.defender, combat.attacker, descriptor);
							}
						}
					}
				}
			}
		}
	}
}
