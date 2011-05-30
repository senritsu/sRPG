package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;
import org.bukkit.entity.Creature;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityListener;


public class SpawnEventListener extends EntityListener {
	
	public boolean debug = false;
	
	public static HashMap<String,Integer> healthTableCreatures;
	
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		String creature = Utility.getEntityName(event.getEntity());
		if (debug) {
			SRPG.output("creature spawned: "+creature);
		}
		if (creature.startsWith("slime")) {
			// slimes don't work properly yet
		} else {
			((Creature)event.getEntity()).setHealth(healthTableCreatures.get(creature));
		}
		}
	}
