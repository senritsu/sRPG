package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class Messager {
	
	static ArrayList<String> vowels = new ArrayList<String>();
	
	{
		vowels.addAll(Arrays.asList(new String[] {"a","i","u","e","o"}));
	}
	
	public static void chargeDisplay(Player player, boolean changed) {
		ProfilePlayer profile = SRPG.profileManager.get(player);
		Integer charges = profile.charges;
		Integer cost = profile.currentActive.cost;
		// check if the tool has an active ability
		String text = "[";
		if (charges >= cost) { // TODO find NPE
			text += ChatColor.DARK_GREEN + MiscGeneric.repeat("o",cost);
			text += ChatColor.WHITE + MiscGeneric.repeat("o",charges-cost);
			charges = ProfilePlayer.chargeMax - charges;
		} else {
			text += ChatColor.WHITE + MiscGeneric.repeat("o",charges);
			text += ChatColor.DARK_RED + MiscGeneric.repeat("o",cost-charges);
			charges = ProfilePlayer.chargeMax - charges - 1;
		}
		text += ChatColor.DARK_GRAY+MiscGeneric.repeat("o",charges)+ChatColor.WHITE+"]";
		// display of blocks to next charge disabled for now
		//if (charges < PlayerData.chargeMax) {
		//	text += " ("+(PlayerData.chargeTicks-data.chargeProgress.get(skillname))+" blocks to next charge)";
		//}
		if (changed) {
			text += " >";
		}
		text += " (Current "+MiscBukkit.parseSingularPlural(Settings.localization.get(profile.locale).getString("terminology.active"),1)+": "+profile.currentActive.name+")";
		player.sendMessage(text);
	}
	
	public static void sendMessage(ProfileNPC profile, String message) {
		sendMessage(profile, message, null);
	}
	
	public static void sendMessage(ProfileNPC profile, String message, String context) {
		if (profile instanceof ProfilePlayer) {
			sendMessage(((ProfilePlayer)profile).player,message, context);
		}
	}
	
	public static void sendMessage(Player player, String message) {
		sendMessage(player, message, null);
	}
	
	public static void sendMessage(Player player, String message, String context) {
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
		    		// TODO: update
		    		
		    	} else if  (match.equalsIgnoreCase("<!xp>")) {
		    		Integer currentLevel = profile.jobLevels.get(profile.currentJob);
		    		matcher.appendReplacement(sb, Integer.toString(profile.jobXP.get(profile.currentJob)-profile.currentJob.xpToNextLevel(currentLevel-1)));
		    		
		    	} else if  (match.equalsIgnoreCase("<!xp2level>")) {
		    		Integer currentLevel = profile.jobLevels.get(profile.currentJob);
		    		matcher.appendReplacement(sb, Integer.toString(currentLevel < profile.currentJob.maximumLevel ? profile.currentJob.xpToNextLevel(currentLevel) : profile.currentJob.xpToNextLevel(currentLevel-1)));
		    		
		    	} else if  (match.equalsIgnoreCase("<!job>")) {
		    		matcher.appendReplacement(sb, localizedJob(context, profile));
		    		
		    	} else if (match.equalsIgnoreCase("<!joblevel>")) {
		    		matcher.appendReplacement(sb, Integer.toString(profile.jobLevels.get(Settings.jobs.get(context))));
		    		
		    	} else if (match.equalsIgnoreCase("<!jobmaxlevel>")) {
		    		matcher.appendReplacement(sb, Integer.toString(Settings.jobs.get(context).maximumLevel));
		    		
		    	} else if (match.equalsIgnoreCase("<!cost>")) {
		    		matcher.appendReplacement(sb, context);
		    		
		    	} else if (match.equalsIgnoreCase("<!buffed>")) {
		    		StructurePassive buff = Settings.passives.get(context);
		    		String localized = localize(context,"passives."+context+".adjective",profile);
		    		matcher.appendReplacement(sb, localized != null ? localized : 
		    			(buff.adjective != null ? buff.adjective : 
		    				Settings.localization.get(profile.locale).getString("messages.buffed-default") + " " + localizedPassive(context, profile)));
		    		
		    	} else if (match.equalsIgnoreCase("<!buff>") || match.equalsIgnoreCase("<!passive>")) {
		    		matcher.appendReplacement(sb, localizedPassive(context, profile));
		    		
		    	} else if  (match.equalsIgnoreCase("<!active>")) {
		    		matcher.appendReplacement(sb, localizedActive(context,profile));
		    		
		    	} else if  (match.equalsIgnoreCase("<!charges>")) {
		    		matcher.appendReplacement(sb, profile.charges.toString());
		    		
		    	} else if  (match.equalsIgnoreCase("<!chargeprogress>")) {
		    		matcher.appendReplacement(sb, profile.chargeProgress.toString());
		    		
		    	} else if (match.startsWith("<#")) { 
		    		// TODO: update
		    		String term = match.substring(2,match.length()-1);
		    		term = term.endsWith("+") ? 
		    				MiscBukkit.parseSingularPlural(Settings.localization.get(profile.locale).getString("terminology."+term.substring(0,term.length()-1)), 2) : 
	    					MiscBukkit.parseSingularPlural(Settings.localization.get(profile.locale).getString("terminology."+term), 1);
		    		matcher.appendReplacement(sb, term);
		    		
		    	} else {
		    		// TODO: update for descriptions for passives and the sort, maybe move parsing to separate function
		    		String replacement = Settings.localization.get(profile.locale).getString(match.substring(1,match.length()-1),"");
		    		if (match.contains(":")) {
			    		// TODO: use proper java string formatting
			    		double value = Settings.advanced.getDouble(replacement.substring(0,match.indexOf(":")),0.0);
			    		String conversion = replacement.substring(match.indexOf(":")+1);
			    		if (conversion.equals("percent")) {
				    		if (value < 0.01) {
				    			replacement = "0."+Integer.toString((int)(value*1000));
				    		} else {
				    			replacement = Integer.toString((int)(value*100));
				    		}
			    		} else if (conversion.equalsIgnoreCase("hearts")) {
			    			Integer hearts = new Integer((int)(value/2));
			    			replacement = (hearts == 0 && value%2 != 0 ? "" : hearts.toString()) + (value%2 != 0 ? (hearts > 0 ? " " : "")+"1/2" : "");
			    		}
			    		
			    	}
	    			matcher.appendReplacement(sb, replacement);
		    	}
		    }
	    	matcher.appendTail(sb);
	    	
	    	// parse a/an replacement
	    	pattern = Pattern.compile("[Aa]/[Aa]n .");
		    matcher = pattern.matcher(sb.toString());
		    sb = new StringBuffer();
		    while (matcher.find()) {
		    	String match = matcher.group();
	    		String followingLetter = match.substring(match.length()-1).toLowerCase();
	    		String replacement = match.substring(0,1);
	    		if (vowels.contains(followingLetter)) {
	    			replacement += "n";
	    		} 
	    		replacement += " "+match.substring(match.length()-1,match.length());
	    		matcher.appendReplacement(sb, replacement);
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
	
	public static String localize(String string, String path) {
		return localize(string, path, null);
	}
	
	public static String localize(String string, String path, ProfilePlayer profile) {
		String locale = profile != null ? profile.locale : Settings.defaultLocale;
		if (path.startsWith("jobs")) {
			if (Settings.JOB_ALIASES.containsKey(string)) {
				string = Settings.JOB_ALIASES.get(locale).get(string);
			}
		}
		String localized = Settings.localization.get(locale).getString(path);
		return localized;
	}
	
	public String localizedPassive(String name) {
		return localizedPassive(name, null);
	}
	
	public static String localizedPassive(String name, ProfilePlayer profile) {
		String localized = localize(name,"passives."+name+".name",profile);
		return localized != null ? localized : (Settings.passives.containsKey(name) ? Settings.passives.get(name).name : "");
	}
	
	public String localizedActive(String name) {
		return localizedActive(name, null);
	}
	
	public static String localizedActive(String name, ProfilePlayer profile) {
		String localized = localize(name,"actives."+name+".name",profile);
		return localized != null ? localized : (Settings.actives.containsKey(name) ? Settings.actives.get(name).name : "");
	}
	
	public String localizedJob(String name) {
		return localizedJob(name, null);
	}
	
	public static String localizedJob(String name, ProfilePlayer profile) {
		String localized = localize(name,"jobs."+name+".name",profile);
		return localized != null ? localized : (Settings.jobs.containsKey(name) ? Settings.jobs.get(name).name : "");
	}
}
