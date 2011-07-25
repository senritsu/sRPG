package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;


public class CombatInstance {
	
	static double defaultCritChance;
	static double defaultCritMultiplier;
	static double defaultMissChance;
	static double defaultMissMultiplier;
	
	private EntityDamageEvent event;
	public LivingEntity attacker;
	public LivingEntity defender;
	
	public Integer basedamage;
	public Integer modifier;
	public Double critChance;
	public Double critMultiplier;
	public Double missChance;
	public Double missMultiplier;
	
	private boolean canceled = false;
	private String cancelMessage;
	
	public CombatInstance(EntityDamageEvent event) {
		this.event = event;
		basedamage = event.getDamage();
		modifier = 0;
		critChance = defaultCritChance;
		critMultiplier = defaultCritMultiplier;
		missChance = defaultMissChance;
		missMultiplier = defaultMissMultiplier;
	}
	
	public void cancel() {
		cancel(null);
	}
	
	public void cancel(String message) {
		canceled = true;
		cancelMessage = message;
	}
	
	public void resolve() {
		if (canceled){
			if (attacker instanceof Player && cancelMessage != null) {
				MessageParser.sendMessage((Player)attacker, cancelMessage);
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
		
		// apply critical hit
		if (SRPG.generator.nextDouble() <= critChance) {
			damage *= critMultiplier;
			crit = true;
		}
		// apply miss
		if (SRPG.generator.nextDouble() <= missChance) {
			damage *= missMultiplier;
			miss = true;
		}
		// send messages to player
		if (attacker instanceof Player) {
			Player player = (Player)attacker;
			if (miss) {
				if (damage == 0) {
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
		
		event.setDamage(Math.round((int)damage));
	}
}
