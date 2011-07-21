package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;


public class PlayerData {
	
	// debug player stuff
	public static boolean debug = false;
	
	// TODO: maybe change to read directly from config
	static Integer xpToLevel;
	static Integer focusBase;
	static Integer focusIncrease;
	static ArrayList<Integer> skillCosts;
	static HashMap<String,Integer> abilityCosts;
	static Integer chargeMax;
	static Integer chargeTicks;
	static HashMap<Integer,String> milestoneRequirements;
	
	Integer id = 0;
	Player player;
	String name;
	Integer xp;
	Integer free;
	Integer spent;
	boolean focusAllowed;
	HashMap<String,Integer> skillpoints;
	//HashMap<String,ArrayList<String>> milestones;
	HashMap<String,Integer> charges;
	HashMap<String,Integer> chargeProgress;
	
	HashMap<String, Integer> effectCounters = new HashMap<String, Integer>();
	
	private String abilityReadied = "";
	private long abilityReadiedTimeStamp;
	
	public long sneakTimeStamp;
	
	String locale;
	
	public Integer getSkill(String skillname) {
		return skillpoints.get(skillname);
	}
	
	public Integer getLevel() {
		return xp/xpToLevel;
	}
	
	// get list of reached milestones for that skill
	public ArrayList<String> getMilestones(String skillname) {
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0; i <= skillpoints.get(skillname); i++) {
			if (milestoneRequirements.containsKey(i)) {
				list.add(milestoneRequirements.get(i));
			}
		}
		return list;
	}
	
	public void addXP(Integer amount) {
		// TODO: maybe move the permission check before the actual xp calculations
		if (!SRPG.permissionHandler.has(player,"srpg.xp")) {
			return;
		}
		xp += amount;
		// debug message
		if (debug) {
			SRPG.output("adding "+amount.toString()+" xp to player "+name);
		}
		if (xp >= (xpToLevel * (free + spent + 1))) {
			free += xp / xpToLevel - free - spent;
			MessageParser.sendMessage(player, "levelup");
		}
	}
	public void addSkillpoint(String skillname) {
		// check for permissions
		if (!SRPG.permissionHandler.has(player,"srpg.skills."+skillname)) {
			return;
		}
		// prevent focus from being raised until one other skill is on "master"
		if (skillname.equals("focus") && !focusAllowed) {
			return;
		}
		Integer current = skillpoints.get(skillname);
		int cost = 0;
		// get skill increase cost
		if (skillname.equals("focus")) {
			cost = focusBase + getSkill(skillname) * focusIncrease;
		} else {
			cost = skillCosts.get(current);
		}
		// check for free points
		if (!skillname.equals("focus") && current >= skillCosts.size()) {
			MessageParser.sendMessage(player, "skill-already-at-max");
		} else if (free < cost) {
			MessageParser.sendMessage(player, "skill-no-free-points", Integer.toString(cost));
		// increase skill
		} else {
			free -= skillCosts.get(current);
			spent += skillCosts.get(current);
			skillpoints.put(skillname, current+1);
			if (current+1 >= skillCosts.size()) {
				focusAllowed = true;
			}
			MessageParser.sendMessage(player, "skill-increased",skillname);
		}
	}
	
	public void removeSkillpoint(String skillname) {
		if (!SRPG.permissionHandler.has(player,"srpg.skills."+skillname)) {
			return;
		}
		Integer current = skillpoints.get(skillname);
		if (current <= 0) {
			MessageParser.sendMessage(player, "skill-already-at-zero");
		// TODO: rewrite this to allow people with the permission srpg.reskill to remove skillpoints anyways (admin function)
		} else if (!Settings.config.getBoolean("settings.features.remove-skillpoints", false)) {
			MessageParser.sendMessage(player, "skill-no-removal");
		} else {
			free += skillCosts.get(current-1);
			spent -= skillCosts.get(current-1);
			skillpoints.put(skillname, current-1);
			MessageParser.sendMessage(player, "skill-decreased",skillname);
			// check if focus conditions are no longer true and remove all focus if true
			if (!checkMastery()) {
				focusAllowed = false;
				int cumulativeCost = 0;
				for (int i = 0;i < getSkill("focus"); i++) {
					cumulativeCost += focusBase + i*focusIncrease;
				}
				free += cumulativeCost;
				spent -= cumulativeCost;
				skillpoints.put("focus", 0);
			}
		}
	}
	
	public boolean checkMastery() {
		boolean mastery = false;
		for (String skill : Settings.SKILLS) {
			if (!skill.equals("focus") && getSkill(skill) >= skillCosts.size()) {
				mastery = true;
				break;
			}
		}
		return mastery;
	}

	public void addChargeTick(String skillname) {
		Integer current = charges.get(skillname);
		Integer progress = chargeProgress.get(skillname);
		if (!(current >= chargeMax)) {
			progress += 1;
			if (progress >= chargeTicks) {
				progress -= chargeTicks;
				charges.put(skillname, current+1);
				MessageParser.sendMessage(player, "charge-acquired");
			}
			chargeProgress.put(skillname, progress);
		}
	}
	
	public void readyAbility(Material material) {
		String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(material);
		Integer cost = abilityCosts.get(toolName);
		if (cost != null && charges.get(Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(material)) >= cost) {
			abilityReadied = toolName;
			abilityReadiedTimeStamp = System.currentTimeMillis();
		} else {
			abilityReadied = "";
		}
	}
	
	public void activateAbility(Material material) {
		String toolName = Settings.TOOL_MATERIAL_TO_STRING.get(material);
		if (abilityReadied.equals(toolName) && (System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500) {
			String tool = Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(material);
			if (ActiveAbility.activate(player, toolName)) {
				charges.put(tool, charges.get(tool) - abilityCosts.get(toolName));
			}
		}
		abilityReadied = "";
	}
	
}
