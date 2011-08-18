package com.behindthemirrors.minecraft.sRPG.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;


public class PlayerEventListener extends PlayerListener {
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		SRPG.profileManager.add(event.getPlayer());
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		SRPG.profileManager.remove(event.getPlayer());
	}
	
	public void onItemHeldChange (PlayerItemHeldEvent event) {
		ProfilePlayer profile = SRPG.profileManager.get(event.getPlayer());
		profile.validateActives(profile.player.getInventory().getItem(event.getNewSlot()).getType());
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ProfilePlayer profile = SRPG.profileManager.get(player);
		Material material = null;
		if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
			material = event.getClickedBlock().getType();
		}
		// block ability ready if interaction was with some interactable block
		if (player.hasPermission("srpg.actives")) {
			if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !Settings.BLOCK_CLICK_BLACKLIST.contains(material))) {
				if (!(event.isBlockInHand() && action == Action.RIGHT_CLICK_BLOCK)) {
					profile.prepare();
				}
			} else if (action == Action.LEFT_CLICK_AIR || (action == Action.LEFT_CLICK_BLOCK && !Settings.BLOCK_CLICK_BLACKLIST.contains(material))) {
				if (profile.activate()) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!player.isSneaking()) {
			SRPG.profileManager.get(player).sneakTimeStamp = System.currentTimeMillis();
		}
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		ProfilePlayer data = SRPG.profileManager.get(event.getPlayer());
		data.hp = data.hp_max;
	}
	
}
