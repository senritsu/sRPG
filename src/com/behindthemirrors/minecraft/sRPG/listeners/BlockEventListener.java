package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.behindthemirrors.minecraft.sRPG.PassiveAbility;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;



public class BlockEventListener extends BlockListener {
	
	public static ArrayList<Material> trackingMaterials = new ArrayList<Material>();
	public static ArrayList<Block> userPlacedBlocks = new ArrayList<Block>();
	public static HashMap<String,ArrayList<Integer>> groupBlockMapping;
	public static HashMap<String,Double> xpChances;
	public static HashMap<String,Integer> xpValues;
	
	// check block rarity and award xp according to config
	public void onBlockBreak(BlockBreakEvent event) {
		ProfilePlayer profile = SRPG.profileManager.get(event.getPlayer());
		SRPG.output("trying to destroy block");
		SRPG.output(SRPG.cascadeQueueScheduler.protectedBlocks.toString());
		SRPG.output(event.getBlock().toString());
		if (SRPG.cascadeQueueScheduler.protectedBlocks.contains(event.getBlock())) {
			event.setCancelled(true);
		}
		if (event.isCancelled() || profile == null) {
			return;
		}
		Material material = event.getBlock().getType();
		// check for permissions
		if (profile.player.hasPermission("srpg.xp")) {
			// award xp
			String rarity = Settings.advanced.getString("xp.blocks.default"); 
			Iterator<Map.Entry<String,ArrayList<Integer>>> groups = groupBlockMapping.entrySet().iterator();
			while (groups.hasNext()) {
				Map.Entry<String,ArrayList<Integer>> pair = groups.next();
				if (pair.getValue().contains(material.getId())) {
					rarity = pair.getKey();
				}
			}
			double roll = SRPG.generator.nextDouble();
			if (roll <= xpChances.get(rarity)) {
				//TODO find the NPE here
				profile.addXP(xpValues.get(rarity));
			}
		}
		// award charge
		if (profile.player.hasPermission("srpg.charges")) {
			//TODO: maybe move saving to the data class
			profile.addChargeTick();
			SRPG.profileManager.save(profile,"chargedata");
		}
		PassiveAbility.trigger(profile, event);
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (trackingMaterials.contains(block.getType())) {
			userPlacedBlocks.add(block);
		}
	}
	
	public void onBlockCanBuild(BlockCanBuildEvent event) {
		if (SRPG.cascadeQueueScheduler.protectedBlocks.contains(event.getBlock())) {
			event.setBuildable(false);
			return;
		}
	}
	
}
