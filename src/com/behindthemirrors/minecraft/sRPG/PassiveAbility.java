package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

// TODO: completely integrate permission checks

public class PassiveAbility {
	
	public static void trigger(Player player, EntityDamageEvent event) {
		if (event.getCause() == DamageCause.FALL) {
			// check permissions
			if (!SRPG.permissionHandler.has(player,"srpg.skills.ukemi")) {
				return;
			}
			
			Integer skillpoints = SRPG.playerDataManager.get(player).getSkill("ukemi");
			ArrayList<String> milestones = SRPG.playerDataManager.get(player).getMilestones("ukemi");
			
			Integer height = (int) Math.ceil(player.getFallDistance());
			Integer damage = height - 2 - milestones.size();
			
			// auto-roll roll
			double roll = SRPG.generator.nextDouble();
			double autorollChance = skillpoints * Settings.advanced.getDouble("skills.effects.ukemi.autoroll-chance", 0) + (milestones.size()-1) * Settings.advanced.getDouble("skills.effects.ukemi.milestone-bonus",0);
			// manual roll check
			boolean manualRoll = player.isSneaking() && (System.currentTimeMillis() - SRPG.playerDataManager.get(player).sneakTimeStamp) < Settings.advanced.getInt("skills.effects.ukemi.roll-window", 0);
			if (manualRoll || roll < autorollChance) {
				damage -= skillpoints;
				if (manualRoll) {
					MessageParser.sendMessage(player, "roll-manual");
				} else {
					MessageParser.sendMessage(player, "roll-auto");
				}
			}
			// no negative damage
			if (damage < 0) {
				damage = 0;
			}
			
			event.setDamage(damage);
		}
	}
	
