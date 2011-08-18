package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class ResolverEffects {

	static void setCombatState(CombatInstance combat, ConfigurationNode node) {
		if (SRPG.generator.nextDouble() < node.getDouble("chance", 1.0)) {
			if (node.getBoolean("canceled", false)) {
				combat.cancel();
			} else if (node.getBoolean("crit", false)) {
				combat.crit = true;
			} else if (node.getBoolean("parry", false)) {
				combat.parry = true;
			} else if (node.getBoolean("evade", false)) {
				combat.evade = true;
			} else if (node.getBoolean("miss", false)) {
				combat.miss = true;
			}
		}
	}
	
	static void applyBuff(ProfileNPC profile, ConfigurationNode node) {
		String name = node.getString("name");
		EffectDescriptor descriptor = new EffectDescriptor(name);
		descriptor.duration = node.getInt("duration", 0);
		StructurePassive buff = Settings.passives.get(MiscBukkit.stripPotency(name));
		profile.addEffect(buff, descriptor);
		Messager.sendMessage(profile, "acquired-buff",buff.signature);
	}
	
	static void directDamage(ProfileNPC profile, ConfigurationNode node, EffectDescriptor descriptor) {
		if (SRPG.generator.nextDouble() <= node.getDouble("chance", 1.0)) {
			profile.entity.damage(node.getInt("value", 0) * descriptor.potency);
		}
	}
	
	static void transmuteItem(ProfileNPC profile, ConfigurationNode node, EffectDescriptor descriptor) {
		if (profile instanceof ProfilePlayer) {
			Player player = ((ProfilePlayer)profile).player;
			// parse ingredients and results
			ArrayList<HashMap<Material,Integer>> transmute = new ArrayList<HashMap<Material,Integer>>();
			String[] keys = new String[] {"from","to"};
			ArrayList<String> temp;
			ArrayList<Integer> temp2;
			for (int i=0;i<2;i++) {
				transmute.add(new HashMap<Material, Integer>());
				temp = (ArrayList<String>) node.getStringList(keys[i], new ArrayList<String>());
				temp2 = (ArrayList<Integer>) node.getIntList(keys[i]+"-amounts", new ArrayList<Integer>());
				for (int j=0;j<temp.size();j++ ) {
					transmute.get(i).put(MiscBukkit.parseMaterial(temp.get(j)), temp2.get(j));
				}
			}
			SRPG.output(transmute.toString());
			// randomize the ingredients and results if applicable
			Boolean[] flags = new Boolean[] {!node.getBoolean("consume-all", false),node.getBoolean("random-result", false)};
			for (int i=0;i<2;i++) {
				if (flags[i]) {
					Material choice = player.getItemInHand().getType();
					HashMap<Material, Integer> selection = new HashMap<Material, Integer>();
					if (transmute.get(i).containsKey(choice)) {
						selection.put(choice, transmute.get(i).get(choice));
					} else {
						ArrayList<Material> pool = new ArrayList<Material>(transmute.get(i).keySet());
						while (!pool.isEmpty()) {
							choice = pool.get(SRPG.generator.nextInt(pool.size()));
							if (i == 0 && !player.getInventory().contains(transmute.get(i).get(choice))) {
								pool.remove(choice);
								continue;
							} else {
								break;
							}
						}
						selection.put(choice, transmute.get(i).get(choice));
					}
					transmute.set(i, selection);
				}
			}
			SRPG.output(transmute.toString());
			// check for ingredients
			boolean sufficient = !transmute.get(0).isEmpty();
			HashMap<Material,HashMap<ItemStack,Integer>> stacks = new HashMap<Material,HashMap<ItemStack,Integer>>(); 
			for (Material material : transmute.get(0).keySet()) {
				if (!player.getInventory().contains(material, transmute.get(0).get(material))) {
					sufficient = false;
				}
				stacks.put(material,new HashMap<ItemStack, Integer>());
				for (Entry<Integer, ? extends ItemStack> entry : player.getInventory().all(material).entrySet()) {
					stacks.get(material).put(entry.getValue(), entry.getKey());
				}
			}
			if (sufficient) {
				for (Material material : transmute.get(0).keySet()) {
					player.getInventory().removeItem(new ItemStack(material, transmute.get(0).get(material)));
				}
				for (Material material : transmute.get(1).keySet()) {
					HashMap<Integer, ItemStack> spillover = player.getInventory().addItem(new ItemStack(material, transmute.get(1).get(material)));
					for (ItemStack item : spillover.values()) {
						player.getWorld().dropItemNaturally(player.getLocation(), item);
					}
				}
				((ProfilePlayer)profile).validateActives();
			} else {
				SRPG.output("not enough items in inventory");
			}
		}
	}
	
	static void blockChange(ProfileNPC profile, Block block, ArrayList<Material> whitelist, ConfigurationNode node, EffectDescriptor descriptor) {
		SRPG.output("entering effect resolver for block change");
		String materialName = node.getString("change-to");
		Material material = materialName == null ? Material.AIR : MiscBukkit.parseMaterial(materialName);
		
		boolean temporary = node.getBoolean("temporary", false);
		boolean drop = material != Material.AIR ? false : node.getBoolean("drop", false);
		int delay = node.getInt("delay", 0);
		
		ArrayList<ArrayList<Block>> blocks = new ArrayList<ArrayList<Block>>();
		blocks.add(new ArrayList<Block>());
		blocks.get(0).add(block);
		String shape = node.getString("shape");
		
		if (shape.equalsIgnoreCase("line")) {
			String direction = node.getString("direction");
			if (direction == null) {
				direction = "forward";
			}
			blocks.addAll(BlockShapes.line(block, 
					node.getBoolean("relative", false) ? 
						MiscGeometric.relativeFacing(direction , profile.entity) : 
						MiscGeometric.directionToFacing.get(direction),
					node.getInt("length", 0)));
		} else if (shape.equalsIgnoreCase("cross2D")) {
			String normal = node.getString("direction");
			if (normal == null) {
				normal = "up";
			}
			blocks.addAll(BlockShapes.cross2D(block, node.getBoolean("relative", false) ? 
						MiscGeometric.relativeFacing(normal , profile.entity) : 
						MiscGeometric.directionToFacing.get(normal),
					node.getInt("length", 0)));
		}
		
		int partDelay = node.getInt("part-delay", 0);
		int blockDelay = node.getInt("block-delay", 0);
		boolean cascadeParts = node.getBoolean("cascade-parts", false);
		boolean cascadeBlocks = node.getBoolean("cascade-blocks", false);
		int combinedDelay = delay;
		for (int i=0;i<blocks.size();i++) {
			ArrayList<Block> part = blocks.get(i);
			
			if (cascadeParts) {
				combinedDelay += partDelay;
			} else {
				combinedDelay = delay;
			}
			
			for (int j=0;j<part.size();j++) {
				Block activeBlock = part.get(j);
				
				if (cascadeBlocks) {
					combinedDelay += blockDelay;
				}
				
				if (whitelist.isEmpty() || whitelist.contains(activeBlock.getType())) {
					if (material == Material.AIR && !temporary) {
						SRPG.cascadeQueueScheduler.scheduleBlockBreak(activeBlock, combinedDelay, profile instanceof ProfilePlayer && node.getBoolean("break-event", false) ? (ProfilePlayer)profile : null, drop);
					} else if (temporary) {
						SRPG.cascadeQueueScheduler.scheduleTemporaryBlockChange(activeBlock, material, combinedDelay, node.getInt("duration", 0), node.getBoolean("protect", false));
					} else {
						SRPG.cascadeQueueScheduler.scheduleBlockChange(activeBlock, material, combinedDelay);
					}
				}
			}
		}
	}

}
