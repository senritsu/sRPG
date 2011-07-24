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
		Iterator<ProfileNPC> iterator = relevantPlayers.iterator();
		while (iterator.hasNext()) {
			ProfileNPC data = iterator.next();
			Iterator<Map.Entry<String,Integer>> entries = data.effectCounters.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<String,Integer> entry = entries.next();
				Integer remainingTicks = entry.getValue();
				if (remainingTicks > 0) {
					data.effectCounters.put(entry.getKey(), remainingTicks - 1);
					TimedEffectResolver.trigger(data,entry.getKey());
				} else {
					if (debug) {
						SRPG.output("effect "+entry.getKey()+" expired");
					}
					entries.remove();
				}
			}
			if (data.effectCounters.isEmpty()) {
				iterator.remove();
			}
		}
	}
	
	public void add(ProfileNPC npcData) {
		if (!relevantPlayers.contains(npcData)) {
			relevantPlayers.add(npcData);
		}
	}
	
}
