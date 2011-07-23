package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class CascadeQueueScheduler implements Runnable {

	HashMap<Block,Integer> blocksToChange = new HashMap<Block, Integer>();
	HashMap<Block,Material> blocksChangeInto = new HashMap<Block, Material>();
	HashMap<Block,Integer> blocksToBreak = new HashMap<Block, Integer>();
	HashMap<Block,Player> blockBreakers = new HashMap<Block, Player>();
	
	public void run() {
		// remove item stacks if something was incorrectly dropped
		boolean remove = false;
		if (remove) {
			for (Entity entity : SRPG.plugin.getServer().getWorlds().get(0).getEntities()) {
				if (entity instanceof Item) {
					entity.remove();
				}
			}
		}
		// check blocks to be placed
		Iterator<Map.Entry<Block,Integer>> iterator = blocksToChange.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Block,Integer> entry = iterator.next();
			entry.setValue(entry.getValue()-1);
			if (entry.getValue() < 0) {
				Block block = entry.getKey();
				block.setType(blocksChangeInto.get(block));
				blocksChangeInto.remove(block);
				iterator.remove();
			}
		}
		iterator = blocksToBreak.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Block,Integer> entry = iterator.next();
			entry.setValue(entry.getValue()-1);
			if (entry.getValue() < 0) {
				Block block = entry.getKey();
				// send block destruction event
				if (blockBreakers.containsKey(block)) {
					//block.setType(Material.AIR);
					BlockBreakEvent event = new BlockBreakEvent(block, blockBreakers.get(block));
					SRPG.pm.callEvent(event);
					if (!event.isCancelled()) {
						ItemStack drop = Utility.getDrop(block);
						block.setType(Material.AIR);
						block.getWorld().dropItemNaturally(block.getLocation(),drop);
					}
					blockBreakers.remove(block);
				} else {
					block.setType(Material.AIR);
				}
				iterator.remove();
			}
		}

	}

}
