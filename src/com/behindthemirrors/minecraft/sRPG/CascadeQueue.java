package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.behindthemirrors.minecraft.sRPG.dataStructures.BlockChangeDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;

public class CascadeQueue implements Runnable {

	ArrayList<BlockChangeDescriptor> queue = new ArrayList<BlockChangeDescriptor>();
	public ArrayList<Block> protectedBlocks = new ArrayList<Block>();
	
	public void run() {
		ArrayList<BlockChangeDescriptor> additions = new ArrayList<BlockChangeDescriptor>();
		Iterator<BlockChangeDescriptor> iterator = queue.iterator();
		while (iterator.hasNext()) {
			BlockChangeDescriptor descriptor = iterator.next();  
			descriptor.ticksToChange--;
			if (descriptor.ticksToChange <= 0) {
				boolean canceled = false;
				Block block = descriptor.targetState.getBlock();
				if (block.getType() != Material.AIR && descriptor.targetState.getType() == Material.AIR) {
					// send block destruction event
					if (descriptor.cause != null) {
						BlockBreakEvent event = new BlockBreakEvent(block, descriptor.cause.player);
						SRPG.pm.callEvent(event);
						canceled = event.isCancelled();
					} 
					// drop items
					if (!canceled && descriptor.drop) { 
						ItemStack item = Utility.getNaturalDrops(block);
						if (item != null) {
							block.getWorld().dropItemNaturally(block.getLocation(),item);
						}
					}
				}
				// change block
				if (!canceled) {
					descriptor.targetState.update(descriptor.force);
				
					if (descriptor.revert) {
						// add reverse change state
						BlockChangeDescriptor revertDescriptor = new BlockChangeDescriptor(block.getState(),descriptor.ticksToRevert);
						revertDescriptor.force = false;
						additions.add(revertDescriptor);
						if (descriptor.protect) {
							protectedBlocks.add(block);
						}
					}
				}
				if (protectedBlocks.contains(block)) {
					protectedBlocks.remove(block);
				}
				iterator.remove();
			}
		}
		queue.addAll(additions);
	}

	public void scheduleBlockBreak(Block block, int delay) {
		scheduleBlockBreak(block, delay, null, false);
	}
	
	public void scheduleBlockBreak(Block block, int delay, boolean drop) {
		scheduleBlockBreak(block, delay, null, drop);
	}
	
	public void scheduleBlockBreak(Block block, int delay, ProfilePlayer cause) {
		scheduleBlockBreak(block, delay, cause, true);
	}
	
	public void scheduleBlockBreak(Block block, int delay, ProfilePlayer cause, boolean drop) {
		BlockState state = block.getState();
		state.setType(Material.AIR);
		state.setData(new MaterialData(Material.AIR));
		BlockChangeDescriptor descriptor = new BlockChangeDescriptor(state, delay, cause);
		descriptor.drop = drop;
		queue.add(descriptor);
	}
	
	public void scheduleTemporaryBlockChange(Block block, Material replacement, int delay, int revertDelay, boolean protect) {
		BlockState state = block.getState();
		state.setType(replacement);
		state.setData(new MaterialData(replacement));
		BlockChangeDescriptor descriptor = new BlockChangeDescriptor(state, delay, revertDelay);
		descriptor.protect = protect;
		queue.add(descriptor);
	}
	
	public void scheduleBlockChange(Block block, Material replacement, int delay) {
		BlockState state = block.getState();
		state.setType(replacement);
		state.setData(new MaterialData(replacement));
		BlockChangeDescriptor descriptor = new BlockChangeDescriptor(state, delay);
		queue.add(descriptor);
	}
	
}
