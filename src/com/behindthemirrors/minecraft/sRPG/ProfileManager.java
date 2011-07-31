package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class ProfileManager {
	
	public HashMap<LivingEntity,ProfileNPC> profiles = new HashMap<LivingEntity,ProfileNPC>();
	
	public void clear() {
		profiles.clear();
	}
	// create new PlayerData and load data from database
	public void add(Player player) {
		ProfilePlayer profile = new ProfilePlayer();
		profiles.put(player,profile);
		profile.entity = (LivingEntity)player; 
		profile.player = player;
		profile.name = player.getName();
		profile.locale = Settings.defaultLocale;
		// mySQL
		if (SRPG.database.mySQLenabled) {
			profile.id = SRPG.database.getSingleIntValue("users", "user_id", "user", player.getName());
			if (profile.id == 0) {
				enterIntoDatabase(player);
				SRPG.output("created player data");
			}
			load(player);
		// no other option available right now, may add SQLite or flatfile later on
		} else {
			SRPG.output("player data not loaded due to mySQL being disabled");
		}
	}
	
	public void add(LivingEntity entity) {
		ProfileNPC profile = new ProfileNPC();
		profile.entity = entity;
		profiles.put(entity, profile);
	}
	
	public void remove(Player player) {
		save(player);
		profiles.remove(player);
	}
	
	public void remove(LivingEntity entity) {
		profiles.remove(entity);
	}
	
	public ProfileNPC get(LivingEntity entity) {
		if (!profiles.containsKey(entity)) {
			add(entity);
		}
		return profiles.get(entity);
	}
	
	public ProfilePlayer get(Player player) {
		return (ProfilePlayer)profiles.get(player);
	}
	
	public ProfilePlayer get(String name) {
		for (ProfileNPC profile : profiles.values()) {
			if (profile instanceof ProfilePlayer && ((ProfilePlayer)profile).name.equals(name)) {
				return (ProfilePlayer)profile;
			}
		}
		return null;
	}
	
	public boolean has(ProfilePlayer profile) {
		return profiles.containsKey(profile.player);
	}
	
	public boolean has(String name) {
		return has(SRPG.plugin.getServer().getPlayer(name));
	}
	
	public boolean has(Player player) {
		return profiles.containsKey(player);
	}

	// load player data from database
	public void load(Player player) {
		ProfilePlayer profile = (ProfilePlayer)profiles.get(player);
		// read current class
		String jobname = SRPG.database.getSingleStringValue("users", "currentjob", "user_id", profile.id);
		if (!Settings.jobs.containsKey(jobname)) {
			jobname = Settings.initialJobs.get(SRPG.generator.nextInt(Settings.initialJobs.size())).signature;
		}
		profile.currentJob = Settings.jobs.get(jobname);
		// read locale
		profile.locale = SRPG.database.getSingleStringValue("users", "locale", "user_id", profile.id);
		// change to default locale if the set locale is not available anymore
		if (!Settings.localization.containsKey(profile.locale)) {
			profile.locale = Settings.defaultLocale;
			save(player,"locale");
		}
		
		// read hp
		profile.hp = SRPG.database.getSingleIntValue("users", "hp", "user_id", profile.id);
		profile.hp_max = 40;
		//Integer normalized = data.hp*20 / data.hp_max;
		//player.setHealth(normalized == 0 && data.hp != 0 ? 1 : normalized);

		// read job xp
		ArrayList<String> jobs = new ArrayList<String>();
		jobs.addAll(Settings.jobs.keySet());
		ArrayList<Integer> xp = SRPG.database.getSingleIntRow("jobxp", jobs, "user_id", profile.id);
		SRPG.output("loaded player xp: "+xp.toString());
		profile.jobXP = new HashMap<StructureJob, Integer>();
		profile.jobLevels = new HashMap<StructureJob, Integer>();
		for (int i=0; i < jobs.size();i++) {
			profile.jobXP.put(Settings.jobs.get(jobs.get(i)), xp.get(i));
			if (xp.get(i) > 0) {
				profile.checkLevelUp(Settings.jobs.get(jobs.get(i)));
			}
		}
		profile.charges = SRPG.database.getSingleIntValue("users", "charges", "user_id", profile.id);
		profile.chargeProgress = SRPG.database.getSingleIntValue("users", "chargeprogress", "user_id", profile.id);
		profile.changeJob(profile.currentJob);
		profile.suppressMessages = false;
	}
	
	// create database entry for player
	public void enterIntoDatabase(Player player) { 
		String name = player.getName();
		ProfilePlayer profile = (ProfilePlayer)profiles.get(player);
		SRPG.output("trying to enter "+name+" into the database");
		HashMap<String,String> map = new HashMap<String, String>();
		map.put("user", name);
		map.put("hp", ""+player.getHealth());
		map.put("locale", profile.locale);
		SRPG.database.insertStringValues("users", map);
		
		SRPG.output("users table written, proceeding to fetch id");
		profile.id = SRPG.database.getSingleIntValue("users", "user_id", "user", name);
		SRPG.database.insertSingleIntValue("jobxp", "user_id", profile.id);
	}
	
	// save all data
	public void save(Player player) {
		save(player,"");
	}
	
	public void save(ProfilePlayer profile) {
		save(profile,"");
	}
	
	public void save(Player player, String partial) {
		save((ProfilePlayer)profiles.get(player),partial);
	}
	
	// save specific part of data to database
	public void save(ProfilePlayer profile, String partial) {
		// write xp
		if (partial.isEmpty() || partial.equalsIgnoreCase("xp")) {
			SRPG.database.setSingleIntValue("jobxp", profile.currentJob.signature, profile.jobXP.get(profile.currentJob), "user_id", profile.id);
		}
		// write job
		if (partial.isEmpty() || partial.equalsIgnoreCase("job")) {
			SRPG.database.setSingleStringValue("users", "currentjob", profile.currentJob.signature, "user_id", profile.id);
		}
		// write hp
		if (partial.isEmpty() || partial.equalsIgnoreCase("hp")) {
			SRPG.database.setSingleIntValue("users", "hp", profile.hp, "user_id", profile.id);
		}
		// write locale
		if (partial.isEmpty() || partial.equalsIgnoreCase("locale")) {
			SRPG.database.setSingleStringValue("users", "locale", profile.locale, "user_id", profile.id);
		}
		// write charge data
		if (partial.isEmpty() || partial.equalsIgnoreCase("chargedata")) {
			SRPG.database.setSingleIntValue("users", "charges", profile.charges, "user_id", profile.id);
			SRPG.database.setSingleIntValue("users", "chargeprogress", profile.chargeProgress, "user_id", profile.id);
		}
	}
	
}