	public static void trigger(Player player, BlockBreakEvent event) {
		String skillname = Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(player.getItemInHand().getType());
		if (skillname == null) {
			return;
		}
		
		Integer skillpoints = SRPG.playerDataManager.get(player).getSkill(skillname);
		ArrayList<String> milestones = SRPG.playerDataManager.get(player).getMilestones(skillname); 
		
		// check active tool and permissions
		if (skillname != "sword" && SRPG.permissionHandler.has(player, "srpg.skills."+skillname+".passive")) {
			// chance for no durability loss by skill
			double roll = SRPG.generator.nextDouble();
			double durabilityRecoveryChance = skillpoints * Settings.advanced.getDouble("skills.effects."+skillname+".durability-recovery-chance", 0) + (milestones.size()-1) * Settings.advanced.getDouble("skills.effects."+skillname+".milestone-bonus",0);
			// focus
			if (SRPG.playerDataManager.get(player).focusAllowed && SRPG.permissionHandler.has(player, "srpg.skills.focus")) {
				durabilityRecoveryChance *= 1.0 + SRPG.playerDataManager.get(player).getSkill("focus") * Settings.advanced.getDouble("skills.effects.focus.boost", 0);
			}
			if (roll < durabilityRecoveryChance){
				player.getItemInHand().setDurability((short)(player.getItemInHand().getDurability() + 1));
			}
		}
		
		// TODO: later make them more configurable and less hardcoded
		// TODO: completely rewrite multidrops to some more elegant solution
		double roll = SRPG.generator.nextDouble();
		Material material = event.getBlock().getType();
		
		double doubleDropChance = 0.0;
		double tripleDropChance = 0.0;
		
		// check active tool and permissions
		if ((skillname.equals("pickaxes") && SRPG.permissionHandler.has(player, "srpg.skills.pickaxes.milestones")) || (skillname.equals("axes") && SRPG.permissionHandler.has(player, "srpg.skills.axes.milestones"))) {
			if (!Settings.MULTIDROP_VALID_BLOCKS.get(player.getItemInHand().getType()).contains(material)) {
				return;
			}
			if (milestones.contains("apprentice")) {
				doubleDropChance += Settings.advanced.getDouble("skills.passive-abilities."+skillname+".apprentice.double-drop-chance", 0);
			}
			if (milestones.contains("expert")) {
				doubleDropChance += Settings.advanced.getDouble("skills.passive-abilities."+skillname+".expert.double-drop-chance", 0);
			}
			if (milestones.contains("master")) {
				doubleDropChance += Settings.advanced.getDouble("skills.passive-abilities."+skillname+".master.double-drop-chance", 0);
				tripleDropChance += Settings.advanced.getDouble("skills.passive-abilities."+skillname+".master.triple-drop-chance", 0);
			}
			
			byte data = event.getBlock().getData();
			Material dropMaterial = Settings.BLOCK_DROPS.get(material);
			if (dropMaterial == null) {
				dropMaterial = material;
			}
			int[] amountRange = Settings.BLOCK_DROP_AMOUNTS.get(dropMaterial);
			if (amountRange == null) {
				amountRange = new int[] {1,1};
			}
			
			int amount = amountRange[0] + SRPG.generator.nextInt(amountRange[1]);
			
			if (roll < tripleDropChance) {
				amount *= 3;
			} else if (roll > tripleDropChance && roll < (tripleDropChance + doubleDropChance)) {
				amount *= 2;
			}
			
			ItemStack item = new ItemStack(dropMaterial,amount);
			// wood
			if (dropMaterial.getId() == 17) {
				item.setDurability(data);
			// lapis
			} else if (dropMaterial.getId() == 351) {
				item.setDurability((byte)4);
			}
			player.getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
			
		// TODO: limit bonus drops to shovel block types (dirt, gravel)
		// TODO: move item selection to config
		// check active tool and permissions
		} else if (skillname.equals("shovels") && SRPG.permissionHandler.has(player, "srpg.skills.shovels.milestones")) {
			Material[] selection = {};
			int amount = 1; 
			double common = Settings.advanced.getDouble("skills.passive-abilities.shovels.apprentice.common-drop-chance", 0);
			double uncommon = Settings.advanced.getDouble("skills.passive-abilities.shovels.expert.uncommon-drop-chance", 0);
			double rare = Settings.advanced.getDouble("skills.passive-abilities.shovels.master.rare-drop-chance", 0);
			if (milestones.contains("apprentice") && roll > uncommon+rare && roll <= common+uncommon+rare) {
				selection = new Material[] {Material.BONE,Material.COAL,Material.FEATHER,Material.FLINT,Material.STICK,Material.SEEDS,Material.LEATHER_BOOTS};
				amount = SRPG.generator.nextInt(3);
			} else if (milestones.contains("expert") && roll > rare && roll <= uncommon+rare) {
				selection = new Material[] {Material.getMaterial(351),Material.ARROW,Material.SULPHUR,Material.CLAY_BALL,Material.GLOWSTONE_DUST,Material.STRING,Material.IRON_INGOT};
				amount = SRPG.generator.nextInt(2);
			} else if (milestones.contains("master") && roll <= rare) {
				selection = new Material[] {Material.DIAMOND,Material.GOLD_INGOT,Material.IRON_SWORD,Material.COMPASS,Material.IRON_BOOTS,Material.BUCKET};
			}
			if (selection.length != 0) {
				ItemStack item = new ItemStack(selection[SRPG.generator.nextInt(selection.length)],amount);
				if (item.getTypeId() == 351) {
					item.setDurability((byte)3);
				}
				player.getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
			}
		
		// check active tool and permissions
		} else if (skillname.equals("hoes") && SRPG.permissionHandler.has(player, "srpg.skills.hoes")) {
			// no abilities yet
		} 
	}
	
