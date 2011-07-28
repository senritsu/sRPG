package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.util.config.ConfigurationNode;

public class StructureJob {
	
	String name;
	String description;
	String details;
	HashMap<String,Integer> prerequisites;
	Integer tier;
	HashMap<String,ConfigurationNode> statOverrides;
	HashMap<String,ConfigurationNode> bonuses;
	HashMap<Integer,ArrayList<StructurePassive>> passives;
	HashMap<Integer,ArrayList<StructureActive>> actives;
	
	public StructureJob (ConfigurationNode root) {
		name = root.getString("name");
		
		description = root.getString("description");
		details = root.getString("details");
		
		prerequisites = new HashMap<String, Integer>();
		if (root.getKeys("prerequisites") != null) {
			for (String job : root.getKeys("prerequisites")) {
				prerequisites.put(job, root.getInt("prerequisites."+job, 1));
			}
		}
		
		tier = root.getInt("tier",1);
		
		statOverrides = new HashMap<String, ConfigurationNode>();
		if (root.getKeys("base-stats") != null) {
			for (String stat : root.getKeys("base-stats")) {
				statOverrides.put(stat, root.getNode("base-stats."+stat));
			}
		}
		
		bonuses = new HashMap<String, ConfigurationNode>();
		if (root.getKeys("bonuses") != null) {
			for (String bonus : root.getKeys("bonuses")) {
				bonuses.put(bonus, root.getNode("bonuses."+bonus));
			}
		}
		
		passives = new HashMap<Integer, ArrayList<StructurePassive>>();
		if (root.getKeys("passives") != null) {
			for (String levelString : root.getKeys("passives")) {
				Integer level = Integer.parseInt(levelString.substring(levelString.indexOf(" ")));
				passives.put(level, new ArrayList<StructurePassive>());
				for (String passive : root.getKeys("passives."+levelString)) {
					passives.get(level).add(new StructurePassive(root.getNode("passives."+levelString+"."+passive)));
				}
			}
		}
		
		actives = new HashMap<Integer, ArrayList<StructureActive>>();
		if (root.getKeys("actives") != null) {
			for (String levelString : root.getKeys("actives")) {
				Integer level = Integer.parseInt(levelString.substring(levelString.indexOf(" ")));
				actives.put(level, new ArrayList<StructureActive>());
				for (String active : root.getKeys("actives."+levelString)) {
					actives.get(level).add(new StructureActive(root.getNode("actives."+levelString+"."+active)));
				}
			}
		}
	}
	
}
