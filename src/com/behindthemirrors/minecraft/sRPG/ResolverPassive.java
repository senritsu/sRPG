package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ArgumentsActive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.TriggerEffect;
import com.behindthemirrors.minecraft.sRPG.dataStructures.Watcher;


public class ResolverPassive {
	
	// resolve effects that do something per tick
	public static void resolve(ProfileNPC profile, StructurePassive passive, EffectDescriptor descriptor) {
		for (String name : passive.effects.keySet()) {
			ConfigurationNode node = passive.effects.get(name);
			SRPG.dout("checking conditions for "+name+", used by "+passive.name,"passives");
			if (!checkConditions(profile,node) || !checkTools(profile,node)) {
				SRPG.dout("conditions failed","passives");
				continue;
			}
			List<String> levelbased = node.getStringList("level-based",new ArrayList<String>());
			if (!(SRPG.generator.nextDouble() <= (levelbased.contains("chance")?descriptor.levelfactor():1.0)*node.getDouble("chance", 1.0))) {
				continue;
			}
			SRPG.dout("conditions cleared","passives");
			if (name.startsWith("direct-damage")) {
				ResolverEffects.directDamage(profile, node, descriptor);
			}
		}
	}
	
	public static void resolve(ProfileNPC profile, TriggerEffect effect, ProfileNPC target, Block block, CombatInstance combat) {
		ConfigurationNode node = effect.node;
		ArgumentsActive arguments = new ArgumentsActive(node.getString("action"), profile, effect.descriptor);
		arguments.target = target;
		arguments.targetBlock = block;
		arguments.combat = combat;
		arguments.complete();
		ArrayList<Material> validFrom = MiscBukkit.parseMaterialList(node.getStringList("from", new ArrayList<String>()));
		if ((!validFrom.isEmpty() && !validFrom.contains(arguments.sourceBlock.getType())) || !(checkConditions(profile,node) || checkCombatConditions(profile, node, combat)) || !checkTools(profile,node,arguments.targetBlock)) {
			SRPG.dout("conditions failed","passives");
			return;
		}
		List<String> levelbased = node.getStringList("level-based",new ArrayList<String>());
		if (!(SRPG.generator.nextDouble() <= (levelbased.contains("chance")?effect.descriptor.levelfactor():1.0)*node.getDouble("chance", 1.0))) {
			return;
		}
		SRPG.dout("conditions cleared","passives");
		
		
		ResolverActive.resolve(arguments);
	}
	
	// resolve effects that influence block events
	public static void resolve(ProfileNPC profile, BlockEvent event) {
		if (profile == null) {
			return;
		}
		Block block = event.getBlock();
		for (Map.Entry<StructurePassive,EffectDescriptor> entry : profile.getCurrentPassives().entrySet()) {
			StructurePassive passive = entry.getKey();
			EffectDescriptor descriptor = entry.getValue();
			for (String name : passive.effects.keySet()) {
				ConfigurationNode node = passive.effects.get(name);
				SRPG.dout("checking conditions for "+name+", used by "+passive.name,"passives");
				if (!(checkConditions(profile,node) || checkBlockConditions(profile,node,event)) || !checkTools(profile,node,block)) {
					SRPG.dout("conditions failed","passives");
					continue;
				}
				List<String> levelbased = node.getStringList("level-based",new ArrayList<String>());
				if (!(SRPG.generator.nextDouble() <= (levelbased.contains("chance")?descriptor.levelfactor():1.0)*node.getDouble("chance", 1.0))) {
					continue;
				}
				SRPG.dout("conditions cleared","passives");
				if (event instanceof BlockBreakEvent && name.startsWith("drop-change")) {
					ResolverEffects.changeBlockDrops((BlockBreakEvent)event,block,node, descriptor);
				}
			}
		}
	}
	
	// resolve static effects that influence combat
	// TODO: update
	public static void resolveCombatBoosts(CombatInstance combat) {
		for (ProfileNPC profile : new ProfileNPC[] {combat.attacker,combat.defender}) {
			if (profile == null) {
				continue;
			}
			for (Map.Entry<StructurePassive,EffectDescriptor> entry : profile.getCurrentPassives().entrySet()) {
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				for (String name : passive.effects.keySet()) {
					ConfigurationNode node = passive.effects.get(name);
					SRPG.dout("checking conditions for "+name+", used by "+passive.name,"passives");
					if (!(checkConditions(profile,node) || checkCombatConditions(profile,node,combat)) || !checkTools(profile, node,combat)) {
						SRPG.dout("conditions failed","passives");
						continue;
					}
					List<String> levelbased = node.getStringList("level-based",new ArrayList<String>());
					if (!(SRPG.generator.nextDouble() <= (levelbased.contains("chance")?descriptor.levelfactor():1.0)*node.getDouble("chance", 1.0))) {
						continue;
					}
					SRPG.dout("conditions cleared","passives");
					if (name.startsWith("boost")) {
						if (!((profile == combat.attacker && !node.getBoolean("self", true)) || (profile == combat.defender && !node.getBoolean("target", false)))) {
							ResolverEffects.combatBoost(combat,node,descriptor);;
						}
					}
				}
			}
		}
	}
	
