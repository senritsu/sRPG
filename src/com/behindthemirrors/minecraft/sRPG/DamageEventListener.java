// thanks to 

package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Material;


public class DamageEventListener extends EntityListener{
	
	public boolean debug = false;
	
	public static HashMap<String,Integer> damageTableMonsters;
	public static HashMap<String,Integer> xpTableCreatures;
	public static HashMap<String,Integer> damageTableTools;
	public static int damageFists;
	public static int damageBow;
	public static double critChance;
	public static double critMultiplier;
	public static double missChance;
	public static double missMultiplier;
	public static boolean increaseDamageWithDepth;
	public static ArrayList<int[]> depthTiers;
	
	private HashMap<Integer,Player> damageTracking = new HashMap<Integer,Player>();
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		String sourcename = "";
		Entity source = null;
		Player player = null;
		Entity target = event.getEntity();
		
		
		if (event.getCause() == DamageCause.FALL) {
			if (target instanceof Player) {
				PassiveAbility.trigger((Player)target, event);
			}
		} else if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			if (event instanceof EntityDamageByEntityEvent) {
				source = (Entity)((EntityDamageByEntityEvent)event).getDamager();
			//} else if (event instanceof EntityDamageByProjectileEvent) {
			//	entity = ((EntityDamageByProjectileEvent)event).getDamager();
			}
			
			if (source != null) {
				sourcename = Utility.getEntityName(source);
			}
			
			CombatInstance combat = new CombatInstance(event);
			// damage from monsters
			if (Settings.MONSTERS.contains(sourcename)) {
				// for now no distinction between arrow hits and normal hits
				combat.basedamage = damageTableMonsters.get(sourcename);
				// depth modifier
				if (increaseDamageWithDepth) {
					for (int[] depth : depthTiers) {
						if (((EntityDamageByEntityEvent)event).getDamager().getLocation().getY() < (double)depth[0]) {
							combat.modifier += depth[1];
						}
					}
				}
			// damage from players
			} else if (sourcename.equalsIgnoreCase("player") && event instanceof EntityDamageByEntityEvent) {
				player = (Player)(((EntityDamageByEntityEvent)event).getDamager());
				
				// debug message, displays remaining health of target before damage from this attack is applied
				if (event.getEntity() instanceof Creature) {
					if (debug) {
						SRPG.output("Target of attack has "+((Creature)event.getEntity()).getHealth() + " health.");
					}
				}
				// select damage value from config depending on what item is held
				if (event instanceof EntityDamageByEntityEvent) {
					Material material = player.getItemInHand().getType();
					String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(material);
					if (toolName != null) {
						combat.basedamage = damageTableTools.get(toolName);
						// award charge tick
						SRPG.playerDataManager.get(player).addChargeTick(Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(material));
						//TODO: maybe move saving to the data class
						SRPG.playerDataManager.save(player,"chargedata");
					} else if (event instanceof EntityDamageByProjectileEvent) {
						combat.basedamage = damageBow;
					} else {
						combat.basedamage = damageFists; 
					}
				}
			}
			
			target = event.getEntity();
			
			// check passive abilities
			if (player != null) {
				PassiveAbility.trigger(player, combat, true);
			}
			if (target instanceof Player) {
				PassiveAbility.trigger((Player)target, combat, false);
			}
			// resolve combat
			if (event instanceof EntityDamageByEntityEvent) {
				combat.resolve(player);
			}
			
			// track entity if damage source was player, for xp gain on kill
			int id = target.getEntityId();
			if (!(target instanceof Player)) {
				if (player != null) {
					if (debug) {
						SRPG.output("id of damaged entity: "+event.getEntity().getEntityId());
					}
					damageTracking.put(id, player);
				} else if (damageTracking.containsKey(id)) {
					damageTracking.remove(id);
				}
			}
		}
	}
	
	// check if entity was tracked, and if yes give the player who killed it xp
	public void onEntityDeath (EntityDeathEvent event) {
		Entity entity = event.getEntity();
		int id = entity.getEntityId();
		if (debug) {
			SRPG.output("entity with id "+id+" died");
		}
		if (damageTracking.containsKey(id)) {
			if (debug) {
				SRPG.output("giving player"+damageTracking.get(id)+" xp");
			}
			String monster = Utility.getEntityName(entity);
			SRPG.playerDataManager.get(damageTracking.get(id)).addXP(xpTableCreatures.get(monster));
			//TODO: maybe move saving to the data class
			SRPG.playerDataManager.save(damageTracking.get(id),"xp");
			damageTracking.remove(id);
		}
	}
}
