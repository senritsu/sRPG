package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
		if (event.isCancelled()) {
			return;
		}
		Player player = event.getPlayer();
		Material material = event.getBlock().getType();
		// check for permissions
		if (player.hasPermission("srpg.xp")) {
			// award xp
			String rarity = Settings.advanced.getString("xp.blocks.default"); 
			Iterator<Map.Entry<String,ArrayList<Integer>>> groups = groupBlockMapping.entrySet().iterator();
			while (groups.hasNext()) {
				Map.Entry<String,ArrayList<Integer>> pair = groups.next();
				if (pair.getValue().contains(material.getId())) {
					rarity = pair.getKey();
				}
			}
			if (SRPG.profileManager.get(player) != null && SRPG.generator.nextDouble() <= xpChances.get(rarity)) {
				//TODO find the NPE here
				SRPG.profileManager.get(player).addXP(xpValues.get(rarity));
				//TODO: maybe move saving to the data class
				SRPG.profileManager.save(player,"xp");
			}
		}
		// award charge
		String tool = Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(player.getItemInHand().getType());
		// check active tool and permissions
		if (tool != null && player.hasPermission("srpg.skills."+tool+".active")) {
			SRPG.profileManager.get(player).addChargeTick(tool);
			//TODO: maybe move saving to the data class
			SRPG.profileManager.save(player,"chargedata");
		}
		
		PassiveAbility.trigger(player, event);
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (trackingMaterials.contains(block.getType())) {
			userPlacedBlocks.add(block);
		}
	}
	
}
