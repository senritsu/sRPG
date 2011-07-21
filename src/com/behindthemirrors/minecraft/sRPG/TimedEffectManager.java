package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class TimedEffectManager implements Runnable {
	
	ArrayList<PlayerData> relevantPlayers = new ArrayList<PlayerData>();
	
	public void run() {
		// check all currently active effects
		//SRPG.output("tick");
		for (PlayerData player : relevantPlayers) {
			Iterator<Map.Entry<String,Integer>> entries = player.effectCounters.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<String,Integer> entry = entries.next();
				Integer remainingTicks = entry.getValue();
				if (remainingTicks > 0) {
					player.effectCounters.put(entry.getKey(), remainingTicks - 1);
				} else {
					entries.remove();
				}
			}
		}
	}

}
