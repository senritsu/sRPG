package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;
import org.getspout.spoutapi.SpoutManager;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureActive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class Messager {
	
	static ArrayList<String> vowels = new ArrayList<String>();
	
	static {
		vowels.addAll(Arrays.asList(new String[] {"a","i","u","e","o"}));
	}
	
	static HashMap<Character,Integer> characterSizes = new HashMap<Character, Integer>();
	
	static {
		String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!\"#%&'()*+,-./:;<=>?@[\\]^_{|}~ ";
		Integer[] sizes = new Integer[] {5,5,5,5,5,5,5,5,5,5,
				5,5,5,5,5,4,5,5,1,5,4,2,5,5,5,5,5,5,5,3,5,5,5,5,5,5,
				5,5,5,5,5,5,5,5,3,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
				1,4,5,5,5,2,4,4,4,5,1,5,1,5,1,1,4,5,4,5,5,3,5,3,5,5,4,1,4,6,3};
		for (int i=0;i<chars.length();i++) {
			characterSizes.put(chars.charAt(i), sizes[i]+1);
		}
	}
	
	static HashMap<String,String> colorMap = new HashMap<String, String>();
	static {
		// initialize color tag replacement map
		String[] colorStrings = new String[] {
				"[aqua]","[black]","[blue]","[dark aqua]",
				"[dark blue]","[dark gray]","[dark green]","[dark purple]",
				"[dark red]","[gold]","[gray]","[green]",
				"[light purple]","[red]","[white]","[yellow]",
				"[]"
				};
		ChatColor[] chatColors = new ChatColor[] {
				ChatColor.AQUA,ChatColor.BLACK,ChatColor.BLUE,ChatColor.DARK_AQUA,
				ChatColor.DARK_BLUE,ChatColor.DARK_GRAY,ChatColor.DARK_GREEN,ChatColor.DARK_PURPLE,
				ChatColor.DARK_RED,ChatColor.GOLD,ChatColor.GRAY,ChatColor.GREEN,
				ChatColor.LIGHT_PURPLE,ChatColor.RED,ChatColor.WHITE,ChatColor.YELLOW,
				ChatColor.WHITE
				};
		for (int i=0;i<colorStrings.length;i++) {
			colorMap.put(colorStrings[i], chatColors[i].toString());
		}
	}
	
	public static int length(String string) {
		int length = 0;
		for (int i=0;i<string.length();i++) {
			length += characterSizes.get(string.charAt(i));
		}
		return length;
	}
	
	public static String columnize(ArrayList<String> strings,ArrayList<Integer> distances) {
		String string = "";
		int spillover = 0;
		for (int i = 0;i<strings.size();i++) {
			String addition = strings.get(i);
			int distance = distances.get(i);
			int length = length(addition);
			while (length > distance) {
				addition = addition.substring(0, addition.length()-1);
				length = length(addition);
			}
			string += addition;
			int spaceToFill = (distance - length + 2) + spillover;
			string += MiscGeneric.repeat(" ", spaceToFill / 4);
			spillover += spaceToFill%4;
		}
		return string.trim();
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
		if (changed) {
			text += " >";
		}
		text += " (Current "+MiscBukkit.parseSingularPlural(Settings.localization.get(profile.locale).getString("terminology.active"),1)+": "+profile.currentActive.name+")";
		player.sendMessage(text);
	}
	
	public static void announce(StructureActive active, ProfilePlayer profile) {
		Location location = profile.player.getLocation();
		String feedback = parseLine(profile, localize(active.feedback,"actives."+active.signature+".feedback",profile),active.signature);
		if (!feedback.isEmpty()) {
			profile.player.sendMessage(feedback);
		}
		if (active.broadcastRange > 0) {
			String message = parseLine(profile, localize(active.broadcast,"actives."+active.signature+".broadcast",profile),active.signature);
			if (!message.isEmpty()) {
				for (Player player : location.getWorld().getPlayers()) {
					if (player != profile.player && player.getLocation().distance(location) <= active.broadcastRange) {
						player.sendMessage(message);
					}
				}
			}
		}
	}
	
	public static void notify(ProfilePlayer profile, String message) {
		notify(profile,message,null, Material.AIR);
	}
	
	public static void notify(ProfilePlayer profile, String message, String context, Material material) {
		Player player = ((ProfilePlayer)profile).player;
		ArrayList<String> notificationList = (ArrayList<String>)Settings.localization.get(profile.locale).getStringList("notifications."+message,new ArrayList<String>());
		if (notificationList.isEmpty()) {
			notificationList.add(Settings.localization.get(profile.locale).getString("notifications."+message,"Error in localization file, contact your admin about notification '"+message+"'"));
		}
		// TODO: change to properly use both parts of the notification
		SpoutManager.getPlayer(player).sendNotification(parseLine(profile,notificationList.get(SRPG.generator.nextInt(notificationList.size())),context), context, material);
	}
	
	public static ArrayList<String> documentPassive(ProfilePlayer profile, StructurePassive passive) {
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(parseLine(profile,localize(passive.signature,"autodoc.special.passive-header",profile),passive.signature));
		if (passive.description != null && !passive.description.isEmpty()) {
			lines.add(parseLine(profile,passive.description,passive.signature));
		} else {
			for (Entry<String,ConfigurationNode> entry : passive.effects.entrySet()) {
				ConfigurationNode node = entry.getValue();
				if (!node.getBoolean("documented", true)) {
					continue;
				}
				lines.addAll(documentEffect(profile, entry.getKey(), node));
			}
		}
		return lines;
	}
	
	public static ArrayList<String> documentEffect(ProfilePlayer profile, String name, ConfigurationNode node) {
		SRPG.dout("getting documentation for effect: "+name);
		ArrayList<String> description = new ArrayList<String>();
		if (name.startsWith("boost")) {
			String stat = node.getString("name");
			String value = node.getString("value");
			String line = parseLine(profile,localize("","autodoc.effects.boost."+stat,profile),value);
			if (!line.isEmpty()){
				description.add(line);
			}
		}
		
		if (!description.isEmpty()) {
			for (String string : new String[] {"tools","versus"}) {
				List<String> tools = node.getStringList(string,new ArrayList<String>());
				ArrayList<String> names = new ArrayList<String>();
				for (String material : tools) {
					String tool = "";
					try {
						material = Material.getMaterial(Integer.parseInt(material)).toString();
					} catch (NumberFormatException ex) {
						if (Material.getMaterial(material.toUpperCase()) != null) {
							material = Material.getMaterial(material.toUpperCase()).toString();
						}
					}
					material = material.replaceAll("_"," ");
					for (String token : material.split(" ")) {
						token = token.toLowerCase();
						tool += token.substring(0, 1).toUpperCase() + token.substring(1);
					}
					names.add(tool);
				}
				if (!tools.isEmpty()) {
					description.add(parseLine(profile, localize("","autodoc.special."+string,profile),MiscGeneric.join(names, ", ")));
				}
			}
		}
			
		return description;
	}
	
	public static void sendMessage(ProfileNPC profile, String message) {
		if (profile instanceof ProfilePlayer) {
			sendMessage(((ProfilePlayer)profile).player, message, null, false);
		}
	}
	
	public static void sendMessage(ProfileNPC profile, String message, String context) {
		if (profile instanceof ProfilePlayer) {
			sendMessage(((ProfilePlayer)profile).player,message, context, false);
		}
	}
	
	public static void sendMessage(Player player, String message) {
		sendMessage(player, message, null, false);
	}
	
	public static void sendMessage(Player player, String message, String context) {
		sendMessage(player, message, context, false);
	}
	
	public static void sendMessage(Player player, String message, String context, boolean columns) {
		for (String line : parseMessage(player,message,context,columns)) {
			// parse variables and localization references
		    player.sendMessage(line);
		}
	}
	
	public static ArrayList<String> parseMessage(Player player, String message, String context, boolean columns) {
		
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
		ArrayList<String> output = new ArrayList<String>();
		if (columns) {
			ArrayList<Integer> spacing = (ArrayList<Integer>)Arrays.asList(new Integer[]{160,160});
			ArrayList<ArrayList<String>> buffer = new ArrayList<ArrayList<String>>();
			buffer.add(new ArrayList<String>());
			int index = 0;
			for (String line : messageList) {
				if (buffer.get(index).size() >= 2) {
					buffer.add(new ArrayList<String>());
					index++;
				}
				buffer.get(index).add(parseLine(profile, line, context));
			}
			if (buffer.get(index).size() < 2) {
				buffer.get(index).add("");
			}
			for (ArrayList<String> row : buffer) {
				output.add(columnize(row, spacing));
			}
			
		} else {
			for (String line : messageList) {
				output.add(parseLine(profile,line,context));
			}
			
		}
		return output;
	}
	
	public static String parseLine(ProfilePlayer profile, String line, String context) {
		Pattern pattern = Pattern.compile("<[!%#\\w\\.-:\\+]+>");
	    Matcher matcher = pattern.matcher(line);
	    StringBuffer sb = new StringBuffer();
	    while (matcher.find()) {
	    	// check for supported variables first
	    	String match = matcher.group();
	    	String replacement = "";
	    	String format = "";
	    	if (match.contains(":")) {
	    		format = match.substring(match.indexOf(":")+1,match.length()-1);
	    		match = match.substring(0,match.indexOf(":"))+match.charAt(match.length()-1);
	    	}
	    	if (match.equalsIgnoreCase("<!value>")) {
	    		replacement = context;
	    	} else if (match.equalsIgnoreCase("<!list>")) {
	    		ArrayList<String> names = new ArrayList<String>();
	    		for (String item : context.split(",")) {
	    			names.add("[light purple]"+item+"[]");
	    		}
	    		replacement += MiscGeneric.join(names, ", ");
	    	} else if (match.equalsIgnoreCase("<!playername>")) { 
	    		replacement += profile.name;
	    	} else if  (match.equalsIgnoreCase("<!xp>")) {
	    		Integer currentLevel = profile.jobLevels.get(profile.currentJob);
	    		replacement = Integer.toString(profile.jobXP.get(profile.currentJob)-profile.currentJob.xpToNextLevel(currentLevel-1));
	    		
	    	} else if  (match.equalsIgnoreCase("<!xp2level>")) {
	    		Integer currentLevel = profile.jobLevels.get(profile.currentJob);
	    		replacement = Integer.toString(currentLevel < profile.currentJob.maximumLevel ? profile.currentJob.xpToNextLevel(currentLevel) : profile.currentJob.xpToNextLevel(currentLevel-1));
	    		
	    	} else if  (match.equalsIgnoreCase("<!job>")) {
	    		replacement = localizedJob(context, profile);
	    		
	    	} else if (match.equalsIgnoreCase("<!joblevel>")) {
	    		Integer level = profile.jobLevels.get(Settings.jobs.get(context));
	    		if (level == null) {
	    			level = 0;
	    		}
	    		replacement = Integer.toString(level);
	    		
	    	} else if (match.equalsIgnoreCase("<!jobmaxlevel>")) {
	    		replacement = Integer.toString(Settings.jobs.get(context).maximumLevel);
	    		
	    	} else if (match.equalsIgnoreCase("<!cost>")) {
	    		replacement = Integer.toString(Settings.actives.get(context).cost);
	    		
	    	} else if (match.equalsIgnoreCase("<!buffed>")) {
	    		StructurePassive buff = Settings.passives.get(context);
	    		String localized = localize(context,"passives."+context+".adjective",profile);
	    		replacement = localized != null ? localized : 
	    			(buff.adjective != null ? buff.adjective : 
	    				Settings.localization.get(profile.locale).getString("messages.buffed-default") + " " + localizedPassive(context, profile));
	    		
	    	} else if (match.equalsIgnoreCase("<!buff>") || match.equalsIgnoreCase("<!passive>")) {
	    		replacement = localizedPassive(context, profile);
	    		
	    	} else if  (match.equalsIgnoreCase("<!active>")) {
	    		replacement = localizedActive(context,profile);
	    		
	    	} else if  (match.equalsIgnoreCase("<!charges>")) {
	    		matcher.appendReplacement(sb, profile.charges.toString());
	    		
	    	} else if  (match.equalsIgnoreCase("<!chargeprogress>")) {
	    		replacement = profile.chargeProgress.toString();
	    		
	    	} else if (match.startsWith("<#")) { 
	    		// TODO: update
	    		String term = match.substring(2,match.length()-1);
	    		term = term.endsWith("+") ? 
	    				MiscBukkit.parseSingularPlural(Settings.localization.get(profile.locale).getString("terminology."+term.substring(0,term.length()-1)), 2) : 
    					MiscBukkit.parseSingularPlural(Settings.localization.get(profile.locale).getString("terminology."+term), 1);
	    		replacement = term;
	    		
	    	} else {
	    		// TODO: update for descriptions for passives and the sort, maybe move parsing to separate function
	    		replacement = localize(match.substring(1,match.length()-1),match.substring(1,match.length()-1),profile);
	    	}
	    	if (!format.isEmpty()) {
	    		// TODO: use proper java string formatting
	    		try {
	    			double value = Double.parseDouble(replacement);
	    			boolean signed = format.endsWith("+");
	    			format = signed?format.substring(0, format.length()-1):format;
	    			replacement = "[light purple]"+ (signed?(value >= 0 ? "+":""):"");
		    		if (format.startsWith("percent")) {
			    		if (Math.abs(value) < 0.01) {
			    			replacement += "0."+Integer.toString((int)(value*1000)); //borked for negatives, switch to proper formatting already!
			    		} else {
			    			replacement += Integer.toString((int)(value*100));
			    		}
			    		replacement += "%";
		    		} else if (format.startsWith("hearts")) {
		    			Integer hearts = new Integer((int)(value/2));
		    			//replacement += (hearts == 0 && value%2 != 0 ? "" : hearts.toString()) + (value%2 != 0 ? (hearts != 0 ? " " : (value < 0 && hearts == 0?"-":""))+"1/2" : "");
		    			replacement += hearts.toString()+(value%2 != 0?".5":"");
		    			replacement += " "+parseLine(profile,"<#heart"+(value != 2 && value != -2 ? "+":"")+">",""); //TODO: maybe localize or remove it
		    		} else {
		    			replacement += value+format;
		    		}
		    		replacement += "[]";
	    		} catch (NumberFormatException ex) {
	    			// fiddle thumbs
	    		}
	    	}
	    	matcher.appendReplacement(sb, replacement);
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
    	pattern = Pattern.compile("\\[[\\w ]*\\]");
	    matcher = pattern.matcher(sb.toString());
	    sb = new StringBuffer();
	    while (matcher.find()) {
	    	if (colorMap.containsKey(matcher.group())) {
	    		matcher.appendReplacement(sb, colorMap.get(matcher.group()));
	    	}
	    }
	    matcher.appendTail(sb);
	    return sb.toString();
	}
	
	public static String localize(String string, String path) {
		return localize(string, path, null);
	}
	
	public static String localize(String string, String path, ProfilePlayer profile) {
		String locale = profile != null ? profile.locale : Settings.defaultLocale;
		String localized = Settings.localization.get(locale).getString(path);
		return localized != null?localized:(string!=null?string:"");
	}
	
	public String localizedPassive(String signature) {
		return localizedPassive(signature, null);
	}
	
	public static String localizedPassive(String signature, ProfilePlayer profile) {
		return localize(Settings.actives.get(signature).name,"passives."+signature+".name",profile);
	}
	
	public String localizedActive(String signature) {
		return localizedActive(signature, null);
	}
	
	public static String localizedActive(String signature, ProfilePlayer profile) {
		return localize(Settings.actives.get(signature).name,"actives."+signature+".name",profile);
	}
	
	public String localizedJob(String signature) {
		return localizedJob(signature, null);
	}
	
	public static String localizedJob(String signature, ProfilePlayer profile) {
		return localize(Settings.jobs.get(signature).name,"jobs."+signature+".name",profile);
	}
}
