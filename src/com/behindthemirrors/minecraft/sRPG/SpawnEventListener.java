package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityListener;


public class SpawnEventListener extends EntityListener {
	
	static boolean debug = false;
	// for testing
	static boolean spawnInvincible = false;
	
	public static HashMap<String,Integer> healthTableCreatures;
	
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		String creature = Utility.getEntityName(event.getEntity());
		if (debug) {
			SRPG.output("creature spawned: "+creature);
		}
		LivingEntity entity = (LivingEntity)event.getEntity();
		entity.setHealth(healthTableCreatures.get(creature));
		SRPG.profileManager.add(entity);
		// for testing
		if (spawnInvincible) {
			SRPG.profileManager.get(entity).addEffect(Settings.passives.get("invincibility"), new EffectDescriptor(10));
		}
	}
}