	public static void resolve(CombatInstance combat) {
		ArrayList<String> triggers = new ArrayList<String>();
		triggers.add("combat");
		for (ProfileNPC profile : new ProfileNPC[] {combat.attacker,combat.defender}) {
			if (profile == null) {
				continue;
			}
			for (Map.Entry<StructurePassive,EffectDescriptor> entry : profile.getCurrentPassives().entrySet()) {
				StructurePassive passive = entry.getKey();
				EffectDescriptor descriptor = entry.getValue();
				for (String name : passive.effects.keySet()) {
					ConfigurationNode node = passive.effects.get(name);
					SRPG.dout("checking conditions for "+name+", used by "+passive.name,"passives");
					if (!(checkConditions(profile,node) || checkCombatConditions(profile,node,combat)) || !checkTools(profile, node,combat)) {
						SRPG.dout("conditions failed","passives");
						continue;
					}
					List<String> levelbased = node.getStringList("level-based",new ArrayList<String>());
					if (!(SRPG.generator.nextDouble() <= (levelbased.contains("chance")?descriptor.levelfactor():1.0)*node.getDouble("chance", 1.0))) {
						continue;
					}
					SRPG.dout("conditions cleared","passives");
					if (name.startsWith("set-combat-state")) {
						if (!((profile == combat.attacker && !node.getBoolean("self", true)) || (profile == combat.defender && !node.getBoolean("target", false)))) {
							ResolverEffects.setCombatState(combat,node);;
						}
					}
				}
			}
			Watcher.checkTriggers(profile, triggers, profile == combat.attacker ? combat.defender : combat.attacker, combat);
		}
	}
	
	public static boolean checkConditions(ProfileNPC profile, ConfigurationNode node) {
		ArrayList<String> conditions = (ArrayList<String>) node.getStringList("conditions", new ArrayList<String>());
		return conditions.isEmpty() || checkGenericConditions(profile,conditions);
	}
	
	public static boolean checkBlockConditions(ProfileNPC profile, ConfigurationNode node, BlockEvent event) {
		ArrayList<String> conditions = (ArrayList<String>) node.getStringList("conditions", new ArrayList<String>());
		if ((event instanceof BlockBreakEvent && conditions.contains("block-break")) ||
			(event instanceof BlockPlaceEvent && conditions.contains("block-place"))) {
			return true;
		}
		return false;
	}
	
	public static boolean checkCombatConditions(ProfileNPC profile, ConfigurationNode node, CombatInstance combat) {
		ArrayList<String> conditions = (ArrayList<String>) node.getStringList("conditions", new ArrayList<String>());
		if (( profile == combat.attacker && (
				conditions.contains("attacking") || 
				(conditions.contains("backstab-offensive") && combat.backstab) || 
				(conditions.contains("highground-offensive") && combat.highground == combat.attacker) ||
				(conditions.contains("crit-offensive") && combat.crit) ||
				(conditions.contains("miss-offensive") && combat.miss) ||
				(conditions.contains("evade-offensive") && combat.evade) ||
				(conditions.contains("parry-offensive") && combat.parry) )) ||
			( profile == combat.defender && (
				conditions.contains("defending") || 
				(conditions.contains("backstab-defensive") && combat.backstab) ||
				(conditions.contains("highground-defensive") && combat.highground == combat.defender) ||
				(conditions.contains("crit-defensive") && combat.crit) ||
				(conditions.contains("miss-defensive") && combat.miss) ||
				(conditions.contains("evade-defensive") && combat.evade) ||
				(conditions.contains("parry-defensive") && combat.parry) )) ) {
			return true;
		}
		return false;
	}
	
	public static boolean checkGenericConditions(ProfileNPC profile, ArrayList<String> conditions) {
		double time = profile.entity.getWorld().getTime();
		Biome biome = profile.entity.getWorld().getBiome(profile.entity.getLocation().getBlockX(), profile.entity.getLocation().getBlockZ());
		if ((conditions.contains("day") && time > 0 && time < 13000) || 
			(conditions.contains("night") && time > 13000 && time < 24000) ||
			(conditions.contains("rain") && profile.entity.getWorld().hasStorm() && biome != Biome.DESERT && biome != Biome.TUNDRA && biome != Biome.TAIGA && biome != Biome.ICE_DESERT) ||
			(conditions.contains("snow") && profile.entity.getWorld().hasStorm() && (biome == Biome.TUNDRA || biome == Biome.TAIGA)) ||
			(conditions.contains("clear") && (!profile.entity.getWorld().hasStorm() || biome == Biome.DESERT ||biome == Biome.ICE_DESERT)) ) {
			return true;
		} else {
			SRPG.dout("generic check failed","passives");
			return false;
		}
	}
	
	public static boolean checkTools(ProfileNPC profile, ConfigurationNode node) {
		return checkTools(node,profile instanceof ProfilePlayer?((ProfilePlayer)profile).player.getItemInHand().getType():null,null);
	}
	
	public static boolean checkTools(ProfileNPC profile, ConfigurationNode node, CombatInstance combat) {
		return checkTools(node,profile == combat.attacker?combat.attackerHandItem:combat.defenderHandItem,profile==combat.defender?combat.defenderHandItem:combat.attackerHandItem);
	}
	
	public static boolean checkTools(ProfileNPC profile, ConfigurationNode node, Block block) {
		return checkTools(node,(profile instanceof ProfilePlayer)?((ProfilePlayer)profile).player.getItemInHand().getType() : null, block.getType());
	}
	
	public static boolean checkTools(ConfigurationNode node, Material material, Material versus) {
		ArrayList<Material> validMaterials = MiscBukkit.parseMaterialList(node.getStringList("tools", new ArrayList<String>()));
		ArrayList<Material> versusMaterials = MiscBukkit.parseMaterialList(node.getStringList("versus", new ArrayList<String>()));
		if ((validMaterials.isEmpty() || material == null || validMaterials.contains(material)) && (versusMaterials.isEmpty() || versus == null || versusMaterials.contains(versus))) {
			return true;
		} else {
			SRPG.dout("tool check failed","passives");
			return false;
		}
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
