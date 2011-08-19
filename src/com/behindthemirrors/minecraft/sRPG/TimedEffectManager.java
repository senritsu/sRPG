package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class TimedEffectManager implements Runnable {
	
	public static boolean debug = false;
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
					ResolverPassive.resolve(profile,passive,descriptor);
				} else {
					if (debug) {
						SRPG.output("effect "+passive.name+" expired");
					}
					effectIterator.remove();
					if (profile instanceof ProfilePlayer){
						Messager.sendMessage(profile, "lost-buff",passive.signature);
					}
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
