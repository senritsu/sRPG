package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class ResolverPassive {
	
	// resolve effects that do something per tick
	public static void resolve(ProfileNPC data, StructurePassive effect, EffectDescriptor descriptor) {
		for (String name : effect.effects.keySet()) {
			if (name.startsWith("direct-damage")) {
				data.entity.damage(effect.effects.get(name).getInt("value", 0) * descriptor.potency);
			}
		}
	}
	
	// resolve effects that influence block events
	public static void resolve(ProfileNPC profile, BlockEvent event) {
		for (Map.Entry<StructurePassive,EffectDescriptor> entry : profile.getCurrentPassives().entrySet()) {
			StructurePassive passive = entry.getKey();
			EffectDescriptor descriptor = entry.getValue();
			for (String name : passive.effects.keySet()) {
				ConfigurationNode node = passive.effects.get(name);
				if (!checkConditions(profile,node,event)) {
					continue;
				}
				if (name.startsWith("drop-change")) {
					ResolverEffects.changeBlockDrops(event.getBlock(),node);
				}
			}
		}
	}
	
	// resolve static effects that influence combat
	// TODO: update
	public static void resolve(CombatInstance combat) {
		for (ProfileNPC profile : new ProfileNPC[] {combat.attacker,combat.defender}) {
			for (Map.Entry<StructurePassive,EffectDescriptor> entry : profile.getCurrentPassives().entrySet()) {
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				for (String name : passive.effects.keySet()) {
					ConfigurationNode node = passive.effects.get(name);
					if (!checkConditions(profile,node,combat)) {
						continue;
					}
					if (name.startsWith("set-combat-state")) {
						if (!((profile == combat.attacker && !node.getBoolean("self", true)) || (profile == combat.defender && !node.getBoolean("target", false)))) {
							ResolverEffects.setCombatState(combat,node);;
						}
					} else if (name.startsWith("trigger-active")) {
						SRPG.dout("trying to trigger active "+node.getString("action"),"passives");
						if (SRPG.generator.nextDouble() <= node.getDouble("chance", 1.0)) {
							ResolverActive.resolve(node.getString("action"), combat.attacker, combat.defender, descriptor);
						}
					}
				}
			}
		}
	}
	
	public static boolean checkConditions(ProfileNPC profile, ConfigurationNode node, BlockEvent event) {
		ArrayList<String> conditions = (ArrayList<String>) node.getStringList("conditions", new ArrayList<String>());
		if (conditions.isEmpty() || 
				(event instanceof BlockBreakEvent && conditions.contains("block-break")) ||
				(event instanceof BlockPlaceEvent && conditions.contains("block-place")) ) {
			return true;
		}
		return false;
	}
	
	public static boolean checkConditions(ProfileNPC profile, ConfigurationNode node, CombatInstance combat) {
		ArrayList<String> conditions = (ArrayList<String>) node.getStringList("conditions", new ArrayList<String>());
		if (conditions.isEmpty() || ( profile == combat.attacker && (
				conditions.contains("attacking") || 
				(conditions.contains("backstab-offensive") && combat.backstab) || 
				(conditions.contains("highground-offensive") && combat.highground == combat.attacker) )) ||
			( profile == combat.defender && (
				conditions.contains("defending") || 
				(conditions.contains("backstab-defensive") && combat.backstab) ||
				(conditions.contains("highground-defensive") && combat.highground == combat.defender) ))
			) {
			return true;
		}
		return false;
	}
	
	public static void recoverDurability(ProfileNPC profile) {
		if (profile instanceof ProfilePlayer) {
			Player player = ((ProfilePlayer)profile).player;
			double roll = SRPG.generator.nextDouble();
			if (roll < profile.getStat("durability-recovery-chance",player.getItemInHand().getType())){
				player.getItemInHand().setDurability((short)(player.getItemInHand().getDurability() + 1));
			}
		}
	}
}
