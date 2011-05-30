package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;


public class PlayerEventListener extends PlayerListener {
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		SRPG.playerDataManager.add(event.getPlayer());
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		SRPG.playerDataManager.remove(event.getPlayer());
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		Material material = null;
		if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
			material = event.getClickedBlock().getType();
		}
		// block ability ready if interaction was with some interactable block
		String tool = Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(player.getItemInHand().getType());
		if (tool != null && SRPG.permissionHandler.has(player, "skills."+tool+".active")) {
			if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !Settings.BLOCK_CLICK_BLACKLIST.contains(material))) {
				MessageParser.chargeDisplay(player);
				SRPG.playerDataManager.get(player).readyAbility(player.getItemInHand().getType());
			} else if (action == Action.LEFT_CLICK_AIR || (action == Action.LEFT_CLICK_BLOCK && !Settings.BLOCK_CLICK_BLACKLIST.contains(material))) {
				SRPG.playerDataManager.get(player).activateAbility(player.getItemInHand().getType());
			}
		}
	}
	
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!player.isSneaking()) {
			SRPG.playerDataManager.get(player).sneakTimeStamp = System.currentTimeMillis();
		}
	}
}
