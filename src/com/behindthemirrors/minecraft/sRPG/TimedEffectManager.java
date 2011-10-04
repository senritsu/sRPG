package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.Watcher;


public class TimedEffectManager implements Runnable {
	
	ArrayList<ProfileNPC> relevantProfiles = new ArrayList<ProfileNPC>();
	
	public void run() {
		Watcher.tick();
		SRPG.profileManager.checkEntityRemoval();
		// check all currently active effects
		//SRPG.output("tick");
		Iterator<ProfileNPC> playerIterator = relevantProfiles.iterator();
		while (playerIterator.hasNext()) {
			ProfileNPC profile = playerIterator.next();
			if (Settings.worldBlacklist.contains(profile.entity.getWorld())) {
				continue;
			}
			Iterator<Map.Entry<StructurePassive,EffectDescriptor>> effectIterator = profile.effects.entrySet().iterator();
			while (effectIterator.hasNext()) {
				Map.Entry<StructurePassive,EffectDescriptor> entry = effectIterator.next();
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				if (descriptor.duration > 0) {
					descriptor.duration--;
					ResolverPassive.resolve(profile,passive,descriptor);
				} else {
					SRPG.dout("effect "+passive.name+" expired","effects");
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
	
	public void add(ProfileNPC profile) {
		if (!relevantProfiles.contains(profile)) {
			relevantProfiles.add(profile);
		}
	}
	
	public void remove(ProfileNPC profile) {
		relevantProfiles.remove(profile);
	}
	
}
