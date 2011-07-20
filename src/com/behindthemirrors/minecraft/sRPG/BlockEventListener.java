package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockBreakEvent;


public class BlockEventListener extends BlockListener {
	
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
		if (SRPG.permissionHandler.has(player, "srpg.xp")) {
			// award xp
			String rarity = Settings.advanced.getString("xp.blocks.default-group"); 
			Iterator<Map.Entry<String,ArrayList<Integer>>> groups = groupBlockMapping.entrySet().iterator();
			while (groups.hasNext()) {
				Map.Entry<String,ArrayList<Integer>> pair = groups.next();
				if (pair.getValue().contains(material.getId())) {
					rarity = pair.getKey();
				}
			}
			if (SRPG.playerDataManager.get(player) != null && SRPG.generator.nextDouble() <= xpChances.get(rarity)) {
				//TODO find the NPE here
				SRPG.playerDataManager.get(player).addXP(xpValues.get(rarity));
				//TODO: maybe move saving to the data class
				SRPG.playerDataManager.save(player,"xp");
			}
		}
		// award charge
		String tool = Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(player.getItemInHand().getType());
		// check active tool and permissions
		if (tool != null && SRPG.permissionHandler.has(player, "srpg.skills."+tool+".active")) {
			SRPG.playerDataManager.get(player).addChargeTick(tool);
			//TODO: maybe move saving to the data class
			SRPG.playerDataManager.save(player,"chargedata");
		}
		
		PassiveAbility.trigger(player, event);
	}
	
}