	public static void trigger(Player player, CombatInstance combat, boolean offensive) {
		String skillname = Settings.TOOL_MATERIAL_TO_TOOL_GROUP.get(player.getItemInHand().getType());
		// bow is not in the normal tool list, so check for it
		if (player.getItemInHand().getType() == Material.BOW) {
			skillname = "bow";
		} else if (skillname == null) {
			return;
		}
		
		Integer skillpoints = SRPG.playerDataManager.get(player).getSkill(skillname);
		ArrayList<String> milestones = SRPG.playerDataManager.get(player).getMilestones(skillname); 
		
		// chance for no durability loss by skill
		// check active tool and permissions
		if (skillname == "sword" && SRPG.permissionHandler.has(player, "srpg.skills.sword.passive")) {
			double roll = SRPG.generator.nextDouble();
			double durabilityRecoveryChance = skillpoints * Settings.advanced.getDouble("skills.effects."+skillname+".durability-recovery-chance", 0) + (milestones.size()-1) * Settings.advanced.getDouble("skills.effects."+skillname+".milestone-bonus",0);
			// focus
			if (SRPG.playerDataManager.get(player).focusAllowed && SRPG.permissionHandler.has(player, "srpg.skills.focus")) {
				durabilityRecoveryChance *= 1.0 + SRPG.playerDataManager.get(player).getSkill("focus") * Settings.advanced.getDouble("skills.effects.focus.boost", 0);
			}
			if (roll < durabilityRecoveryChance){
				player.getItemInHand().setDurability((short)(player.getItemInHand().getDurability() + 1));
			}
		}
		
		// chance for evasion
		double roll = SRPG.generator.nextDouble();
		double evadeChance = skillpoints * Settings.advanced.getDouble("skills.effects.evasion.chance", 0) + (milestones.size()-1) * Settings.advanced.getDouble("skills.effects.evasion.milestone-bonus",0);
		// focus
		if (SRPG.playerDataManager.get(player).focusAllowed && SRPG.permissionHandler.has(player, "srpg.skills.focus")) {
			evadeChance *= 1.0 + SRPG.playerDataManager.get(player).getSkill("focus") * Settings.advanced.getDouble("skills.effects.focus.boost", 0);
		}
			
		// check permissions
		if (SRPG.permissionHandler.has(player, "srpg.skills.evasion")) {
			if (!offensive) {
				if (roll < evadeChance) {
					combat.cancel("evasion-attacker");
					MessageParser.sendMessage(player, "evasion-defender");
				}
			}
		}
		
		// ability's numerical effects configurable TODO: make them completely configurable in the restraints of some general effects
		// check active tool and permissions
		if (skillname.equals("swords") && SRPG.permissionHandler.has(player, "srpg.skills.swords.milestones")) {
			if (offensive) {
				if (milestones.contains("expert")) {
					combat.critChance += Settings.advanced.getDouble("skills.passive-abilities.swords.expert.crit-chance", 0);
				}
				if (milestones.contains("master")) {
					combat.critChance += Settings.advanced.getDouble("skills.passive-abilities.swords.master.crit-chance", 0);
					combat.critMultiplier += Settings.advanced.getDouble("skills.passive-abilities.swords.master.crit-multiplier", 0);
				}
			} else {
				double parryChance = 0.0;
				if (milestones.contains("apprentice")) {
					parryChance += Settings.advanced.getDouble("skills.passive-abilities.swords.apprentice.parry-chance", 0);
				}
				if (milestones.contains("expert")) {
					parryChance += Settings.advanced.getDouble("skills.passive-abilities.swords.expert.parry-chance", 0);
				}
				// parry check
				if (roll > evadeChance && roll <= evadeChance + parryChance) {
					combat.cancel("parry-attacker");
					MessageParser.sendMessage(player, "parry-defender");
				}
			}
		// check active tool and permissions
		} else if (skillname.equals("bow")){
			if (offensive) {
				// increased crit chance by skill
				// check permissions
				if (SRPG.permissionHandler.has(player, "srpg.skills.bow.passive")) {
					double critBonus = skillpoints * Settings.advanced.getDouble("skills.effects.bow.crit-chance", 0) + (milestones.size()-1) * Settings.advanced.getDouble("skills.effects.bow.milestone-bonus",0);
					// focus
					if (SRPG.playerDataManager.get(player).focusAllowed && SRPG.permissionHandler.has(player, "srpg.skills.focus")) {
						critBonus *= 1.0 + SRPG.playerDataManager.get(player).getSkill("focus") * Settings.advanced.getDouble("skills.effects.focus.boost", 0);
					}
					combat.critChance += critBonus;
				}
				// passive abilities
				// check permissions
				if (SRPG.permissionHandler.has(player, "srpg.skills.bow.active")) {
					if (milestones.contains("apprentice")) {
						combat.modifier += Settings.advanced.getInt("skills.passive-abilities.bow.apprentice.damage-modifier", 0);
					}
					if (milestones.contains("expert")) {
						combat.critMultiplier += Settings.advanced.getDouble("skills.passive-abilities.bow.expert.crit-multiplier", 0);
					}
					if (milestones.contains("master")) {
						combat.modifier += Settings.advanced.getInt("skills.passive-abilities.bow.master.damage-modifier", 0);
						combat.critMultiplier += Settings.advanced.getDouble("skills.passive-abilities.bow.master.crit-multiplier", 0);
					}
				}
			}
		}
	}

}