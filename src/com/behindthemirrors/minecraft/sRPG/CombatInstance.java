package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;


public class CombatInstance {
	
	private EntityDamageEvent event;
	
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
		critChance = Settings.advanced.getDouble("combat.crit-chance", 0.0);
		critMultiplier = Settings.advanced.getDouble("combat.crit-multiplier", 0.0);
		missChance = Settings.advanced.getDouble("combat.miss-chance", 0.0);
		missMultiplier = Settings.advanced.getDouble("combat.miss-multiplier", 0.0);
	}
	
	public void cancel() {
		cancel(null);
	}
	
	public void cancel(String message) {
		canceled = true;
		cancelMessage = message;
	}
	
	public void resolve() {
		resolve(null);
	}
	
	public void resolve(Player player) {
		if (canceled){
			if (player != null && cancelMessage != null) {
				MessageParser.sendMessage(player, cancelMessage);
			}
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
		if (player != null) {
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
