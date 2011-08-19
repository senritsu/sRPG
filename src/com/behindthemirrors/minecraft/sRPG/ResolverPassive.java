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
	
	// resolve effects that do something per tick
	public static void resolve(ProfileNPC data, StructurePassive effect, EffectDescriptor descriptor) {
		for (String name : effect.effects.keySet()) {
			if (name.startsWith("direct-damage")) {
				data.entity.damage(effect.effects.get(name).getInt("value", 0) * descriptor.potency);
			}
		}
	}
	
	// resolve static effects that influence combat
	// TODO: update
	public static void resolve(CombatInstance combat) {
		for (ProfileNPC profile : new ProfileNPC[] {combat.attacker,combat.defender}) {
			HashMap<StructurePassive,EffectDescriptor> map = new HashMap<StructurePassive, EffectDescriptor>();
			map.putAll(profile.effects);
			map.putAll(profile.passives);
			for (Map.Entry<StructurePassive,EffectDescriptor> entry : map.entrySet()) {
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				for (String name : passive.effects.keySet()) {
					ConfigurationNode node = passive.effects.get(name);
					if (!checkConditions(profile,(ArrayList<String>) node.getStringList("conditions", new ArrayList<String>()),combat)) {
						continue;
					}
					if (name.startsWith("set-combat-state")) {
						if (!((profile == combat.attacker && !node.getBoolean("as-attacker", true)) || (profile == combat.defender && !node.getBoolean("as-defender", false)))) {
							ResolverEffects.setCombatState(combat,node);;
						}
					} else if (name.startsWith("trigger-active")) {
						if (debug) {
							SRPG.output("trying to trigger active "+node.getString("action"));
						}
						if (SRPG.generator.nextDouble() <= node.getDouble("chance", 1.0)) {
							ResolverActive.resolve(node.getString("action"), combat.attacker, combat.defender, descriptor);
						}
					}
				}
			}
		}
	}
	
	public static boolean checkConditions(ProfileNPC profile, ArrayList<String> conditions, CombatInstance combat) {
		if (conditions.isEmpty() || ( profile == combat.attacker && (
				conditions.contains("attacking") || 
				(conditions.contains("backstab-offensive") && combat.backstab) || 
				(conditions.contains("highground-offensive") && combat.highground == combat.attacker) )) ||
			( profile == combat.defender && (
				conditions.contains("defending") || 
				(conditions.contains("backstab-defensive") && combat.backstab) ||
				(conditions.contains("highground-defensive") && combat.highground == combat.defender) ))
			) {
			return true;
		}
		return false;
	}
	
}
