package com.behindthemirrors.minecraft.sRPG;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
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
						SRPG.profileManager.get(player).locale = args[1];
						SRPG.profileManager.save(player, "locale");
						MessageParser.sendMessage(player, "locale-changed");
						return true;
					}
				// display xp,skillpoints,milestones (if enabled)
				} else if (args[0].equalsIgnoreCase("status")) {
					MessageParser.sendMessage(player, "status-header");
					ProfilePlayer data = SRPG.profileManager.get(player);
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
				// TODO find NPE
				} else if (Settings.SKILLS_ALIASES.get(SRPG.profileManager.get(player).locale).contains(args[0].toLowerCase())) {
					String skillname = Settings.SKILLS.get(Settings.SKILLS_ALIASES.get(SRPG.profileManager.get(player).locale).indexOf(args[0].toLowerCase()));
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
								SRPG.profileManager.get(player).addSkillpoint(skillname);
								SRPG.profileManager.save(player,"skillpoints");
							}
							return true;
						} else if (args[1].equals("-")) {
							for (int i=0;i<amount;i++) {
								SRPG.profileManager.get(player).removeSkillpoint(skillname);
								SRPG.profileManager.save(player,"skillpoints");
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
						SpawnEventListener.debug = !SpawnEventListener.debug;
						SRPG.output("spawn debugging set to "+SpawnEventListener.debug);
						return true;
					}
					// display debug messages from combat listener 
					if (args[1].equalsIgnoreCase("combat")) {
						DamageEventListener.debug = !DamageEventListener.debug;
						SRPG.output("combat debugging set to "+DamageEventListener.debug);
						return true;
					}
					if (args[1].equalsIgnoreCase("effects")) {
						TimedEffectManager.debug = !TimedEffectManager.debug;
						TimedEffectResolver.debug = !TimedEffectResolver.debug;
						SRPG.output("effect debugging set to "+TimedEffectResolver.debug);
						return true;
					}
					// display debug messages from combat listener 
					if (args[1].equalsIgnoreCase("player")) {
						ProfilePlayer.debug = !ProfilePlayer.debug;
						SRPG.output("player debugging set to "+ProfilePlayer.debug);
						return true;
					}
					// remove item stacks if something was incorrectly dropped
					if (args[1].equalsIgnoreCase("removeitems")) {
						for (Entity entity : SRPG.plugin.getServer().getWorlds().get(0).getEntities()) {
							if (entity instanceof Item) {
								entity.remove();
							}
						}
						SRPG.output("removed all items");
						return true;
					}
					if (args[1].equalsIgnoreCase("spawninvincible")) {
						SpawnEventListener.spawnInvincible = !SpawnEventListener.spawnInvincible;
						SRPG.output("spawn invincibility set to "+(new Boolean(SpawnEventListener.spawnInvincible).toString()));
						return true;
					}
				// add xp to a player (handle with care, no removal atm)
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("list")) {
					// display hp values
					if (args[1].equalsIgnoreCase("tooldamage")) {
						Iterator<Map.Entry<String,Integer>> pairs = DamageEventListener.damageTableTools.entrySet().iterator();
						while (pairs.hasNext()) {
							Map.Entry<String,Integer>pair = pairs.next();
							SRPG.output(pair.getKey()+": "+pair.getValue());
						}
						return true;
					}
					
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("charge")) {
					ProfilePlayer profile = SRPG.profileManager.getByName(args[1]);
					if ( profile != null) {
						Iterator<Map.Entry<String,Integer>> iterator = profile.charges.entrySet().iterator();
						while (iterator.hasNext()) {
							Map.Entry<String,Integer> entry = iterator.next();
							entry.setValue(11);
						}
						SRPG.profileManager.save(profile, "chargedata");
						SRPG.output("gave player "+args[1]+" maximum charges");
						return true;
					}
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("enrage")) {
						ProfilePlayer profile = SRPG.profileManager.getByName(args[1]);
						if ( profile != null) {
							profile.addEffect("rage", 10);
							SRPG.output("enraged player "+args[1]);
							return true;
						}
				} else if (args.length >= 3 && args[0].equalsIgnoreCase("poison")) {
					ProfilePlayer profile = SRPG.profileManager.getByName(args[1]);
					if ( profile != null) {
						profile.addEffect("poison"+args[2], 5);
						SRPG.output("poisoned player "+args[1]);
						return true;
					}
				} else if (args.length >= 3 && args[0].equalsIgnoreCase("xp")) {
					ProfilePlayer data = SRPG.profileManager.getByName(args[1]);
					if ( data != null) {
						Integer amount;
						try {
							amount = Integer.parseInt(args[2]);
						} catch (NumberFormatException e) {
							SRPG.output("Not a valid number");
							return true;
						}
						
						data.addXP(amount);
						SRPG.profileManager.save(data, "xp");
						SRPG.output("gave "+amount.toString()+" xp to player "+args[1]);
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
