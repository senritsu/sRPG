package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.Messager;
import com.behindthemirrors.minecraft.sRPG.MiscGeneric;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
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
					Messager.sendMessage(player, "welcome");
					return true;
				// change locale
				} else if (args[0].equalsIgnoreCase("locale")) {
					if (args.length > 1 && Settings.localization.containsKey(args[1])) {
						profile.locale = args[1];
						SRPG.profileManager.save(profile, "locale");
						Messager.sendMessage(player, "locale-changed");
						return true;
					}
				// display xp,skillpoints,milestones (if enabled)
				} else if (args[0].equalsIgnoreCase("status")) {
					Messager.sendMessage(player, "status-header");
					// display xp
					if (player.hasPermission("srpg.xp")) {
						Messager.sendMessage(player, "xp");
					}
					return true;
				// display available charges with the current tool
				} else if (args[0].equalsIgnoreCase("charges")) {
					Messager.chargeDisplay(player, false);
					return true;
				// get info about a skill or increase it
				// TODO find NPE
				} else if (args.length >= 3 && (args[0]+" "+args[1]).equalsIgnoreCase("change to")) {
					if (Settings.worldBlacklist.contains(player.getWorld())) {
						Messager.sendMessage(player,"disabled-world");
					}
					// TODO: accommodate for names with spaces
					String name = MiscGeneric.join(new ArrayList<String>(new ArrayList<String>(Arrays.asList(args)).subList(2, args.length)), " ");
					StructureJob job = Settings.jobs.get(Settings.JOB_ALIASES.get(profile.locale).containsKey(name) ? 
							Settings.JOB_ALIASES.get(profile.locale).get(name) : name.toLowerCase());
					if (job != null) {
						if (player.hasPermission("srpg.jobs") || player.hasPermission("srpg.jobs."+job.signature)) {
							if (job == profile.currentJob) {
								Messager.sendMessage(player,"job-already-selected",job.signature);
							} else if (profile.jobAvailability.get(job)) {
								SRPG.profileManager.get(player).changeJob(job);
								Messager.sendMessage(player,"job-changed",job.signature);
							} else {
								Messager.sendMessage(player,"job-prerequisite-missing",job.signature);
//								for (Map.Entry<StructureJob, Integer> entry : job.prerequisites.entrySet()) {
//									MessageParser.sendMessage(player,"job-prerequisite-missing",entry.getKey().signature+","+entry.getValue());
//								}
							}
						} else {
							Messager.sendMessage(player,"job-no-permissions");
						}
					} else {
						Messager.sendMessage(player,"job-not-available");
					}
					return true;
				} else if (args[0].equalsIgnoreCase("stats")) {
					ArrayList<String> words = new ArrayList<String>();
					words.add("bla");
					words.add("very long word");
					words.add("foobar = 42");
					ArrayList<Integer> tabpoints = new ArrayList<Integer>();
					tabpoints.add(90);
					tabpoints.add(90);
					tabpoints.add(90);
					ArrayList<String> words2 = new ArrayList<String>();
					words2.add("laaawl");
					words2.add("some other word");
					words2.add("rage");
					player.sendMessage(Messager.columnize(words, tabpoints));
					player.sendMessage(Messager.columnize(words2, tabpoints));
				} else if (args[0].equalsIgnoreCase("passive")) {
					if (args.length >= 2) {
						String name = MiscGeneric.join(Arrays.asList(args).subList(1, args.length), " ");
						if (Settings.PASSIVE_ALIASES.get(profile.locale).containsKey(name.toLowerCase())) {
							name = Settings.PASSIVE_ALIASES.get(profile.locale).get(name.toLowerCase());
						} else if (Settings.PASSIVE_ALIASES.get(null).containsKey(name.toLowerCase())) {
							name = Settings.PASSIVE_ALIASES.get(null).get(name.toLowerCase());
						}
						StructurePassive passive = Settings.passives.get(name);
						if (passive != null) {
							for (String line : Messager.documentPassive(profile, passive)) {
								player.sendMessage(line);
							}
						}
						return true;
					} else {
						Messager.sendMessage(player, "needs-more-arguments");
						return false;
					}
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
						StructureJob job = Settings.jobs.get(name);
						Integer level = profile.jobLevels.get(job);
						Messager.sendMessage(player, "job-header",name);
						Messager.sendMessage(player, "job-progress",name);
						for (int i=0;i<job.maximumLevel;i++) {
							if (level < i) {
								break;
							}
							if (job.passives.containsKey(i)) {
								Messager.sendMessage(player, "level-header",job.signature);
								for (StructurePassive passive : job.passives.get(i).keySet()) {
									Messager.sendMessage(player, "passive-short", passive.signature);
								}
							}
						}
					} else {
						Messager.sendMessage(player,"job-not-available");
					}
					return true;
					
				// internal help (TODO: maybe eventually replaced with some help plugin)
				} else if (args[0].equalsIgnoreCase("help")) {
					//String topic = args[1];
					// TODO: distinguish between help command with arguments and without
					Messager.sendMessage(player, "help-general");
					return true;
				}
			}
		} else {
			// server console commands
			if (command.getName().equals("srpg")) {
				// toggle debug messages
				if (args.length >= 2 && args[0].equalsIgnoreCase("debug")) {
					// remove item stacks if something was incorrectly dropped
					if (args[1].equalsIgnoreCase("removeitems")) {
						for (Entity entity : SRPG.plugin.getServer().getWorlds().get(0).getEntities()) {
							if (entity instanceof Item) {
								entity.remove();
							}
						}
						SRPG.output("removed all items");
						return true;
					} else if (args[1].equalsIgnoreCase("spawninvincible")) {
						SpawnEventListener.spawnInvincible = !SpawnEventListener.spawnInvincible;
						SRPG.output("spawn invincibility set to "+(new Boolean(SpawnEventListener.spawnInvincible).toString()));
						return true;
					} else {
						if (!SRPG.debugmodes.contains(args[1])) {
							SRPG.debugmodes.add(args[1]);
							SRPG.output("added "+args[1]+" to debugmodes");
						} else {
							SRPG.debugmodes.remove(args[1]);
							SRPG.output("removed '"+args[1]+"' from debugmodes");
						}
						return true;
					}
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("list")) {
					// display hp values
					if (args[1].equalsIgnoreCase("tooldamage")) {
						Iterator<Map.Entry<String,Integer>> pairs = CombatInstance.damageTableTools.entrySet().iterator();
						while (pairs.hasNext()) {
							Map.Entry<String,Integer>pair = pairs.next();
							SRPG.dout(pair.getKey()+": "+pair.getValue());
						}
						return true;
					} else if (args[1].equalsIgnoreCase("inventory") && args.length >= 3) {
						ProfilePlayer profile = SRPG.profileManager.get(args[2]);
						if (profile != null) {
							for (int i = 0;i<40;i++) {
								try {
									ItemStack item = profile.player.getInventory().getItem(i);
									SRPG.dout(i+": "+item.getAmount()+" x "+item.getType().toString());
								} catch (ArrayIndexOutOfBoundsException ex) {
									SRPG.dout(i+": no valid slot");
								}
							}
						} else {
							SRPG.dout("No player by that name");
						}
						return true;
					}
					
				} else if (args.length >= 2 && SRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = SRPG.profileManager.get(args[1]);
					
					if (Settings.worldBlacklist.contains(profile.player.getWorld())) {
						SRPG.output("the targeted player is in a world that is set as disabled");
					} else if (args[0].endsWith("charge")) {
						if (args[0].startsWith("un")) {
							profile.charges = 0;
						} else {
							profile.charges = 11;
						}
						profile.updateChargeDisplay();
						SRPG.profileManager.save(profile, "chargedata");
						SRPG.output("gave player "+args[1]+" maximum charges");
					} else if (args[0].equalsIgnoreCase("enrage")) {
						StructurePassive buff = Settings.passives.get("rage");
						profile.addEffect(buff, new EffectDescriptor(10));
						SRPG.output("enraged player "+args[1]);
						Messager.sendMessage(profile, "acquired-buff",buff.signature);
					} else if (args[0].equalsIgnoreCase("protect")) {
						StructurePassive buff = Settings.passives.get("invincibility");
						profile.addEffect(buff, new EffectDescriptor(10));
						SRPG.output("protected player "+args[1]);
						Messager.sendMessage(profile, "acquired-buff",buff.signature);
					} else if (args[0].equalsIgnoreCase("poison")) {
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
						Messager.sendMessage(profile, "acquired-buff",buff.signature);
						SRPG.output("poisoned player "+args[1]);
					} else if (args[0].equalsIgnoreCase("xp") && args.length >= 3) {
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
					} else if (args[0].equalsIgnoreCase("setboost") && args.length >= 4) {
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
					} else {
						return false;
					}
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
