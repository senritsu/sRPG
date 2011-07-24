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
		profile.current_class = "adventurer";
		profile.locale = Settings.defaultLocale;
		// mySQL
		if (SRPG.database.mySQLenabled) {
			profile.id = SRPG.database.GetInt("SELECT user_id FROM " + SRPG.database.dbTablePrefix + "users WHERE user = \"" + player.getName() + "\"");
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
	
	public ProfilePlayer getByName(String name) {
		for (ProfileNPC profile : profiles.values()) {
			if (profile instanceof ProfilePlayer && ((ProfilePlayer)profile).name.equals(name)) {
				return (ProfilePlayer)profile;
			}
		}
		return null;
	}

	// load player data from database
	public void load(Player player) {
		ProfilePlayer profile = (ProfilePlayer)profiles.get(player);
		String suffix = " WHERE user_id = '" + profile.id + "'";
		// read current class
		profile.locale = new String(SRPG.database.Read("SELECT class FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		// read locale
		profile.locale = new String(SRPG.database.Read("SELECT locale FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		// change to default locale if the set locale is not available anymore
		if (!Settings.localization.containsKey(profile.locale)) {
			profile.locale = Settings.defaultLocale;
			save(player,"locale");
		}
		// read xp
		profile.xp = new Integer(SRPG.database.Read("SELECT xp FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		
		// read hp
		profile.hp = new Integer(SRPG.database.Read("SELECT hp FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		profile.hp_max = 40;
		//Integer normalized = data.hp*20 / data.hp_max;
		//player.setHealth(normalized == 0 && data.hp != 0 ? 1 : normalized);

		// read skill points
		profile.free = profile.xp/ProfilePlayer.xpToLevel;
		profile.spent = 0;
		String prefix = "SELECT " + Utility.join(Settings.SKILLS,",") + " FROM " + SRPG.database.dbTablePrefix;
		ArrayList<String> rs = SRPG.database.Read(prefix + "skillpoints" + suffix).get(1);
		profile.skillpoints = new HashMap<String, Integer>();
		for (int i = 0; i < Settings.SKILLS.size(); i++) {
			Integer points = new Integer(rs.get(i));
			int cumulativeCost = 0;
			for (int j = 0;j < points; j++) {
				cumulativeCost += ProfilePlayer.skillCosts.get(j);
			}
			profile.free -= cumulativeCost;
			profile.spent += cumulativeCost;
			profile.skillpoints.put(Settings.SKILLS.get(i), points);
		}
		profile.focusAllowed = profile.checkMastery();
		// read charge data
		ArrayList<String> fields = new ArrayList<String>();
		for (String tool : Settings.TOOLS) {
			fields.add(tool+"_charges");
			fields.add(tool+"_chargeprogress");
		}
		prefix = "SELECT " + Utility.join(fields,",") + " FROM " + SRPG.database.dbTablePrefix;
		rs = SRPG.database.Read(prefix + "chargedata" + suffix).get(1);
		profile.charges = new HashMap<String, Integer>();
		profile.chargeProgress = new HashMap<String, Integer>();
		for (int i = 0; i < Settings.TOOLS.size(); i++) {
			profile.charges.put(Settings.TOOLS.get(i), new Integer(rs.get(2*i)));
			profile.chargeProgress.put(Settings.TOOLS.get(i), new Integer(rs.get(2*i+1)));
		}
	}
	
	// create database entry for player
	public void enterIntoDatabase(Player player) { 
		String name = player.getName();
		ProfilePlayer data = (ProfilePlayer)profiles.get(player);
		String prefix = "INSERT INTO " + SRPG.database.dbTablePrefix;
		SRPG.output("trying to enter "+name+" into the database");
		SRPG.database.Write(prefix + "users (user,hp,class,locale) VALUES (\"" + name + "\",'" + player.getHealth() + "',\"" + data.current_class + "\",\"" + data.locale + "\")");
		SRPG.output("users table written, proceeding to fetch id");
		data.id = SRPG.database.GetInt("SELECT user_id FROM " + SRPG.database.dbTablePrefix + "users WHERE user = \"" + name + "\"");
		SRPG.output("id retrieved, proceeding to enter skillpoints and chargedata");
		for (String table : new String[] {"skillpoints","chargedata"}) {
			SRPG.database.Write(prefix + table + " (user_id) VALUES ('" + data.id + "')");
		}
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
		String prefix = "UPDATE " + SRPG.database.dbTablePrefix;
		String suffix = " WHERE user_id = '" + profile.id + "'";
		
		// write hp
		if (partial.isEmpty() || partial.equalsIgnoreCase("hp")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET hp = '" + profile.hp + "'" + suffix);
		}
		// write class
		if (partial.isEmpty() || partial.equalsIgnoreCase("class")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET class = \"" + profile.current_class + "\"" + suffix);
		}
		// write locale
		if (partial.isEmpty() || partial.equalsIgnoreCase("locale")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET locale = \"" + profile.locale + "\"" + suffix);
		}
		// write xp
		if (partial.isEmpty() || partial.equalsIgnoreCase("xp")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET xp = '" + profile.xp + "'" + suffix);
		}
		// write skill points
		String update = " SET ";
		boolean first = true;
		if (partial.isEmpty() || partial.equalsIgnoreCase("skillpoints")) {
			for (int i = 0; i < Settings.SKILLS.size(); i++) {
				if (!first) {
					update += ",";
				} else {
					first = false;
				}
				update += Settings.SKILLS.get(i) + " = '" + profile.skillpoints.get(Settings.SKILLS.get(i)) + "'";
			}
		SRPG.database.Write(prefix + "skillpoints" + update + suffix);
		}
		// write charge data
		if (partial.isEmpty() || partial.equalsIgnoreCase("chargedata")) {
			update = " SET ";
			first = true;
			for (int i = 0; i < Settings.TOOLS.size(); i++) {
				String type = Settings.TOOLS.get(i);
				if (!first) {
					update += ",";
				} else {
					first = false;
				}
				update += type + "_charges = '" + profile.charges.get(type) + "'";
				update += ","+type + "_chargeprogress = '" + profile.chargeProgress.get(type) + "'";
			}
			SRPG.database.Write(prefix + "chargedata" + update + suffix);
		}
	}
	
}
