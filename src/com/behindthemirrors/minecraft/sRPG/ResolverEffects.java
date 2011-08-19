package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
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
		if (profile == null) {
			return;
		}
		String name = node.getString("name");
		EffectDescriptor descriptor = new EffectDescriptor(name);
		descriptor.duration = node.getInt("duration", 0);
		StructurePassive buff = Settings.passives.get(MiscBukkit.stripPotency(name));
		profile.addEffect(buff, descriptor);
		Messager.sendMessage(profile, "acquired-buff",buff.signature);
	}
	
	static void directDamage(ProfileNPC profile, ConfigurationNode node, EffectDescriptor descriptor) {
		if (profile == null) {
			return;
		}
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
	
	static void blockChange(ProfileNPC profile, Block block, ConfigurationNode node, EffectDescriptor descriptor) {
		if (block == null) {
			return;
		}
		
		SRPG.output(block.toString());
		
		try {
			ArrayList<Integer> offset = (ArrayList<Integer>)node.getIntList("offset", new ArrayList<Integer>());
			offset = MiscGeometric.relativeOffset(offset,MiscGeometric.getEntityFacing(profile.entity));
			block = block.getRelative(offset.get(0),offset.get(1) , offset.get(2));
			SRPG.output(block.toString());
		} catch (IndexOutOfBoundsException ex) {
			SRPG.output("error while getting offset");
		}
		
		SRPG.output("entering effect resolver for block change");
		String materialName = node.getString("change-to");
		Material material = materialName == null ? Material.AIR : MiscBukkit.parseMaterial(materialName);
		
		boolean temporary = node.getBoolean("temporary", false);
		boolean drop = material != Material.AIR ? false : node.getBoolean("drop", false);
		int delay = node.getInt("delay", 0);
		
		ArrayList<ArrayList<Block>> blockArray = new ArrayList<ArrayList<Block>>();
		blockArray.add(new ArrayList<Block>());
		blockArray.get(0).add(block);
		String shape = node.getString("shape");
		
		if (shape.equalsIgnoreCase("line")) {
			String direction = node.getString("direction");
			if (direction == null) {
				direction = "forward";
			}
			blockArray.addAll(BlockShapes.line(block, 
					node.getBoolean("relative", false) ? 
						MiscGeometric.relativeFacing(direction , profile.entity) : 
						MiscGeometric.directionToFacing.get(direction),
					node.getInt("length", 0)));
		} else if (shape.equalsIgnoreCase("cross2D")) {
			String normal = node.getString("direction");
			if (normal == null) {
				normal = "up";
			}
			blockArray.addAll(BlockShapes.cross2D(block, node.getBoolean("relative", false) ? 
						MiscGeometric.relativeFacing(normal , profile.entity) : 
						MiscGeometric.directionToFacing.get(normal),
					node.getInt("length", 0)));
		}
		
		int partDelay = node.getInt("part-delay", 0);
		int blockDelay = node.getInt("block-delay", 0);
		boolean cascadeParts = node.getBoolean("cascade-parts", false);
		boolean cascadeBlocks = node.getBoolean("cascade-blocks", false);
		int combinedDelay = delay;
		
		ArrayList<Material> whitelist = MiscBukkit.parseMaterialList(node.getStringList("whitelist", new ArrayList<String>()));
		// TODO: think of a way to remove the null from the parsed material list while still having everything work properly
		whitelist.remove(null);
		
		ArrayList<Block> blocks = new ArrayList<Block>();
		ArrayList<Integer> delays = new ArrayList<Integer>();
		int lastDelay = 0;
		
		for (int i=0;i<blockArray.size();i++) {
			ArrayList<Block> part = blockArray.get(i);
			
			if (cascadeParts) {
				combinedDelay += partDelay;
			} else {
				combinedDelay = delay;
			}
			
			for (int j=0;j<part.size();j++) {
				if (cascadeBlocks) {
					combinedDelay += blockDelay;
				}
				
				blocks.add(part.get(j));
				delays.add(combinedDelay);
				
				lastDelay = combinedDelay > lastDelay ? combinedDelay : lastDelay;
			}
		}
		
		int activeRevertDelay = node.getInt("duration", 0);
		String revertMode = node.getString("revert-as");
		double factor = 0;
		if (revertMode != null) {
			if (revertMode.equalsIgnoreCase("instant")) {
				factor = 1;
			} else if (revertMode.equalsIgnoreCase("lifo")) {
				factor = 2;
			} else if (revertMode.equalsIgnoreCase("lifo+")) {
				factor = 1.5;
			} else if (revertMode.equalsIgnoreCase("random")) {
				factor = 1.2+SRPG.generator.nextDouble();
			}
		}
		
		for (int i = 0; i < blocks.size();i++) {
			Block activeBlock = blocks.get(i);
			int activeDelay = delays.get(i);
			if (whitelist.isEmpty() || whitelist.contains(activeBlock.getType())) {
				
				if (material == Material.AIR && !temporary) {
					SRPG.cascadeQueueScheduler.scheduleBlockBreak(activeBlock, activeDelay, profile instanceof ProfilePlayer && node.getBoolean("break-event", false) ? (ProfilePlayer)profile : null, drop);
				} else if (temporary) {
					SRPG.output("scheduling block change for "+activeDelay+" set to revert after "+activeRevertDelay);
					SRPG.cascadeQueueScheduler.scheduleTemporaryBlockChange(activeBlock, material, activeDelay, (int) (activeRevertDelay + factor*(lastDelay - activeDelay)), node.getBoolean("protect", false));
				} else {
					SRPG.cascadeQueueScheduler.scheduleBlockChange(activeBlock, material, activeDelay);
				}
			}
		}
	}

	public static void impulse(ProfileNPC profile, ConfigurationNode node, EffectDescriptor descriptor) {
		if (profile == null) {
			return;
		}
		Vector v = new Vector();
		// pitch : up > down
		// yaw : clockwise west > north > ... from -180 to 180
		// x = NORTH, z = WEST, y = UP
		
		double x = node.getDouble("x", 0);
		double y = node.getDouble("y", 0);
		double z = node.getDouble("z", 0);
		
		boolean ypf = node.getBoolean("use-y-p-f", false);
		
		double yaw = ypf ? node.getDouble("yaw", 0) : Math.toDegrees(-Math.atan2(x, z));
		double force = ypf ? node.getDouble("force", 0) : Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
		double pitch = ypf ? node.getDouble("pitch", 0) : Math.toDegrees(Math.asin(y / force));
		
		if (node.getBoolean("use-y-p-f", false)) {
			yaw = node.getDouble("yaw", 0);
			pitch = node.getDouble("pitch", 0);
			force = node.getDouble("force", 0);
		}
		
		SRPG.output(""+yaw+" "+pitch+" "+force);
		
		if (node.getBoolean("relative", false)) {
			yaw += profile.entity.getLocation().getYaw();
			pitch += profile.entity.getLocation().getPitch(); 
		}
		
		v.setX(- Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)) );
		v.setY(Math.sin(Math.toRadians(pitch)));
		v.setZ(Math.cos(Math.toRadians(pitch))* Math.cos(Math.toRadians(yaw)) );
		v.multiply(force);
			
		if (node.getBoolean("add", false)) {
			v = v.add(profile.entity.getVelocity());
		}
		
		
		SRPG.output(profile.entity.getVelocity().toString());
		profile.entity.setVelocity(v);
		SRPG.output(v.length()+"");
		SRPG.output(profile.entity.getVelocity().toString());
	}

}
