
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
import com.behindthemirrors.minecraft.sRPG.Messager;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.MiscBukkit;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;


public class DamageEventListener extends EntityListener {
	
	private HashMap<Integer,Player> damageTracking = new HashMap<Integer,Player>();

	static ArrayList<String> ANIMALS = new ArrayList<String>(Arrays.asList(new String[] {"pig","sheep","chicken","cow","squid"}));
	static ArrayList<String> MONSTERS = new ArrayList<String>(Arrays.asList(new String[] {"zombie","spider","skeleton","creeper","slime","pigzombie","ghast","giant","wolf"}));
	static ArrayList<DamageCause> NATURAL_CAUSES = new ArrayList<DamageCause>(Arrays.asList(new DamageCause[] {DamageCause.FALL,DamageCause.FIRE,DamageCause.FIRE_TICK,DamageCause.LAVA,DamageCause.SUFFOCATION}));
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity || Settings.worldBlacklist.contains(event.getEntity().getWorld()))) {
			return;
		}
		LivingEntity source = null;
		LivingEntity target = (LivingEntity)event.getEntity();
		
		if (target instanceof Player && event.getCause() != DamageCause.FIRE_TICK&& event.getCause() != DamageCause.FIRE) {
			SRPG.dout("player damaged by "+event.getCause().name()+" ("+event.getDamage()+" damage)", "combat");
		}
		
