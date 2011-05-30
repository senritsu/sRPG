package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockBreakEvent;


public class BlockEventListener extends BlockListener {
	
	public static ArrayList<ArrayList<Integer>> blockRarities;
	public static ArrayList<Double> xpChances;
	public static ArrayList<Integer> xpValues;
	
	// check block rarity and award xp according to config
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Material material = event.getBlock().getType();
		// check for permissions
		if (SRPG.permissionHandler.has(player, "srpg.xp")) {
			// award xp
			int rarity = 0; 
			for (int i=1;i<3;i++) {
				if (blockRarities.get(i).contains(material.getId())) {
					rarity = i;
				}
			}
			if (SRPG.generator.nextDouble() <= xpChances.get(rarity)) {
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
