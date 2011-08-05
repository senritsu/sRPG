package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.util.config.ConfigurationNode;

public class ResolverEffects {

	static void setCombatState(CombatInstance combat, ConfigurationNode node) {
		if (SRPG.generator.nextDouble() < node.getDouble("chance", 1.0)) {
			if (node.getBoolean("canceled", false)) {
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
	
	static void applyBuff(ProfileNPC profile, ConfigurationNode node) {
		String name = node.getString("name");
		EffectDescriptor descriptor = new EffectDescriptor(name);
		descriptor.duration = node.getInt("duration", 0);
		StructurePassive buff = Settings.passives.get(Utility.stripPotency(name));
		profile.addEffect(buff, descriptor);
		MessageParser.sendMessage(profile, "acquired-buff",buff.name);
	}
	
	static void directDamage(ProfileNPC profile, ConfigurationNode node, EffectDescriptor descriptor) {
		if (SRPG.generator.nextDouble() <= node.getDouble("chance", 1.0)) {
			profile.entity.damage(node.getInt("value", 0) * descriptor.potency);
		}
	}

}
