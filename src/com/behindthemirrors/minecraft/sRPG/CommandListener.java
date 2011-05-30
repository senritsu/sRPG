package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListener implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			if (command.getName().equals("srpg")) {
				if (args.length < 1) {
					MessageParser.sendMessage(player, "welcome");
					return true;
				// change locale
				} else if (args[0].equalsIgnoreCase("locale")) {
					if (args.length > 1 && Settings.localization.containsKey(args[1])) {
						SRPG.playerDataManager.get(player).locale = args[1];
						SRPG.playerDataManager.save(player, "locale");
						MessageParser.sendMessage(player, "locale-changed");
						return true;
					}
				// display xp,skillpoints,milestones (if enabled)
				} else if (args[0].equalsIgnoreCase("status")) {
					MessageParser.sendMessage(player, "status-header");
					PlayerData data = SRPG.playerDataManager.get(player);
					// display xp
					if (SRPG.permissionHandler.has(player,"srpg.xp")) {
						MessageParser.sendMessage(player, "xp");
					}
					// display individual skills
					for (String skillname : Settings.SKILLS) {
						if (!SRPG.permissionHandler.has(player,"srpg.skills."+skillname) || skillname.equals("focus")) {
							continue;
						}
						MessageParser.sendMessage(player, "check-skillpoints",skillname);
					}
					// display focus
					if (SRPG.permissionHandler.has(player,"srpg.skills.focus") && data.focusAllowed) {
						MessageParser.sendMessage(player, "check-focus","focus");
					}
					// display unallocated skillpoints
					if (data.free > 0) {
						MessageParser.sendMessage(player, "free-skillpoints");
					}
					return true;
				// display available charges with the current tool
				} else if (args[0].equalsIgnoreCase("charges")) {
					MessageParser.chargeDisplay(player);
					return true;
				// get info about a skill or increase it
				} else if (Settings.SKILLS_ALIASES.get(SRPG.playerDataManager.get(player).locale).contains(args[0].toLowerCase())) {
					String skillname = Settings.SKILLS.get(Settings.SKILLS_ALIASES.get(SRPG.playerDataManager.get(player).locale).indexOf(args[0].toLowerCase()));
					// increasing/decreasing
					if (args.length > 1) {
						Integer amount = 1;
						if (args.length > 2) {
							try {
								amount = Integer.parseInt(args[2]);
							} catch (NumberFormatException e) {
							}
						}
						if (args[1].equals("+")) {
							for (int i=0;i<amount;i++) {
								SRPG.playerDataManager.get(player).addSkillpoint(skillname);
								SRPG.playerDataManager.save(player,"skillpoints");
							}
							return true;
						} else if (args[1].equals("-")) {
							for (int i=0;i<amount;i++) {
								SRPG.playerDataManager.get(player).addSkillpoint(skillname);
								SRPG.playerDataManager.save(player,"skillpoints");
							}
							return true;
						}
					// information
					} else {
						MessageParser.sendMessage(player, "skill-info.common-header", skillname);
						MessageParser.sendMessage(player, "skill-info."+skillname+".description", skillname);
						MessageParser.sendMessage(player, "skill-info.skill-effect-header", skillname);
						MessageParser.sendMessage(player, "skill-info."+skillname+".basic", skillname);
						MessageParser.sendMessage(player, "skill-info.passive-header", skillname);
						MessageParser.sendMessage(player, "skill-info."+skillname+".passive", skillname);
						MessageParser.sendMessage(player, "skill-info.active-header", skillname);
						MessageParser.sendMessage(player, "skill-info."+skillname+".active", skillname);
						return true;
					}
				// internal help (TODO: maybe eventually replaced with some help plugin)
				} else if (args[0].equalsIgnoreCase("help")) {
					//String topic = args[1];
					// TODO: distinguish between help command with arguments and without
					MessageParser.sendMessage(player, "help-general");
					return true;
				}
			}
		} else {
			// server console commands
			if (command.getName().equals("srpg")) {
				// toggle debug messages
				if (args.length >= 2 && args[0].equalsIgnoreCase("debug")) {
					// display debug messages from spawn listener
					if (args[1].equalsIgnoreCase("spawn")) {
						SRPG.spawnListener.debug = !SRPG.spawnListener.debug;
						SRPG.output("spawn debugging set to "+SRPG.spawnListener.debug);
						return true;
					}
					// display debug messages from combat listener 
					if (args[1].equalsIgnoreCase("combat")) {
						SRPG.damageListener.debug = !SRPG.damageListener.debug;
						SRPG.output("combat debugging set to "+SRPG.spawnListener.debug);
						return true;
					}
				// add xp to a player (handle with care, no removal atm)
				} else if (args.length >= 3 && args[0].equalsIgnoreCase("xp")) {
					PlayerData data = SRPG.playerDataManager.getByName(args[1]);
					if ( data != null) {
						Integer amount;
						try {
							amount = Integer.parseInt(args[2]);
						} catch (NumberFormatException e) {
							SRPG.output("Not a valid number");
							return true;
						}
						
						data.addXP(amount);
						// xp given by this command are not saved atm if the server is shut down and the player doesnt quit properly
						return true;
						
					} else {
						SRPG.output("No player by that name");
						return true;
					}
				}
			}
		}
		
		// reload settings (TODO: still needs proper testing, some settings might be not properly re-initialized)
		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				if (!(sender instanceof Player) || ((sender instanceof Player) && SRPG.permissionHandler.has((Player)sender, "srpg.reload"))) {
					SRPG.settings.load();
					SRPG.output("Reloaded configuration");
					return true;
				}
			}
		}
		return false;
	}
}
