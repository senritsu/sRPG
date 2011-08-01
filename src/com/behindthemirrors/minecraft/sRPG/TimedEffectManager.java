package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class TimedEffectManager implements Runnable {
	
	static boolean debug = false;
	ArrayList<ProfileNPC> relevantPlayers = new ArrayList<ProfileNPC>();
	
	public void run() {
		// check all currently active effects
		//SRPG.output("tick");
		Iterator<ProfileNPC> playerIterator = relevantPlayers.iterator();
		while (playerIterator.hasNext()) {
			ProfileNPC profile = playerIterator.next();
			Iterator<Map.Entry<StructurePassive,EffectDescriptor>> effectIterator = profile.effects.entrySet().iterator();
			while (effectIterator.hasNext()) {
				Map.Entry<StructurePassive,EffectDescriptor> entry = effectIterator.next();
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				if (descriptor.duration > 0) {
					descriptor.duration--;
					EffectResolver.tick(profile,passive,descriptor);
				} else {
					if (debug) {
						SRPG.output("effect "+passive.name+" expired");
					}
					effectIterator.remove();
					profile.timedStatsDirty = true;
				}
			}
			if (profile.effects.isEmpty()) {
				playerIterator.remove();
			}
			profile.recalculateBuffs();
		}
	}
	
	public void add(ProfileNPC npcData) {
		if (!relevantPlayers.contains(npcData)) {
			relevantPlayers.add(npcData);
		}
	}
	
}
