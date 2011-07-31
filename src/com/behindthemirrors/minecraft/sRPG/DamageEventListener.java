
package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.Material;


public class DamageEventListener extends EntityListener{
	
	static boolean debug = false;
	
	public static HashMap<String,Integer> damageTableMonsters;
	public static HashMap<String,Integer> xpTableCreatures;
	public static HashMap<String,Integer> damageTableTools;
	public static boolean increaseDamageWithDepth;
	public static ArrayList<int[]> depthTiers;
	
	private HashMap<Integer,Player> damageTracking = new HashMap<Integer,Player>();
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		String sourcename = "";
		LivingEntity source = null;
		Player player = null;
		LivingEntity target = (LivingEntity)event.getEntity();
		
		if (debug && target instanceof Player && event.getCause() != DamageCause.FIRE_TICK&& event.getCause() != DamageCause.FIRE) {
			SRPG.output("player damaged by "+event.getCause().name()+" ("+event.getDamage()+" damage)");
		}
		
		if (event.getCause() == DamageCause.FALL) {
			if (target instanceof Player) {
				PassiveAbility.trigger(event);
			}
		} else if (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			if (event instanceof EntityDamageByEntityEvent) {
				source = (LivingEntity)((EntityDamageByEntityEvent)event).getDamager();
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
				if (sourcename.equalsIgnoreCase("creeper")) {
					combat.basedamage = (int)Math.round(new Double(event.getDamage()*damageTableMonsters.get(sourcename))/14);
				}
				if (sourcename.equalsIgnoreCase("ghast")) {
					combat.basedamage = (int)Math.round(new Double(event.getDamage()*damageTableMonsters.get(sourcename))/5);
				}
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
				if (event.getEntity() instanceof LivingEntity) {
					if (debug) {
						SRPG.output("Target of attack has "+((LivingEntity)event.getEntity()).getHealth() + " health.");
					}
				}
				// select damage value from config depending on what item is held
				if (event instanceof EntityDamageByEntityEvent) {
					Material material = player.getItemInHand().getType();
					String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(material);
					if (toolName != null) {
						combat.basedamage = damageTableTools.get(toolName);
						// award charge tick
						//TODO: maybe move saving to the data class
						SRPG.profileManager.save(player,"chargedata");
					} else if (event instanceof EntityDamageByProjectileEvent && ((EntityDamageByProjectileEvent)event).getProjectile() instanceof Arrow) {
						combat.basedamage = damageTableTools.get("bow");
					} else {
						combat.basedamage = damageTableTools.get("fists"); 
					}
				}
			}
			
			// resolve combat
			if (event instanceof EntityDamageByEntityEvent) {
				combat.attacker = SRPG.profileManager.get(source);
				combat.defender = SRPG.profileManager.get(target);
				if (player != null && Settings.advanced.getBoolean("combat.restrictions.enabled", false)) {
					String prefix = Settings.advanced.getString("combat.restrictions.group-prefix");
					boolean forbidden = false;
					for (String group : Settings.advanced.getKeys("combat.restrictions.groups")) {
						if (player.hasPermission(prefix+group)) {
							forbidden = true;
							String targetname = Utility.getEntityName(target);
							for (String otherGroup : Settings.advanced.getStringList("combat.restrictions.groups."+group,null)) {
								if ((target instanceof Player && ((Player)target).hasPermission(prefix+otherGroup)) || 
										(otherGroup.equalsIgnoreCase("animals") && Settings.ANIMALS.contains(targetname)) || 
										(otherGroup.equalsIgnoreCase("monsters") && Settings.MONSTERS.contains(targetname)) ) {
									forbidden = false;
								}
							}
							break;
						}
					}
					if (forbidden) {
						combat.cancel();
					}
				}
				combat.resolve();
				if (debug) {
					SRPG.output("combat resolved, damage changed to "+(new Integer(event.getDamage())).toString());
				}
			}
			
			// track entity if damage source was player, for xp gain on kill
			int id = target.getEntityId();
			if (!(target instanceof Player) && !event.isCancelled() && event.getDamage() > 0) {
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
		
		// override standard health change for players to enable variable maximum hp
		boolean deactivated = true; // not production ready yet
		if (!deactivated && !event.isCancelled() && target instanceof Player && SRPG.profileManager.profiles.containsKey((Player)target)) {
			SRPG.output("overriding damage routine");
			player = (Player)target;
			ProfilePlayer profile = SRPG.profileManager.get(player);
			SRPG.output(profile.hp.toString());
			profile.hp -= event.getDamage();
			if (profile.hp < 0) {
				profile.hp = 0;
			}
			Integer normalized = 20*profile.hp / profile.hp_max;
			if (normalized == 0 && profile.hp != 0) {
				normalized = 1;
			}
			if (debug) {
				SRPG.output("player health changed to "+profile.hp+"/"+profile.hp_max+" ("+player.getHealth()+" to "+normalized+" normalized");
			}
			event.setDamage(player.getHealth() - normalized);
		}
	}
	
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		// override standard health change for players to enable variable maximum hp
		boolean deactivated = true; // not production ready yet
		if (!deactivated && !event.isCancelled() && event.getEntity() instanceof Player && SRPG.profileManager.profiles.containsKey((Player)event.getEntity())) {
			Player player = (Player)event.getEntity(); 
			ProfilePlayer profile = SRPG.profileManager.get(player);
			profile.hp += event.getAmount();
			profile.hp = profile.hp > profile.hp_max ? profile.hp_max : profile.hp;
			Integer normalized = 20*profile.hp / profile.hp_max;
			if (normalized == 0 && profile.hp != 0) {
				normalized = 1;
			}
			if (debug) {
				SRPG.output("player health changed to "+profile.hp+"/"+profile.hp_max+" ("+player.getHealth()+" to "+normalized+" normalized");
			}
			event.setAmount(normalized - player.getHealth());
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
			SRPG.profileManager.get(damageTracking.get(id)).addXP(xpTableCreatures.get(monster));
			//TODO: maybe move saving to the data class
			SRPG.profileManager.save(damageTracking.get(id),"xp");
			damageTracking.remove(id);
		}
		if (entity instanceof LivingEntity && !(entity instanceof Player)) {
			SRPG.profileManager.remove((LivingEntity)entity);
		}
	}
}
