package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;



public class CombatInstance {
	
	private EntityDamageEvent event;
	public ProfileNPC attacker;
	public ProfileNPC defender;
	
	public double basedamage;
	public double modifier;
	public double critChance;
	public double critMultiplier;
	public double missChance;
	public double missMultiplier;
	public double evadeChance;
	public double parryChance;
	
	boolean crit = false;
	boolean miss = false;
	boolean evade = false;
	boolean parry = false;
	private boolean canceled = false;
	
	private String cancelMessageAttacker;
	private String cancelMessageDefender;
	public static HashMap<String,Integer> damageTableTools;
	
	public CombatInstance(EntityDamageEvent event) {
		this.event = event;
		modifier = 0;
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
		PassiveAbility.trigger(this);
		Material attackerHandItem = attacker instanceof ProfilePlayer ? ((ProfilePlayer)attacker).player.getItemInHand().getType() : null;
		Material defenderHandItem = attacker instanceof ProfilePlayer ? ((ProfilePlayer)attacker).player.getItemInHand().getType() : null;
		
		evadeChance += defender.getStat("evade-chance", defenderHandItem, attackerHandItem) - attacker.getStat("anti-evade-chance", attackerHandItem, defenderHandItem);
		parryChance += defender.getStat("parry-chance", defenderHandItem, attackerHandItem) - attacker.getStat("anti-parry-chance", attackerHandItem, defenderHandItem);
		critChance += attacker.getStat("crit-chance", attackerHandItem, defenderHandItem) - defender.getStat("anti-crit-chance", defenderHandItem, attackerHandItem);
		critMultiplier += attacker.getStat("crit-multiplier", attackerHandItem, defenderHandItem) - defender.getStat("anti-crit-multiplier", defenderHandItem, attackerHandItem);
		
		if (attackerHandItem != null) {
			String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(attackerHandItem);
			if (toolName != null) {
				basedamage = damageTableTools.get(toolName);
			} else if (event instanceof EntityDamageByProjectileEvent && ((EntityDamageByProjectileEvent)event).getProjectile() instanceof Arrow) {
				basedamage = damageTableTools.get("bow");
			} else {
				basedamage = attacker.getStat("damage-unknown-item", 1);
			}
		} else {
			String entityName = Utility.getEntityName(attacker.entity);
			basedamage = attacker.getStat("damage-unarmed", 1);
			if (entityName.equalsIgnoreCase("creeper")) {
				basedamage = Math.round(new Double(event.getDamage()*basedamage)/14);
			}
			if (entityName.equalsIgnoreCase("ghast")) {
				basedamage = Math.round(new Double(event.getDamage()*basedamage)/5);
			}
		}
		
		double damage = basedamage + attacker.getStat("damage-modifier", attackerHandItem, defenderHandItem) - attacker.getStat("anti-damage-modifier", attackerHandItem, defenderHandItem);
		
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
		
		SRPG.output("triggering resolver");
		SRPG.output(attacker.passives.toString());
		ResolverPassive.trigger(this);
		if (attacker instanceof ProfilePlayer) {
			((ProfilePlayer)attacker).activate(this, defenderHandItem);
		}
		
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
			factor -= attacker.getStat("miss-damage-factor", attackerHandItem, defenderHandItem) -  defender.getStat("anti-miss-damage-factor", defenderHandItem, attackerHandItem);
		}
		if (parry) {
			if (factor > 0) {
				Messager.sendMessage(attacker, "parry-attacker");
				Messager.sendMessage(defender, "parry-defender");
			}
			factor -= defender.getStat("parry-efficiency", defenderHandItem, attackerHandItem) +  attacker.getStat("anti-parry-efficiency", attackerHandItem, defenderHandItem);
		}
		if (factor > 0 && evade) {
			if (factor > 0) {
				Messager.sendMessage(attacker, "evade-attacker");
				Messager.sendMessage(defender, "evade-defender");
			}
			factor -= defender.getStat("evade-efficiency", defenderHandItem, attackerHandItem) +  attacker.getStat("anti-evade-efficiency", attackerHandItem, defenderHandItem);
		}
		
		damage *= factor <= 1.0 ? factor : 1.0;
		
		if (crit && damage > 0) {
			Messager.sendMessage(attacker, "crit-attacker");
			Messager.sendMessage(defender, "crit-defender");
			damage *= critMultiplier;
		}
		
		SRPG.output("combat damage: "+damage);
		damage *= Utility.getArmorFactor(defender);
		SRPG.output("combat damage after armor mitigation: "+damage);
		
		if (damage > 0 && attacker instanceof ProfilePlayer) {
			((ProfilePlayer)attacker).addChargeTick();
		}
		event.setDamage(damage > 0 ? (int)Math.round(damage) : 0);
	}
}
