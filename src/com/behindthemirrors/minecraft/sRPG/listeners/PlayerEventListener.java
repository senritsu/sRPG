package com.behindthemirrors.minecraft.sRPG.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.behindthemirrors.minecraft.sRPG.ResolverPassive;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.Watcher;


public class PlayerEventListener extends PlayerListener {
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		SRPG.profileManager.add(event.getPlayer());
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		SRPG.profileManager.remove(event.getPlayer());
	}
	
	public void onItemHeldChange (PlayerItemHeldEvent event) {
		if (Settings.worldBlacklist.contains(event.getPlayer().getWorld())) {
			return;
		}
		ProfilePlayer profile = SRPG.profileManager.get(event.getPlayer());
		profile.prepared = false;
		profile.validateActives(profile.player.getInventory().getItem(event.getNewSlot()).getType());
	}
	
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		SRPG.dout("player interact entity event"+event.getRightClicked().toString(),"actives");
	}
	
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (Settings.worldBlacklist.contains(event.getPlayer().getWorld())) {
			return;
		}
		if (Watcher.isProtected(event.getItem())) {
			event.setCancelled(true);
		}
	}
	public void onPlayerMove(PlayerMoveEvent event) {
		if (Settings.worldBlacklist.contains(event.getPlayer().getWorld())) {
			return;
		}
		Block from = event.getFrom().getBlock();
		Block to = event.getTo().getBlock();
		if (from != to) {
			if (from.getType() == Material.AIR) {
				from = from.getRelative(BlockFace.DOWN);
			}
			if (to.getType() == Material.AIR) {
				to = to.getRelative(BlockFace.DOWN);
			}
			SRPG.dout("player moved from "+from.getType()+" to "+to.getType());
			ResolverPassive.resolve(SRPG.profileManager.get(event.getPlayer()),from,to);
		}
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (Settings.worldBlacklist.contains(event.getPlayer().getWorld())) {
			return;
		}
		SRPG.dout("player interact event"+event.getAction(),"actives");
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
		if (Settings.worldBlacklist.contains(event.getPlayer().getWorld())) {
			return;
		}
		Player player = event.getPlayer();
		if (!player.isSneaking()) {
			SRPG.profileManager.get(player).sneakTimeStamp = System.currentTimeMillis();
		}
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (Settings.worldBlacklist.contains(event.getPlayer().getWorld())) {
			return;
		}
		ProfilePlayer data = SRPG.profileManager.get(event.getPlayer());
		data.hp = data.hp_max;
	}
	
}
