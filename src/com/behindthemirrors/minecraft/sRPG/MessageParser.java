package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageParser {
	
	public static void chargeDisplay(Player player) {
		String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(player.getItemInHand().getType());
		if (toolName != null) {
			String skillname = toolName.substring(0, toolName.indexOf("."));
			PlayerData data = SRPG.playerDataManager.get(player);
			Integer charges = data.charges.get(skillname);
			Integer cost = PlayerData.abilityCosts.get(toolName);
			// check if the tool has an active ability
			if (cost==null) {
				return;
			}
			String text = "[";
			if (charges >= cost) { // TODO find NPE
				text += ChatColor.DARK_GREEN + Utility.repeat("o",cost);
				text += ChatColor.WHITE + Utility.repeat("o",charges-cost);
				charges = PlayerData.chargeMax - charges;
			} else {
				text += ChatColor.WHITE + Utility.repeat("o",charges);
				text += ChatColor.DARK_RED + Utility.repeat("o",cost-charges);
				charges = PlayerData.chargeMax - charges - 1;
			}
			text += ChatColor.DARK_GRAY+Utility.repeat("o",charges)+ChatColor.WHITE+"]";
			// display of blocks to next charge disabled for now
			//if (charges < PlayerData.chargeMax) {
			//	text += " ("+(PlayerData.chargeTicks-data.chargeProgress.get(skillname))+" blocks to next charge)";
			//}
			player.sendMessage(text);
		}
	}
	
	public static void sendMessage(Player player, String message) {
		sendMessage(player, message, null);
	}
	
	static void sendMessage(Player player, String message, String context) {
		PlayerData data = SRPG.playerDataManager.get(player);
		ArrayList<String> messageList = (ArrayList<String>)Settings.localization.get(SRPG.playerDataManager.get(player).locale).getStringList("messages."+message,new ArrayList<String>());
		if (messageList.isEmpty()) {
			messageList.add(Settings.localization.get(SRPG.playerDataManager.get(player).locale).getString("messages."+message,"Error in localization file, contact your admin about message '"+message+"'"));
		}
		
		if (Settings.localization.get(SRPG.playerDataManager.get(player).locale).getStringList("messages.randomize", (new ArrayList<String>())).contains(message)) {
			String choice = messageList.get(SRPG.generator.nextInt(messageList.size()));
			messageList.clear();
			messageList.add(choice);
		}
		
		for (String line : messageList) {
			// parse variables and localization references
			Pattern pattern = Pattern.compile("<[!%#\\w\\.-]+>");
		    Matcher matcher = pattern.matcher(line);
		    StringBuffer sb = new StringBuffer();
		    while (matcher.find()) {
		    	// check for supported variables first
		    	String match = matcher.group();
		    	if (match.equalsIgnoreCase("<!level>")) {
		    		matcher.appendReplacement(sb, Integer.toString(data.free + data.spent));
		    		
		    	} else if  (match.equalsIgnoreCase("<!xp>")) {
		    		matcher.appendReplacement(sb, Integer.toString(data.xp%PlayerData.xpToLevel));
		    		
		    	} else if  (match.equalsIgnoreCase("<!xp2level>")) {
		    		matcher.appendReplacement(sb, PlayerData.xpToLevel.toString());
		    		
		    	} else if  (match.equalsIgnoreCase("<!free>")) {
		    		matcher.appendReplacement(sb, data.free.toString());
		    		
		    	} else if  (match.equalsIgnoreCase("<!skillname>")) {
		    		matcher.appendReplacement(sb, Settings.nameReplacements.get(data.locale).get("skills."+context));
		    		
		    	} else if  (match.equalsIgnoreCase("<!skillpoints>")) {
		    		matcher.appendReplacement(sb, data.skillpoints.get(context).toString());
		    		
		    	} else if  (match.equalsIgnoreCase("<!cost>")) {
		    		matcher.appendReplacement(sb, context);
		    		
		    	} else if  (match.equalsIgnoreCase("<!ability>")) {
		    		matcher.appendReplacement(sb, Settings.nameReplacements.get(data.locale).get("active-abilities."+context));
		    		
		    	} else if  (match.equalsIgnoreCase("<!milestone>")) {
		    		ArrayList<String> milestones = data.getMilestones(context);
		    		matcher.appendReplacement(sb, Settings.nameReplacements.get(data.locale).get("milestones."+milestones.get(milestones.size()-1)));
		    		
		    	} else if  (match.equalsIgnoreCase("<!charges>")) {
		    		matcher.appendReplacement(sb, data.charges.get(context).toString());
		    		
		    	} else if  (match.equalsIgnoreCase("<!chargeprogress>")) {
		    		matcher.appendReplacement(sb, data.chargeProgress.get(context).toString());
		    		
		    	} else if (match.startsWith("<#")) { 
		    		matcher.appendReplacement(sb, Settings.advanced.getString(match.substring(2,match.length()-1)));
		    		
		    	} else if (match.startsWith("<%")) {
		    		// hack, replace with proper float string conversion later
		    		double value = Settings.advanced.getDouble(match.substring(2,match.length()-1),0.0);
		    		String result = "";
		    		if (value < 0.01) {
		    			result = "0."+Integer.toString((int)(value*1000));
		    		} else {
		    			result = Integer.toString((int)(value*100));
		    		}
		    		matcher.appendReplacement(sb, result+"%");
		    		
		    	} else if (Settings.nameReplacements.get(data.locale).containsKey(match.substring(1,match.length()-1))) {
		    			matcher.appendReplacement(sb, Settings.nameReplacements.get(data.locale).get(match.substring(1,match.length()-1)));
		    	}
		    }
	    	matcher.appendTail(sb);
	    	
	    	// parse color codes
	    	pattern = Pattern.compile("\\[\\w+]");
		    matcher = pattern.matcher(sb.toString());
		    sb = new StringBuffer();
		    while (matcher.find()) {
		    	if (Settings.colorMap.containsKey(matcher.group())) {
		    		matcher.appendReplacement(sb, Settings.colorMap.get(matcher.group()));
		    	}
		    }
		    matcher.appendTail(sb);
		    
		    player.sendMessage(sb.toString());
		}
	}
}
