package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;



public class CombatInstance {
	
	private EntityDamageByEntityEvent event;
	public ProfileNPC attacker;
	public Material attackerHandItem;
	public ProfileNPC defender;
	public Material defenderHandItem;
	
	public double basedamage;
	public double damagerange;
	public double critChance;
	public double critMultiplier;
	public double missChance;
	public double missMultiplier;
	public double evadeChance;
	public double parryChance;
	public double bowcharge;
	
	boolean crit = false;
	boolean miss = false;
	boolean evade = false;
	boolean parry = false;
	boolean backstab = false;
	ProfileNPC highground = null;
	
	private boolean canceled = false;
	
	private String cancelMessageAttacker;
	private String cancelMessageDefender;
	public static HashMap<String,Integer> damageTableTools;
	
	public CombatInstance(EntityDamageByEntityEvent event) {
		this.event = event;
		critChance = 0.0;
		missChance = 0.0;
		evadeChance = 0.0;
		parryChance = 0.0;
	}
	
	public void cancel() {
		cancel(null,null);
	}
	
	public void cancel(String messageAttacker,String messageDefender) {
		canceled = true;
		cancelMessageAttacker = messageAttacker;
		cancelMessageDefender = messageDefender;
	}
	
	public void resolve() {
		//SRPG.dout("yaw attacker: "+attacker.entity.getLocation().getYaw()%360);
		//SRPG.dout("yaw defender: "+defender.entity.getLocation().getYaw()%360);
		double angle = MiscGeometric.angleBetweenFacings(attacker.entity.getLocation(),defender.entity.getLocation());
		//SRPG.dout("difference angle: "+angle);
		if (angle < 30) {
			backstab = true;
		}
		double heightdifference = attacker.entity.getLocation().getY() - defender.entity.getLocation().getY();
		if (heightdifference >= 3) {
			highground = attacker;
		} else if (heightdifference <= -3) {
			highground = defender;
		}
		attackerHandItem = attacker instanceof ProfilePlayer ? ((ProfilePlayer)attacker).player.getItemInHand().getType() : null;
		defenderHandItem = defender instanceof ProfilePlayer ? ((ProfilePlayer)defender).player.getItemInHand().getType() : null;
		if (event.getDamager() instanceof Arrow && attacker instanceof ProfilePlayer) {
			attackerHandItem = Material.BOW;
		}
		
		SRPG.dout("attack launched with "+attackerHandItem+" versus "+defenderHandItem,"combat");
		SRPG.dout("backstab = "+backstab+ " highground = "+(highground == null?"nobody":(highground == attacker? "attacker" : "defender")),"combat");
		
		evadeChance += defender.getStat("evade-chance", defenderHandItem, attackerHandItem) + attacker.getStat("target-evade-chance", attackerHandItem, defenderHandItem);
		parryChance += defender.getStat("parry-chance", defenderHandItem, attackerHandItem) + attacker.getStat("target-parry-chance", attackerHandItem, defenderHandItem);
		critChance += attacker.getStat("crit-chance", attackerHandItem, defenderHandItem) + defender.getStat("target-crit-chance", defenderHandItem, attackerHandItem);
		critMultiplier += attacker.getStat("crit-multiplier", attackerHandItem, defenderHandItem) + defender.getStat("target-crit-multiplier", defenderHandItem, attackerHandItem);
		
		basedamage = 0;
		damagerange = 0;
		double charge = 0;
		if (attackerHandItem != null) {
			String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(attackerHandItem);
			if (toolName != null) {
				basedamage = damageTableTools.get(toolName);
				if (attackerHandItem == Material.BOW) {
					charge = bowcharge;
				} else {
					// TODO: think of a way to implement damage ranges for other tools
					charge = SRPG.generator.nextDouble();
				}
				damagerange = damageTableTools.get(toolName+"-range");
			} else {
				basedamage = attacker.getStat("damage-unknown-item", 1);
			}
		} else {
			String entityName = MiscBukkit.getEntityName(attacker.entity);
			basedamage = attacker.getStat("damage-unarmed", 1);
			if (entityName.equalsIgnoreCase("creeper")) {
				damagerange = basedamage;
				basedamage = 0;
				charge = (event.getDamage()*damagerange)/14;
			} else if (entityName.equalsIgnoreCase("ghast")) {
				damagerange = basedamage;
				basedamage = 0;
				charge = (event.getDamage()*damagerange)/5;
			} else {
				damagerange = Math.max(attacker.getStat("max-damage-unarmed",basedamage),basedamage) - basedamage;
			}
		}
		
		SRPG.dout("basedamage: "+basedamage,"combat");
		SRPG.dout("damagerange: "+damagerange,"combat");
		basedamage += attacker.getStat("damage-modifier", attackerHandItem, defenderHandItem) + attacker.getStat("target-damage-modifier", attackerHandItem, defenderHandItem);
		damagerange += attacker.getStat("max-damage-modifier", attackerHandItem, defenderHandItem) + attacker.getStat("target-max-damage-modifier", attackerHandItem, defenderHandItem);
		SRPG.dout("basedamage2: "+basedamage,"combat");
		SRPG.dout("damagerange2: "+damagerange,"combat");
		ResolverPassive.resolveCombatBoosts(this);
		if (attacker instanceof ProfilePlayer) {
			((ProfilePlayer)attacker).activate(this, defenderHandItem);
		}
		
		if (damagerange < 0) {
			basedamage += damagerange;
			damagerange = 0;
		}
		SRPG.dout("basedamage3: "+basedamage,"combat");
		SRPG.dout("damagerange3: "+damagerange,"combat");
		SRPG.dout("charge: "+charge,"combat");
		double damage = basedamage + charge * damagerange;
		SRPG.dout("damage: "+damage,"combat");
		// apply critical hit
		if (SRPG.generator.nextDouble() <= critChance) {
			crit = true;
		}
		// apply miss
		if (SRPG.generator.nextDouble() <= missChance) {
			miss = true;
		}
		if (SRPG.generator.nextDouble() <= evadeChance) {
			evade = true;
		}
		if (SRPG.generator.nextDouble() <= parryChance) {
			parry = true;
		}
		
		SRPG.dout("triggering resolver","combat");
		SRPG.dout(attacker.passives.toString(),"combat");
		
		ResolverPassive.resolve(this);
		
		if (canceled){
			if (attacker instanceof Player && cancelMessageAttacker != null) {
				Messager.sendMessage((Player)attacker, cancelMessageAttacker);
			}
			if (defender instanceof Player && cancelMessageDefender != null) {
				Messager.sendMessage((Player)defender, cancelMessageDefender);
			}
			event.setCancelled(true);
			return;
		}
		
		double factor = 1.0;
		if (miss) {
			if (factor > 0) {
				Messager.sendMessage(attacker, "miss-attacker");
				Messager.sendMessage(defender, "miss-defender");
			}
			factor -= attacker.getStat("miss-damage-factor", attackerHandItem, defenderHandItem) +  defender.getStat("target-miss-damage-factor", defenderHandItem, attackerHandItem);
		}
		if (parry) {
			if (factor > 0) {
				Messager.sendMessage(attacker, "parry-attacker");
				Messager.sendMessage(defender, "parry-defender");
			}
			factor -= defender.getStat("parry-efficiency", defenderHandItem, attackerHandItem) +  attacker.getStat("target-parry-efficiency", attackerHandItem, defenderHandItem);
		}
		if (factor > 0 && evade) {
			if (factor > 0) {
				Messager.sendMessage(attacker, "evade-attacker");
				Messager.sendMessage(defender, "evade-defender");
			}
			factor -= defender.getStat("evade-efficiency", defenderHandItem, attackerHandItem) +  attacker.getStat("target-evade-efficiency", attackerHandItem, defenderHandItem);
		}
		
		damage *= factor <= 1.0 ? factor : 1.0;
		
		SRPG.dout("basedamage: "+damage,"combat");
		if (crit && damage > 0) {
			Messager.sendMessage(attacker, "crit-attacker");
			Messager.sendMessage(defender, "crit-defender");
			damage *= critMultiplier;
			SRPG.dout("critdamage: "+damage,"combat");
		}
		
		SRPG.dout("combat damage: "+damage,"combat");
		
		damage *= MiscBukkit.getArmorFactor(defender);
		
		SRPG.dout("combat damage after armor mitigation: "+damage,"combat");
		
		if (damage > 0 && attacker instanceof ProfilePlayer) {
			((ProfilePlayer)attacker).addChargeTicks(Settings.advanced.getInt("settings.charges.ticks.combat-hit", 0));
		}
		event.setDamage(damage > 0 ? (int)Math.round(damage) : 0);
		
		ResolverPassive.recoverDurability(attacker);
	}
}
