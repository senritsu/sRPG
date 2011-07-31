package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;


public class BlockEventListener extends BlockListener {
	
	public static ArrayList<Material> trackingMaterials;
	static ArrayList<Block> userPlacedBlocks = new ArrayList<Block>();
	public static HashMap<String,ArrayList<Integer>> groupBlockMapping;
	public static HashMap<String,Double> xpChances;
	public static HashMap<String,Integer> xpValues;
	
	// check block rarity and award xp according to config
	public void onBlockBreak(BlockBreakEvent event) {
		ProfilePlayer profile = SRPG.profileManager.get(event.getPlayer());
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
			SRPG.output(xpChances.get(rarity)+" chance to get "+xpValues.get(rarity)+"xp, roll was"+roll);
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
	
}
