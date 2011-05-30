package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.entity.Player;

public class ActiveAbility {
	
	public static boolean activate(Player player, String toolName) {
		MessageParser.sendMessage(player, "ability", toolName);
		player.getWorld().strikeLightningEffect(player.getTargetBlock(null, 5).getLocation());
		MessageParser.sendMessage(player, "ability-not-implemented");
		return true;
	}

}
