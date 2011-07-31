package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

// TODO: completely integrate permission checks

public class PassiveAbility {
	
	static boolean debug = false;
	
	public static void trigger(EntityDamageEvent event) {
		ProfileNPC profile = SRPG.profileManager.get((LivingEntity)event.getEntity());
		if (profile == null) return;
		Player player = profile instanceof ProfilePlayer ? ((ProfilePlayer) profile).player : null;
		
		if (event.getCause() == DamageCause.FALL) {
			// check permissions
			
			Integer height = (int) Math.ceil(event.getEntity().getFallDistance());
			Integer damage = height - 2 - profile.getStat("fall-damage-reduction", 0);
			
			// auto-roll roll
			double roll = SRPG.generator.nextDouble();
			double autorollChance = profile.getStat("roll-chance");
			// manual roll check
			boolean manualRoll = (player != null) && player.isSneaking() && (System.currentTimeMillis() - ((ProfilePlayer) profile).sneakTimeStamp) < profile.getStat("manual-roll-window", 0);
			if (manualRoll || roll < autorollChance) {
				damage -= profile.getStat("roll-damage-reduction",0);
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
		// quickfix for NPCs
		ProfilePlayer profile = SRPG.profileManager.get(player);
		if (profile == null) return;
		
		Material handitem = player.getItemInHand().getType();
		
		// check active tool and permissions
			// chance for no durability loss by skill
		double roll = SRPG.generator.nextDouble();
		if (roll < profile.getStat("durability-recovery-chance",player.getItemInHand().getType())){
			player.getItemInHand().setDurability((short)(player.getItemInHand().getDurability() + 1));
		}
		
		// TODO: later make them more configurable and less hardcoded
		// TODO: completely rewrite multidrops to some more elegant solution
		roll = SRPG.generator.nextDouble();
		Block block = event.getBlock();
		
		double doubleDropChance = profile.getStat("double-drop-chance", handitem);
		double tripleDropChance = profile.getStat("triple-drop-chance", handitem);
		
		// check active tool and permissions
		if (!(BlockEventListener.userPlacedBlocks.contains(block) || !Settings.MULTIDROP_VALID_BLOCKS.get(handitem).contains(block.getType()))) {
			if (debug) {
				SRPG.output("roll: "+(new Double(roll).toString()));
				SRPG.output("chances: "+(new Double(doubleDropChance).toString())+" for double, "+(new Double(tripleDropChance).toString())+" for triple");
			}
			
			ItemStack item = Utility.getNaturalDrops(block);
			
			if (roll < tripleDropChance) {
				item.setAmount(item.getAmount() * 2);
			} else if (roll >= tripleDropChance + doubleDropChance) {
				item.setAmount(item.getAmount() * 0);
			}
			
			if (item.getAmount() > 0) {
				block.getWorld().dropItemNaturally(block.getLocation(), item);
			}
		}
			
		// TODO: limit bonus drops to shovel block types (dirt, gravel)
		// TODO: move item selection to config
		// check active tool and permissions
		Material[] selection = {};
		int amount = 1; 
		double common = profile.getStat("extradrop-common",handitem);
		double uncommon = profile.getStat("extradrop-uncommon",handitem);
		double rare = profile.getStat("extradrop-rare",handitem);
		if (roll > uncommon+rare && roll <= common+uncommon+rare) {
			selection = new Material[] {Material.BONE,Material.COAL,Material.FEATHER,Material.FLINT,Material.STICK,Material.SEEDS,Material.LEATHER_BOOTS};
			amount = SRPG.generator.nextInt(3);
		} else if (roll > rare && roll <= uncommon+rare) {
			selection = new Material[] {Material.getMaterial(351),Material.ARROW,Material.SULPHUR,Material.CLAY_BALL,Material.GLOWSTONE_DUST,Material.STRING,Material.IRON_INGOT};
			amount = SRPG.generator.nextInt(2);
		} else if (roll <= rare) {
			selection = new Material[] {Material.DIAMOND,Material.GOLD_INGOT,Material.IRON_SWORD,Material.COMPASS,Material.IRON_BOOTS,Material.BUCKET};
		}
		if (selection.length != 0) {
			ItemStack item = new ItemStack(selection[SRPG.generator.nextInt(selection.length)],amount);
			if (item.getTypeId() == 351) {
				item.setDurability((byte)3);
			}
			block.getWorld().dropItemNaturally(block.getLocation(), item);
		}
	}
	
	public static void trigger(CombatInstance combat) {
		// check active tool and permissions
		double roll = SRPG.generator.nextDouble();
		if (combat.attacker instanceof ProfilePlayer && roll < combat.attacker.getStat("durability-recovery-chance",((ProfilePlayer)combat.attacker).player.getItemInHand().getType())){
			Player player = ((ProfilePlayer)combat.attacker).player;
			player.getItemInHand().setDurability((short)(player.getItemInHand().getDurability() + 1));
		}
	}

}