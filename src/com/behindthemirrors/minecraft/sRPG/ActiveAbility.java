package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ActiveAbility {
	
	public static boolean activate(Player player, String toolName) {
		MessageParser.sendMessage(player, "ability", toolName);
		Block block = player.getTargetBlock(null, 5);
		player.getWorld().strikeLightningEffect(block.getLocation());
		boolean deactivated = true; // for public build
		if (!deactivated && block.getType() != Material.AIR) {
			for (int i=0;i<10;i++) {
				SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(i,0,0), i);
				SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(i,0,0), player);
			}
		}
		MessageParser.sendMessage(player, "ability-not-implemented");
		return true;
	}

}
