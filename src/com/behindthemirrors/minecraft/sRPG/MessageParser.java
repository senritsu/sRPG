package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageParser {
	
	public static void chargeDisplay(Player player) {
		ProfilePlayer profile = SRPG.profileManager.get(player);
		Integer charges = profile.charges;
		Integer cost = profile.currentActive.cost;
		// check if the tool has an active ability
		String text = "[";
		if (charges >= cost) { // TODO find NPE
			text += ChatColor.DARK_GREEN + Utility.repeat("o",cost);
			text += ChatColor.WHITE + Utility.repeat("o",charges-cost);
			charges = ProfilePlayer.chargeMax - charges;
		} else {
			text += ChatColor.WHITE + Utility.repeat("o",charges);
			text += ChatColor.DARK_RED + Utility.repeat("o",cost-charges);
			charges = ProfilePlayer.chargeMax - charges - 1;
		}
		text += ChatColor.DARK_GRAY+Utility.repeat("o",charges)+ChatColor.WHITE+"]";
		// display of blocks to next charge disabled for now
		//if (charges < PlayerData.chargeMax) {
		//	text += " ("+(PlayerData.chargeTicks-data.chargeProgress.get(skillname))+" blocks to next charge)";
		//}
		text += " (Current "+Utility.parseSingularPlural(Settings.jobsettings.getString("job-terminology.active"),1)+": "+profile.currentActive.name+")";
		player.sendMessage(text);
	}
	
	public static void sendMessage(Player player, String message) {
		sendMessage(player, message, null);
	}
	
	static void sendMessage(Player player, String message, String context) {
		ProfilePlayer profile = SRPG.profileManager.get(player);
		ArrayList<String> messageList = (ArrayList<String>)Settings.localization.get(SRPG.profileManager.get(player).locale).getStringList("messages."+message,new ArrayList<String>());
		if (messageList.isEmpty()) {
			messageList.add(Settings.localization.get(SRPG.profileManager.get(player).locale).getString("messages."+message,"Error in localization file, contact your admin about message '"+message+"'"));
		}
		
		if (Settings.localization.get(SRPG.profileManager.get(player).locale).getStringList("messages.randomize", (new ArrayList<String>())).contains(message)) {
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
		    		matcher.appendReplacement(sb, Integer.toString(profile.jobLevels.get(profile.currentJob)));
		    		
		    	} else if  (match.equalsIgnoreCase("<!xp>")) {
		    		Integer currentLevel = profile.jobLevels.get(profile.currentJob);
		    		matcher.appendReplacement(sb, Integer.toString(profile.jobXP.get(profile.currentJob)-profile.currentJob.xpToNextLevel(currentLevel-1)));
		    		
		    	} else if  (match.equalsIgnoreCase("<!xp2level>")) {
		    		Integer currentLevel = profile.jobLevels.get(profile.currentJob);
		    		matcher.appendReplacement(sb, Integer.toString(currentLevel < profile.currentJob.maximumLevel ? profile.currentJob.xpToNextLevel(currentLevel) : profile.currentJob.xpToNextLevel(currentLevel-1)));
		    		
		    	} else if  (match.equalsIgnoreCase("<!skillname>")) {
		    		matcher.appendReplacement(sb, Settings.localization.get(profile.locale).getString("skills."+context));
		    		
		    	} else if  (match.equalsIgnoreCase("<!cost>")) {
		    		matcher.appendReplacement(sb, context);
		    		
		    	} else if  (match.equalsIgnoreCase("<!ability>")) {
		    		matcher.appendReplacement(sb, Settings.localization.get(profile.locale).getString("active-abilities."+context));
		    		
		    	} else if  (match.equalsIgnoreCase("<!charges>")) {
		    		matcher.appendReplacement(sb, profile.charges.toString());
		    		
		    	} else if  (match.equalsIgnoreCase("<!chargeprogress>")) {
		    		matcher.appendReplacement(sb, profile.chargeProgress.toString());
		    		
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
		    		
		    	} else {
		    		String replacement = Settings.localization.get(profile.locale).getString(match.substring(1,match.length()-1),"");
		    		if (!replacement.isEmpty()) {
		    			matcher.appendReplacement(sb, replacement);
		    		}
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
