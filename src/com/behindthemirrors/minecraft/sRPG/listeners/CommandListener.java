package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.MessageParser;
import com.behindthemirrors.minecraft.sRPG.ResolverPassive;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.TimedEffectManager;
import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureJob;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class CommandListener implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			ProfilePlayer profile = SRPG.profileManager.get(player);
			if (command.getName().equals("srpg")) {
				if (args.length < 1) {
					MessageParser.sendMessage(player, "welcome");
					return true;
				// change locale
				} else if (args[0].equalsIgnoreCase("locale")) {
					if (args.length > 1 && Settings.localization.containsKey(args[1])) {
						profile.locale = args[1];
						SRPG.profileManager.save(profile, "locale");
						MessageParser.sendMessage(player, "locale-changed");
						return true;
					}
				// display xp,skillpoints,milestones (if enabled)
				} else if (args[0].equalsIgnoreCase("status")) {
					MessageParser.sendMessage(player, "status-header");
					// display xp
					if (player.hasPermission("srpg.xp")) {
						MessageParser.sendMessage(player, "xp");
					}
					// display individual skills
					for (String skillname : Settings.SKILLS) {
						if (!player.hasPermission("srpg.skills."+skillname) || skillname.equals("focus")) {
							continue;
						}
						MessageParser.sendMessage(player, "check-skillpoints",skillname);
					}
					return true;
				// display available charges with the current tool
				} else if (args[0].equalsIgnoreCase("charges")) {
					MessageParser.chargeDisplay(player, false);
					return true;
				// get info about a skill or increase it
				// TODO find NPE
				} else if (args.length >= 3 && (args[0]+" "+args[1]).equalsIgnoreCase("change to")) {
					// TODO: accommodate for names with spaces
					String name = Settings.JOB_ALIASES.get(profile.locale).get(args[2]);
					if (name == null) {
						name = Settings.jobs.containsKey(args[2].toLowerCase()) ? Settings.jobs.get(args[2].toLowerCase()).signature : null;
					}
					if (name != null) {
						if (player.hasPermission("srpg.jobs") || player.hasPermission("srpg.jobs."+name)) {
							StructureJob job = Settings.jobs.get(name);
							if (job == profile.currentJob) {
								MessageParser.sendMessage(player,"job-already-selected",job.signature);
							} else if (job.prerequisitesMet(profile)) {
								SRPG.profileManager.get(player).changeJob(job);
								MessageParser.sendMessage(player,"job-changed",job.signature);
							} else {
								MessageParser.sendMessage(player,"job-prerequisite-missing",job.signature);
//								for (Map.Entry<StructureJob, Integer> entry : job.prerequisites.entrySet()) {
//									MessageParser.sendMessage(player,"job-prerequisite-missing",entry.getKey().signature+","+entry.getValue());
//								}
							}
						} else {
							MessageParser.sendMessage(player,"job-no-permissions");
						}
					} else {
						MessageParser.sendMessage(player,"job-not-available");
					}
					return true;
				} else if (args[0].equalsIgnoreCase("info")) {
					String name = null;
					if (args.length >= 2) {
						name = Settings.JOB_ALIASES.get(profile.locale).get(args[2]);
						if (name == null) {
							name = Settings.jobs.containsKey(args[2].toLowerCase()) ? Settings.jobs.get(args[2].toLowerCase()).name : null;
						}
					} else {
						name = profile.currentJob.signature;
					}
					if (name != null) {
						MessageParser.sendMessage(player, "job-info",name);
					} else {
						MessageParser.sendMessage(player,"job-not-available");
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
						ResolverPassive.debug = !ResolverPassive.debug;
						SRPG.output("effect debugging set to "+ResolverPassive.debug);
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
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("list")) {
					// display hp values
					if (args[1].equalsIgnoreCase("tooldamage")) {
						Iterator<Map.Entry<String,Integer>> pairs = CombatInstance.damageTableTools.entrySet().iterator();
						while (pairs.hasNext()) {
							Map.Entry<String,Integer>pair = pairs.next();
							SRPG.output(pair.getKey()+": "+pair.getValue());
						}
						return true;
					}
					
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("charge") && SRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = SRPG.profileManager.get(args[1]);
					profile.charges = 11;
					SRPG.profileManager.save(profile, "chargedata");
					SRPG.output("gave player "+args[1]+" maximum charges");
					return true;
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("enrage") && SRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = SRPG.profileManager.get(args[1]);
					profile.addEffect(Settings.passives.get("rage"), new EffectDescriptor(10));
					SRPG.output("enraged player "+args[1]);
					return true;
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("poison") && SRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = SRPG.profileManager.get(args[1]);
					Integer potency;
					try {
						potency = Integer.parseInt(args[2]); 
					} catch (NumberFormatException ex) {
						potency = 1;
					} catch (IndexOutOfBoundsException ex) {
						potency = 1;
					}
					EffectDescriptor descriptor = new EffectDescriptor(5);
					descriptor.potency = potency < 1 ? 1 : potency;
					StructurePassive buff = Settings.passives.get(potency == 0 ? "weakpoison" : "poison");
					profile.addEffect(buff, descriptor);
					MessageParser.sendMessage(profile, "acquired-buff",buff.name);
					SRPG.output("poisoned player "+args[1]);
					return true;
				// add xp to a player (handle with care, no removal atm)
				} else if (args.length >= 3 && args[0].equalsIgnoreCase("xp") && SRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = SRPG.profileManager.get(args[1]);
					if (profile == null) {
						return false;
					}
					Integer amount;
					try {
						amount = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						SRPG.output("Not a valid number");
						return false;
					}
					
					profile.addXP(amount);
					SRPG.profileManager.save(profile, "xp");
					SRPG.output("gave "+amount.toString()+" xp to player "+args[1]);
					return true;
				} else if (args.length >= 4 && args[0].equalsIgnoreCase("setboost") && SRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = SRPG.profileManager.get(args[1]);
					if (profile == null) {
						SRPG.output("No player by that name");
						return false;
					}
					Double value;
					try {
						value = Double.parseDouble(args[3]);
					} catch (NumberFormatException e) {
						SRPG.output("Not a valid number");
						return false;
					}
					
					profile.stats.get(0).get(null).get(null).put(args[2], value);
					SRPG.output("Set boost "+args[2]+" to "+value);
					SRPG.output(profile.stats.toString());
					return true;
				}
			}
		}
		
		// reload settings (TODO: still needs proper testing, some settings might be not properly re-initialized)
		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				if (!(sender instanceof Player) || ((sender instanceof Player) && ((Player)sender).hasPermission("srpg.reload"))) {
					SRPG.settings.load();
					SRPG.output("Reloaded configuration");
					return true;
				}
			}
		}
		return false;
	}
}
