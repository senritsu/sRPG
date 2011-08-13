package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;

public class ActiveAbility {
	
	public static boolean activate(ProfilePlayer profile, String toolName) {
		Messager.sendMessage(profile.player, "ability", toolName);
		Block block = profile.player.getTargetBlock(null, 5);
		profile.player.getWorld().strikeLightningEffect(block.getLocation());
		boolean deactivated = false; // for public build
		if (!deactivated && block.getType() != Material.AIR) {
			SRPG.output("starting cascaded break");
			for (int i=0;i<5;i++) {
				SRPG.cascadeQueueScheduler.scheduleBlockBreak(block.getRelative(i,0,0), 2*i, profile);
				if (i > 0) {
					SRPG.cascadeQueueScheduler.scheduleBlockBreak(block.getRelative(-i,0,0), 2*i, profile);
					SRPG.cascadeQueueScheduler.scheduleBlockBreak(block.getRelative(0,i,0), 2*i, profile);
					SRPG.cascadeQueueScheduler.scheduleBlockBreak(block.getRelative(0,-i,0), 2*i, profile);
					SRPG.cascadeQueueScheduler.scheduleBlockBreak(block.getRelative(0,0,i), 2*i, profile);
					SRPG.cascadeQueueScheduler.scheduleBlockBreak(block.getRelative(0,0,-i), 2*i, profile);
				}
			}
		}
		Messager.sendMessage(profile.player, "ability-not-implemented");
		return true;
	}

}
