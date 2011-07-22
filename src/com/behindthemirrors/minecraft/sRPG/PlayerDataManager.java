package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerDataManager {
	
	public HashMap<Player,PlayerData> players = new HashMap<Player,PlayerData>();
	
	public void clear() {
		players.clear();
	}
	// create new PlayerData and load data from database
	public void add(Player player) {
		PlayerData data = new PlayerData();
		players.put(player,data);
		data.player = player;
		data.name = player.getName();
		data.current_class = "adventurer";
		data.locale = Settings.defaultLocale;
		// mySQL
		if (SRPG.database.mySQLenabled) {
			data.id = SRPG.database.GetInt("SELECT user_id FROM " + SRPG.database.dbTablePrefix + "users WHERE user = \"" + player.getName() + "\"");
			if (data.id == 0) {
				enterIntoDatabase(player);
				SRPG.output("created player data");
			}
			load(player);
		// no other option available right now, may add SQLite or flatfile later on
		} else {
			SRPG.output("player data not loaded due to mySQL being disabled");
		}
	}
	
	public void remove(Player player) {
		save(player);
		players.remove(player);
	}
	
	public PlayerData get(Player player) {
		return players.get(player);
	}
	
	public PlayerData getByName(String name) {
		for (PlayerData data : players.values()) {
			if (data.name.equals(name)) {
				return data;
			}
		}
		return null;
	}

	// load player data from database
	public void load(Player player) {
		PlayerData data = players.get(player);
		String suffix = " WHERE user_id = '" + data.id + "'";
		// read current class
		data.locale = new String(SRPG.database.Read("SELECT class FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		// read locale
		data.locale = new String(SRPG.database.Read("SELECT locale FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		// change to default locale if the set locale is not available anymore
		if (!Settings.localization.containsKey(data.locale)) {
			data.locale = Settings.defaultLocale;
			save(player,"locale");
		}
		// read xp
		data.xp = new Integer(SRPG.database.Read("SELECT xp FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		
		// read hp
		data.hp = new Integer(SRPG.database.Read("SELECT hp FROM " + SRPG.database.dbTablePrefix + "users" + suffix).get(1).get(0));
		data.hp_max = 40;
		//Integer normalized = data.hp*20 / data.hp_max;
		//player.setHealth(normalized == 0 && data.hp != 0 ? 1 : normalized);

		// read skill points
		data.free = data.xp/PlayerData.xpToLevel;
		data.spent = 0;
		String prefix = "SELECT " + Utility.join(Settings.SKILLS,",") + " FROM " + SRPG.database.dbTablePrefix;
		ArrayList<String> rs = SRPG.database.Read(prefix + "skillpoints" + suffix).get(1);
		data.skillpoints = new HashMap<String, Integer>();
		for (int i = 0; i < Settings.SKILLS.size(); i++) {
			Integer points = new Integer(rs.get(i));
			int cumulativeCost = 0;
			for (int j = 0;j < points; j++) {
				cumulativeCost += PlayerData.skillCosts.get(j);
			}
			data.free -= cumulativeCost;
			data.spent += cumulativeCost;
			data.skillpoints.put(Settings.SKILLS.get(i), points);
		}
		data.focusAllowed = data.checkMastery();
		// read charge data
		ArrayList<String> fields = new ArrayList<String>();
		for (String tool : Settings.TOOLS) {
			fields.add(tool+"_charges");
			fields.add(tool+"_chargeprogress");
		}
		prefix = "SELECT " + Utility.join(fields,",") + " FROM " + SRPG.database.dbTablePrefix;
		rs = SRPG.database.Read(prefix + "chargedata" + suffix).get(1);
		data.charges = new HashMap<String, Integer>();
		data.chargeProgress = new HashMap<String, Integer>();
		for (int i = 0; i < Settings.TOOLS.size(); i++) {
			data.charges.put(Settings.TOOLS.get(i), new Integer(rs.get(2*i)));
			data.chargeProgress.put(Settings.TOOLS.get(i), new Integer(rs.get(2*i+1)));
		}
	}
	
	// create database entry for player
	public void enterIntoDatabase(Player player) { 
		String name = player.getName();
		PlayerData data = players.get(player);
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
	
	// save specific part of data to database
	public void save(Player player, String partial) {
		PlayerData data = players.get(player);
		String prefix = "UPDATE " + SRPG.database.dbTablePrefix;
		String suffix = " WHERE user_id = '" + data.id + "'";
		
		// write hp
		if (partial.isEmpty() || partial.equalsIgnoreCase("hp")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET hp = '" + data.hp + "'" + suffix);
		}
		// write class
		if (partial.isEmpty() || partial.equalsIgnoreCase("class")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET class = \"" + data.current_class + "\"" + suffix);
		}
		// write locale
		if (partial.isEmpty() || partial.equalsIgnoreCase("locale")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET locale = \"" + data.locale + "\"" + suffix);
		}
		// write xp
		if (partial.isEmpty() || partial.equalsIgnoreCase("xp")) {
			SRPG.database.Write("UPDATE " + SRPG.database.dbTablePrefix + "users SET xp = '" + data.xp + "'" + suffix);
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
				update += Settings.SKILLS.get(i) + " = '" + data.skillpoints.get(Settings.SKILLS.get(i)) + "'";
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
				update += type + "_charges = '" + data.charges.get(type) + "'";
				update += ","+type + "_chargeprogress = '" + data.chargeProgress.get(type) + "'";
			}
			SRPG.database.Write(prefix + "chargedata" + update + suffix);
		}
	}
	
}
