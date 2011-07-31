package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;


public class CombatInstance {
	
	static double defaultCritChance;
	static double defaultCritMultiplier;
	static double defaultMissChance;
	static double defaultMissMultiplier;
	
	private EntityDamageEvent event;
	public ProfileNPC attacker;
	public ProfileNPC defender;
	
	public Integer basedamage;
	public Integer modifier;
	public Double critChance;
	public Double critMultiplier;
	public Double missChance;
	public Double missMultiplier;
	public Double evadeChance;
	
	private boolean canceled = false;
	private String cancelMessageAttacker;
	private String cancelMessageDefender;
	
	public CombatInstance(EntityDamageEvent event) {
		this.event = event;
		basedamage = event.getDamage();
		modifier = 0;
		critChance = defaultCritChance;
		critMultiplier = defaultCritMultiplier;
		missChance = defaultMissChance;
		missMultiplier = defaultMissMultiplier;
		evadeChance = 0.0;
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
		TimedEffectResolver.trigger(this);
		Material attackerHandItem = attacker instanceof Player ? ((Player)attacker).getItemInHand().getType() : null;
		Material defenderHandItem = attacker instanceof Player ? ((Player)attacker).getItemInHand().getType() : null;
		
		evadeChance += defender.getStat("evasion", defenderHandItem);
		critChance += attacker.getStat("crit-chance", attackerHandItem);
		critMultiplier += attacker.getStat("crit-multiplier", attackerHandItem);
		// TODO: parry
		
		if (canceled){
			if (attacker instanceof Player && cancelMessageAttacker != null) {
				MessageParser.sendMessage((Player)attacker, cancelMessageAttacker);
			}
			if (defender instanceof Player && cancelMessageDefender != null) {
				MessageParser.sendMessage((Player)defender, cancelMessageDefender);
			}
			event.setCancelled(true);
			return;
		}
		// override for deactivated tools
		if (basedamage == null) {
			basedamage = event.getDamage();
		}
		double damage = basedamage + modifier;
		boolean crit = false;
		boolean miss = false;
		boolean evade = false;
		
		// apply critical hit
		if (SRPG.generator.nextDouble() <= critChance) {
			damage *= critMultiplier;
			crit = true;
		}
		// apply miss
		double roll = SRPG.generator.nextDouble();
		if (roll <= missChance + evadeChance) {
			damage *= missMultiplier;
			miss = true;
			if (roll <= evadeChance) {
				evade = true;
			}
		}
		// send messages to player
		// TODO: proper miss/evade messages
		if (attacker instanceof ProfilePlayer) {
			Player player = ((ProfilePlayer)attacker).player;
			if (miss) {
				if (damage <= 0) {
					MessageParser.sendMessage(player, "miss-no-damage");
				} else {
					MessageParser.sendMessage(player, "miss-damage");
				}
			} else if (crit) {
				if (!miss) {
					MessageParser.sendMessage(player, "critical-hit");
				} else if (damage > 0) {
					MessageParser.sendMessage(player, "miss-critical-damage");
				}
			}
		}
		
		if (damage > 0 && attacker instanceof ProfilePlayer) {
			((ProfilePlayer)attacker).addChargeTick();
		}
		
		event.setDamage(damage > 0 ? (int)Math.round(damage) : 0);
	}
}
