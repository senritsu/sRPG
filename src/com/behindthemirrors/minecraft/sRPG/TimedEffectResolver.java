package com.behindthemirrors.minecraft.sRPG;

public class TimedEffectResolver {
	
	static boolean debug = false;
	
	// trigger effects that do something per tick
	public static void trigger(ProfileNPC data, String effect) {
		if (effect.startsWith("poison")) {
			Integer potency = Integer.parseInt(effect.substring(6));
			data.entity.damage(potency);
		}
	}
	
	// trigger static effects that influence combat
	public static void trigger(CombatInstance combat) {
		ProfileNPC attackerProfile = SRPG.profileManager.get(combat.attacker);
		ProfileNPC defenderProfile = SRPG.profileManager.get(combat.defender);
		if (attackerProfile.effectCounters.containsKey("rage")) {
			combat.critChance = 1.0;
		}
		if (defenderProfile.effectCounters.containsKey("invincibility")) {
			if (debug) {
				SRPG.output("canceled combat because target was invincible");
			}
			combat.cancel();
		}
	}
}
