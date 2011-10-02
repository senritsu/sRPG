package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.behindthemirrors.minecraft.sRPG.ResolverPassive;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.Watcher;



public class BlockEventListener extends BlockListener {
	
	public static HashMap<Material,String> materialToXpGroup = new HashMap<Material, String>();
	public static HashMap<String,Double> xpChances = new HashMap<String, Double>();
	public static HashMap<String,Integer> xpValuesMin = new HashMap<String, Integer>();
	public static HashMap<String,Integer> xpValuesRange = new HashMap<String, Integer>();
	public static HashMap<String,Integer> chargeTicks = new HashMap<String, Integer>();
	
 	// check block rarity and award xp according to config
	public void onBlockBreak(BlockBreakEvent event) {
		ProfilePlayer profile = SRPG.profileManager.get(event.getPlayer());
		Block block = event.getBlock();
		if (Watcher.protectedBlocks.contains(block)) {
			event.setCancelled(true);
		}
		if (event.isCancelled() || profile == null || Settings.worldBlacklist.contains(block.getWorld())) {
			return;
		}
		Material material = block.getType();
		String group = materialToXpGroup.get(material);
		if (Watcher.givesOk(block)) {
			// check for permissions
			if (profile.player.hasPermission("srpg.xp")) {
				// award xp
				if (SRPG.generator.nextDouble() <= xpChances.get(group)) {
					profile.addXP(xpValuesMin.get(group) + (xpValuesRange.get(group) > 0 ? SRPG.generator.nextInt(xpValuesRange.get(group)) : 0));
				}
			}
			// award charge
			if (profile.player.hasPermission("srpg.charges")) {
				//TODO: maybe move saving to the data class
				profile.addChargeTicks(chargeTicks.get(group));
				SRPG.profileManager.save(profile,"chargedata");
			}
		}
		ResolverPassive.resolve(profile, event);
		ArrayList<String> triggers = new ArrayList<String>();
		triggers.add("break");
		Watcher.checkTriggers(SRPG.profileManager.get(event.getPlayer()), triggers, block);
		ResolverPassive.recoverDurability(profile);
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (Settings.worldBlacklist.contains(block.getWorld())) {
			return;
		}
		if (Watcher.blocksToWatch.contains(block.getType())) {
			Watcher.watch(block);
		}
		ResolverPassive.resolve(SRPG.profileManager.get(event.getPlayer()), event);
		ArrayList<String> triggers = new ArrayList<String>();
		triggers.add("place");
		Watcher.checkTriggers(SRPG.profileManager.get(event.getPlayer()), triggers, event.getBlock());
	}
	
	public void onBlockCanBuild(BlockCanBuildEvent event) {
		if (!Settings.worldBlacklist.contains(event.getBlock().getWorld()) && Watcher.protectedBlocks.contains(event.getBlock())) {
			event.setBuildable(false);
			return;
		}
	}
	
}