//		if (event.getCause() != DamageCause.SUFFOCATION && event.getCause() != DamageCause.FIRE_TICK && event.getCause() != DamageCause.FIRE && event.getCause() != DamageCause.LAVA) {
//			SRPG.output(event.toString());
//			SRPG.output(event.getCause().toString());
//			SRPG.output(event.getDamage()+"");
//		}
//		if (event instanceof EntityDamageByEntityEvent) {
//			SRPG.output(((EntityDamageByEntityEvent)event).getDamager().toString());
//		}
		
		if (!NATURAL_CAUSES.contains(event.getCause())) {
			SRPG.dout("damage event: "+event.toString()+", cause:"+event.getCause().name(), "combat");
		}
		
		if (event.getCause() == DamageCause.FALL) {
			if (target instanceof Player) {
				ProfilePlayer profile = (ProfilePlayer)SRPG.profileManager.get(target);
				if (event.getCause() == DamageCause.FALL) {
					// check permissions
					
					Integer height = (int) Math.ceil(event.getEntity().getFallDistance());
					Integer damage = height - 2 + profile.getStat("fall-damage-modifier", 0);
					
					// auto-roll roll
					double roll = SRPG.generator.nextDouble();
					double autorollChance = profile.getStat("roll-chance");
					// manual roll check
					boolean manualRoll = (profile.player != null) && profile.player.isSneaking() && (System.currentTimeMillis() - ((ProfilePlayer) profile).sneakTimeStamp) < profile.getStat("manual-roll-window", 0);
					if (manualRoll || roll < autorollChance) {
						damage -= profile.getStat("roll-damage-reduction",0);
						if (manualRoll) {
							Messager.sendMessage(profile.player, "roll-manual");
						} else {
							Messager.sendMessage(profile.player, "roll-auto");
						}
					}
					// no negative damage
					if (damage < 0) {
						damage = 0;
					}
					
					event.setDamage(damage);
				}
			}
		} else if (event instanceof EntityDamageByEntityEvent && 
				((EntityDamageByEntityEvent)event).getDamager() instanceof LivingEntity ||
				event.getCause() == DamageCause.PROJECTILE) { // || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			
			EntityDamageByEntityEvent attackEvent = (EntityDamageByEntityEvent)event;
			CombatInstance combat = new CombatInstance(attackEvent);
			
			// debug message
			if (attackEvent.getDamager() instanceof LivingEntity) {
				source = (LivingEntity)attackEvent.getDamager();
				SRPG.dout("entity attack","combat");
			} else if (attackEvent.getDamager() instanceof Projectile) {
				source = ((Projectile)attackEvent.getDamager()).getShooter();
				SRPG.dout("projectile attack","combat");
				if (source instanceof Player) {
					int damage = event.getDamage();
					combat.bowcharge = (damage-2)/8.0;
					SRPG.dout("bow charge level: "+combat.bowcharge+" (from "+damage+")","combat");
				}
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
					SRPG.dout("combat canceled because of combat restrictions","combat");
					combat.cancel();
				}
			}
			
			// resolve combat
			if (!combat.defender.entity.isDead()) {
				combat.resolve();
				SRPG.dout("combat resolved, damage changed to "+(new Integer(event.getDamage())).toString(),"combat");
			
				// track entity if damage source was player, for xp gain on kill
				if (!(target instanceof Player) && !event.isCancelled() && event.getDamage() > 0) {
					int id = target.getEntityId();
					if (source instanceof Player) {
						SRPG.dout("id of damaged entity: "+event.getEntity().getEntityId(),"combat");
						damageTracking.put(id, (Player)source);
					} else if (damageTracking.containsKey(id)) {
						damageTracking.remove(id);
					}
				}
			}
		}
		
		// override standard health change for players to enable variable maximum hp
		boolean deactivated = true; // not production ready yet
		if (!deactivated && !event.isCancelled() && target instanceof Player && SRPG.profileManager.profiles.containsKey((Player)target)) {
			SRPG.dout("overriding damage routine","combat");
			Player player = (Player)target;
			ProfilePlayer profile = SRPG.profileManager.get(player);
			SRPG.dout(profile.hp.toString(),"combat");
			profile.hp -= event.getDamage();
			if (profile.hp < 0) {
				profile.hp = 0;
			}
			Integer normalized = 20*profile.hp / profile.hp_max;
			if (normalized == 0 && profile.hp != 0) {
				normalized = 1;
			}
			SRPG.dout("player health changed to "+profile.hp+"/"+profile.hp_max+" ("+player.getHealth()+" to "+normalized+" normalized","combat");
			event.setDamage(player.getHealth() - normalized);
		}
	}
	
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if (Settings.worldBlacklist.contains(event.getEntity().getWorld())) {
			return;
		}
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
			SRPG.dout("player health changed to "+profile.hp+"/"+profile.hp_max+" ("+player.getHealth()+" to "+normalized+" normalized","combat");
			event.setAmount(normalized - player.getHealth());
		}
	}
	
	// check if entity was tracked, and if yes give the player who killed it xp
	public void onEntityDeath (EntityDeathEvent event) {
		if (Settings.worldBlacklist.contains(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof LivingEntity)) {
			return;
		}
		LivingEntity entity = (LivingEntity)event.getEntity();
		int id = entity.getEntityId();
		SRPG.dout("entity with id "+id+" died","death");
		if (damageTracking.containsKey(id)) {
			ProfileNPC profile = SRPG.profileManager.get(entity);
			SRPG.dout("giving player"+damageTracking.get(id)+" xp","death");
			try {
				ProfilePlayer killer = SRPG.profileManager.get(damageTracking.get(id));
				killer.addXP((int) profile.getStat("xp"));
				killer.addChargeTicks(Settings.advanced.getInt("settings.charges.ticks.combat-kill", 0));
				
			} catch (NullPointerException ex) {
				SRPG.output("NPE at xp awarding, contact zaph34r about it");
				SRPG.output("profile: "+(profile == null ? null : profile.toString()));
				SRPG.output("tracking entry: "+(damageTracking.get(id) == null ? null : damageTracking.get(id).toString()));
				SRPG.output("xp stat: "+profile.getStat("xp"));
				SRPG.output("NPE end");
			}
			//TODO: maybe move saving to the data class
			SRPG.profileManager.save(damageTracking.get(id),"xp");
			SRPG.profileManager.save(damageTracking.get(id),"chargedata");
			damageTracking.remove(id);
		}
		if (!(entity instanceof Player)) {
			SRPG.dout("removing entity "+entity.toString()+" because of its death");
			SRPG.profileManager.scheduleRemoval(entity,5);
		}
	}
}
