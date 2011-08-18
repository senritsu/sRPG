
package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.PassiveAbility;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.MiscBukkit;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;


public class DamageEventListener extends EntityListener {
	
	static boolean debug = false;
	
	private HashMap<Integer,Player> damageTracking = new HashMap<Integer,Player>();

	static ArrayList<String> ANIMALS = new ArrayList<String>(Arrays.asList(new String[] {"pig","sheep","chicken","cow","squid"}));
	static ArrayList<String> MONSTERS = new ArrayList<String>(Arrays.asList(new String[] {"zombie","spider","skeleton","creeper","slime","pigzombie","ghast","giant","wolf"}));
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		LivingEntity source = null;
		LivingEntity target = (LivingEntity)event.getEntity();
		
		if (debug && target instanceof Player && event.getCause() != DamageCause.FIRE_TICK&& event.getCause() != DamageCause.FIRE) {
			SRPG.output("player damaged by "+event.getCause().name()+" ("+event.getDamage()+" damage)");
		}
		
//		if (event.getCause() != DamageCause.SUFFOCATION && event.getCause() != DamageCause.FIRE_TICK && event.getCause() != DamageCause.FIRE && event.getCause() != DamageCause.LAVA) {
//			SRPG.output(event.toString());
//			SRPG.output(event.getCause().toString());
//			SRPG.output(event.getDamage()+"");
//		}
//		if (event instanceof EntityDamageByEntityEvent) {
//			SRPG.output(((EntityDamageByEntityEvent)event).getDamager().toString());
//		}
		
		if (event.getCause() == DamageCause.FALL) {
			if (target instanceof Player) {
				PassiveAbility.trigger(event);
			}
		} else if (event instanceof EntityDamageByEntityEvent) { // || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			EntityDamageByEntityEvent attackEvent = (EntityDamageByEntityEvent)event;
			CombatInstance combat = new CombatInstance(attackEvent);
			
			// debug message
			if (source instanceof Player) {
				// debug message, displays remaining health of target before damage from this attack is applied
				if (debug) {
					SRPG.output("Target of attack has "+((LivingEntity)event.getEntity()).getHealth() + " health.");
				}
			}
			if (attackEvent.getDamager() instanceof LivingEntity) {
				source = (LivingEntity)attackEvent.getDamager();
				SRPG.output("entity attack");
			} else if (attackEvent.getDamager() instanceof Projectile) {
				source = ((Projectile)attackEvent.getDamager()).getShooter();
				SRPG.output("projectile attack");
			}
			
			// check attack restrictions
			combat.attacker = SRPG.profileManager.get(source);
			combat.defender = SRPG.profileManager.get(target);
			if (source instanceof Player && Settings.advanced.getBoolean("combat.restrictions.enabled", false)) {
				String prefix = Settings.advanced.getString("combat.restrictions.group-prefix");
				boolean forbidden = false;
				for (String group : Settings.advanced.getKeys("combat.restrictions.groups")) {
					if (((Player)source).hasPermission(prefix+group)) {
						forbidden = true;
						String targetname = MiscBukkit.getEntityName(target);
						for (String otherGroup : Settings.advanced.getStringList("combat.restrictions.groups."+group,null)) {
							if ((target instanceof Player && ((Player)target).hasPermission(prefix+otherGroup)) || 
									(otherGroup.equalsIgnoreCase("animals") && DamageEventListener.ANIMALS.contains(targetname)) || 
									(otherGroup.equalsIgnoreCase("monsters") && DamageEventListener.MONSTERS.contains(targetname)) ) {
								forbidden = false;
							}
						}
						break;
					}
				}
				if (forbidden) {
					if (debug) {
						SRPG.output("combat canceled because of combat restrictions");
					}
					combat.cancel();
				}
			}
			
			// resolve combat
			combat.resolve();
			if (debug) {
				SRPG.output("combat resolved, damage changed to "+(new Integer(event.getDamage())).toString());
			}
			
			// track entity if damage source was player, for xp gain on kill
			if (!(target instanceof Player) && !event.isCancelled() && event.getDamage() > 0) {
				int id = target.getEntityId();
				if (source instanceof Player) {
					if (debug) {
						SRPG.output("id of damaged entity: "+event.getEntity().getEntityId());
					}
					damageTracking.put(id, (Player)source);
				} else if (damageTracking.containsKey(id)) {
					damageTracking.remove(id);
				}
			}
		}
		
		// override standard health change for players to enable variable maximum hp
		boolean deactivated = true; // not production ready yet
		if (!deactivated && !event.isCancelled() && target instanceof Player && SRPG.profileManager.profiles.containsKey((Player)target)) {
			SRPG.output("overriding damage routine");
			Player player = (Player)target;
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
		if (!(event.getEntity() instanceof LivingEntity)) {
			return;
		}
		LivingEntity entity = (LivingEntity)event.getEntity();
		int id = entity.getEntityId();
		if (debug) {
			SRPG.output("entity with id "+id+" died");
		}
		if (damageTracking.containsKey(id)) {
			ProfileNPC profile = SRPG.profileManager.get(entity);
			if (debug) {
				SRPG.output("giving player"+damageTracking.get(id)+" xp");
			}
			try {
				SRPG.profileManager.get(damageTracking.get(id)).addXP((int) profile.getStat("xp"));
			} catch (NullPointerException ex) {
				SRPG.output("NPE at xp awarding");
				SRPG.output("profile: "+(profile == null ? null : profile.toString()));
				SRPG.output("tracking entry: "+(damageTracking.get(id) == null ? null : damageTracking.get(id).toString()));
				SRPG.output("xp stat: "+profile.getStat("xp"));
				SRPG.output("NPE end");
			}
			//TODO: maybe move saving to the data class
			SRPG.profileManager.save(damageTracking.get(id),"xp");
			damageTracking.remove(id);
		}
		if (!(entity instanceof Player)) {
			SRPG.profileManager.remove(entity);
		}
	}
}
