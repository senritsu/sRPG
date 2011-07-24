package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ActiveAbility {
	
	public static boolean activate(Player player, String toolName) {
		MessageParser.sendMessage(player, "ability", toolName);
		Block block = player.getTargetBlock(null, 5);
		player.getWorld().strikeLightningEffect(block.getLocation());
		boolean deactivated = false; // for public build
		if (!deactivated && block.getType() != Material.AIR) {
			SRPG.output("starting cascaded break");
			for (int i=0;i<5;i++) {
				SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(i,0,0), 2*i);
				SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(i,0,0), player);
				if (i > 0) {
					SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(-i,0,0), 2*i);
					SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(-i,0,0), player);
					SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(0,i,0), 2*i);
					SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(0,i,0), player);
					SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(0,-i,0), 2*i);
					SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(0,-i,0), player);
					SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(0,0,i), 2*i);
					SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(0,0,i), player);
					SRPG.cascadeQueueScheduler.blocksToBreak.put(block.getRelative(0,0,-i), 2*i);
					SRPG.cascadeQueueScheduler.blockBreakers.put(block.getRelative(0,0,-i), player);
				}
			}
		}
		MessageParser.sendMessage(player, "ability-not-implemented");
		return true;
	}

}
