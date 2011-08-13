package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class ResolverEffects {

	static void setCombatState(CombatInstance combat, ConfigurationNode node) {
		if (SRPG.generator.nextDouble() < node.getDouble("chance", 1.0)) {
			if (node.getBoolean("canceled", false)) {
				combat.cancel();
			} else if (node.getBoolean("crit", false)) {
				combat.crit = true;
			} else if (node.getBoolean("parry", false)) {
				combat.parry = true;
			} else if (node.getBoolean("evade", false)) {
				combat.evade = true;
			} else if (node.getBoolean("miss", false)) {
				combat.miss = true;
			}
		}
	}
	
	static void applyBuff(ProfileNPC profile, ConfigurationNode node) {
		String name = node.getString("name");
		EffectDescriptor descriptor = new EffectDescriptor(name);
		descriptor.duration = node.getInt("duration", 0);
		StructurePassive buff = Settings.passives.get(Utility.stripPotency(name));
		profile.addEffect(buff, descriptor);
		Messager.sendMessage(profile, "acquired-buff",buff.signature);
	}
	
	static void directDamage(ProfileNPC profile, ConfigurationNode node, EffectDescriptor descriptor) {
		if (SRPG.generator.nextDouble() <= node.getDouble("chance", 1.0)) {
			profile.entity.damage(node.getInt("value", 0) * descriptor.potency);
		}
	}
	
	static void blockChange(ProfileNPC profile, Block block, ConfigurationNode node, EffectDescriptor descriptor) {
		String materialName = node.getString("change-to");
		Material material = materialName == null ? Material.AIR : Utility.parseMaterial(materialName);
		boolean temporary = node.getBoolean("temporary", false);
		boolean drop = material != Material.AIR ? false : node.getBoolean("drop", false);
		int delay = node.getInt("delay", 0);
		if (material == Material.AIR && !temporary) {
			SRPG.cascadeQueueScheduler.scheduleBlockBreak(block, delay, profile instanceof ProfilePlayer && node.getBoolean("break-event", false) ? (ProfilePlayer)profile : null, drop);
		} else if (temporary) {
			SRPG.cascadeQueueScheduler.scheduleTemporaryBlockChange(block, material, delay, node.getInt("duration", 0), node.getBoolean("protect", false));
		} else {
			SRPG.cascadeQueueScheduler.scheduleBlockChange(block, material, delay);
		}
	}

}
