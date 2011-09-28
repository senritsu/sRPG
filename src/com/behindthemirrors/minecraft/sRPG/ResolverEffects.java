package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.bukkit.util.config.ConfigurationNode;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.Watcher;


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
	
	static void manipulateItem(ProfileNPC source, ProfileNPC target, ConfigurationNode node, EffectDescriptor descriptor) {
		String action = node.getString("action");
		ArrayList<String> locations = (ArrayList<String>)node.getStringList("location", new ArrayList<String>());
		if (action == null) {
			return;
		}
		
		ArrayList<Material> whitelist = MiscBukkit.parseMaterialList(node.getStringList("whitelist", new ArrayList<String>()));
		ArrayList<Material> blacklist = MiscBukkit.parseMaterialList(node.getStringList("blacklist", new ArrayList<String>()));
		
		ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
		for (String location : locations) {
			if (target instanceof ProfilePlayer) {
				ArrayList<Integer> slots = new ArrayList<Integer>();
				PlayerInventory inventory = ((Player)target.entity).getInventory();
				if (location.equalsIgnoreCase("hand")) {
					slots.add(inventory.getHeldItemSlot());
				} else if (location.equalsIgnoreCase("quickbar")) {
					for (int i=0;i<=8;i++) {
						slots.add(i);
					}
				} else if (location.equalsIgnoreCase("inventory")) {
					for (int i=9;i<=35;i++) {
						slots.add(i);
					}
				} else if (location.equalsIgnoreCase("armor")) {
					slots.addAll(Arrays.asList(new Integer[] {36,37,38,39}));
				} else if (location.equalsIgnoreCase("boots")) {
					slots.add(36);
				} else if (location.equalsIgnoreCase("leggings")) {
					slots.add(37);
				} else if (location.equalsIgnoreCase("chestplate")) {
					slots.add(38);
				} else if (location.equalsIgnoreCase("helmet")) {
					slots.add(39);
				}
				Iterator<Integer> iterator = slots.iterator();
				while (iterator.hasNext()) {
					Material type = inventory.getItem(iterator.next()).getType();
					if ((whitelist.isEmpty() && blacklist.contains(type)) || 
						(!whitelist.isEmpty() && !whitelist.contains(type)) ||
						type == Material.AIR) {
						iterator.remove();
					}
				}
				if (!node.getBoolean("all", false)) {
					int choice = slots.get(SRPG.generator.nextInt(slots.size()));
					slots.clear();
					slots.add(choice);
				}
				for (int i : slots) {
					ItemStack stack = inventory.getItem(i);
					int amount = Math.min(node.getInt("amount",stack.getAmount()),stack.getAmount());
					ItemStack copy = new ItemStack(stack.getType(),amount);
					stack.setAmount(stack.getAmount() - amount);
					
					copy.setDurability(stack.getDurability());
					copy.setData(stack.getData());
					stacks.add(copy);
					
					if (!(stack.getAmount() > 0)) {
						inventory.clear(i);
					}
				}
			} else {
				String creature = MiscBukkit.getEntityName(target.entity);
				if (location.equalsIgnoreCase("hand")) {
					if (creature.equalsIgnoreCase("skeleton")) {
						stacks.add(new ItemStack(Material.BOW, 1));
					} else if (creature.equalsIgnoreCase("pigzombie")) {
						stacks.add(new ItemStack(Material.GOLD_SWORD, 1));
					}
				} else if (location.equalsIgnoreCase("inventory")) {
					stacks.addAll(MiscBukkit.getNaturalDrops(target.entity));
				}
				Iterator<ItemStack> iterator = stacks.iterator();
				while (iterator.hasNext()) {
					Material type = iterator.next().getType();
					if ((whitelist.isEmpty() && blacklist.contains(type)) || 
						(!whitelist.isEmpty() && !whitelist.contains(type)) ||
						type == Material.AIR) {
						iterator.remove();
					}
				}
				if (!node.getBoolean("all", false)) {
					ItemStack choice = stacks.get(SRPG.generator.nextInt(stacks.size()));
					stacks.clear();
					stacks.add(choice);
				}
			}
		}
		
		for (ItemStack stack : stacks) {
			if (action.equalsIgnoreCase("steal")) {
				if (source instanceof ProfilePlayer) {
					((Player)source.entity).getInventory().addItem(stack);
				}
			} else if (action.equalsIgnoreCase("drop")) {
				if (stack.getAmount() > 0) {
					Item item = target.entity.getWorld().dropItemNaturally(target.entity.getLocation(), stack);
					Watcher.protect(item,node.getInt("protected-for",0));
				}
			}
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
			SRPG.dout(transmute.toString(),"effects");
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
			SRPG.dout(transmute.toString(),"effects");
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
				SRPG.dout("not enough items in inventory","effects");
			}
		}
	}
	
	static void blockChange(ProfileNPC profile, Location location, Block block, ConfigurationNode node, EffectDescriptor descriptor) {
		if (block == null) {
			return;
		}
		
		String materialName = node.getString("change-to");
		Material material = materialName == null ? Material.AIR : MiscBukkit.parseMaterial(materialName);
		
		boolean temporary = node.getBoolean("temporary", false);
		boolean drop = material != Material.AIR ? false : node.getBoolean("drop", false);
		
		ArrayList<ArrayList<Block>> blockArray = new ArrayList<ArrayList<Block>>();
		blockArray.add(new ArrayList<Block>());
		blockArray.get(0).add(block);
		String shape = node.getString("shape");
		
		boolean relative = node.getBoolean("relative", false);
		
		if (shape.equalsIgnoreCase("line")) {
			String direction = node.getString("direction");
			if (direction == null) {
				direction = "forward";
			}
			
			blockArray.addAll(BlockShapes.line(block, relative ? 
						MiscGeometric.relativeFacing(direction , location) : 
						MiscGeometric.directionToFacing.get(direction),
					node.getInt("length", 0)));
		} else if (shape.equalsIgnoreCase("cross2D")) {
			String normal = node.getString("direction");
			if (normal == null) {
				normal = "up";
			}
			blockArray.addAll(BlockShapes.cross2D(block, relative ? 
						MiscGeometric.relativeFacing(normal , location) : 
						MiscGeometric.directionToFacing.get(normal),
					node.getInt("length", 0)));
		} else if (shape.equalsIgnoreCase("cross3D")) {
			ArrayList<BlockFace> ignore = new ArrayList<BlockFace>();
			for (String direction : node.getStringList("ignore",new ArrayList<String>())) {
				if (relative) {
					ignore.add(MiscGeometric.relativeFacing(direction , location));
				} else {
					ignore.add(MiscGeometric.directionToFacing.get(direction));
				}
			}
			blockArray.addAll(BlockShapes.cross3D(block, ignore, node.getInt("length", 0)));
		} else if (shape.equalsIgnoreCase("circle")) {
			String normal = node.getString("direction");
			if (normal == null) {
				normal = "up";
			}
			blockArray.addAll(BlockShapes.circle(block, relative ? 
					MiscGeometric.relativeFacing(normal , location) : 
					MiscGeometric.directionToFacing.get(normal),
				node.getInt("length", 0)));
		} else if (shape.equalsIgnoreCase("sphere")) {
			ArrayList<BlockFace> ignore = new ArrayList<BlockFace>();
			for (String direction : node.getStringList("ignore",new ArrayList<String>())) {
				if (relative) {
					ignore.add(MiscGeometric.relativeFacing(direction , location));
				} else {
					ignore.add(MiscGeometric.directionToFacing.get(direction));
				}
			}
			blockArray.addAll(BlockShapes.sphere(block, ignore, node.getInt("length", 0)));
		} 
		
		int partDelay = node.getInt("part-delay", 0);
		int blockDelay = node.getInt("block-delay", 0);
		boolean cascadeParts = node.getBoolean("cascade-parts", false);
		boolean cascadeBlocks = node.getBoolean("cascade-blocks", false);
		int combinedDelay = 0;
		
		ArrayList<Material> whitelist = MiscBukkit.parseMaterialList(node.getStringList("whitelist", new ArrayList<String>()));
		
		ArrayList<Block> blocks = new ArrayList<Block>();
		ArrayList<Integer> delays = new ArrayList<Integer>();
		int lastDelay = 0;
		
		for (int i=0;i<blockArray.size();i++) {
			ArrayList<Block> part = blockArray.get(i);
			
			if (cascadeParts) {
				combinedDelay += partDelay;
			} else {
				combinedDelay = 0;
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
		String revertMode = node.getString("revert-mode");
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
					SRPG.cascadeQueueScheduler.scheduleTemporaryBlockChange(activeBlock, material, activeDelay, (int) (activeRevertDelay + factor*(lastDelay - activeDelay)), node.getBoolean("protect", false));
				} else {
					SRPG.cascadeQueueScheduler.scheduleBlockChange(activeBlock, material, activeDelay);
				}
			}
		}
	}

	public static void impulse(ProfileNPC profile, Location location, ConfigurationNode node, EffectDescriptor descriptor) {
		if (location == null) {
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
		
		SRPG.dout(""+yaw+" "+pitch+" "+force,"effects");
		
		if (node.getBoolean("relative", false)) {
			yaw += location.getYaw();
			pitch += location.getPitch(); 
		}
		
		v.setX(- Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)) );
		v.setY(Math.sin(Math.toRadians(pitch)));
		v.setZ(Math.cos(Math.toRadians(pitch))* Math.cos(Math.toRadians(yaw)) );
		v.multiply(force);
			
		if (node.getBoolean("add", false)) {
			v = v.add(profile.entity.getVelocity());
		}
		
		SRPG.dout(profile.entity.getVelocity().toString(),"effects");
		profile.entity.setVelocity(v);
		SRPG.dout(v.length()+"","effects");
		SRPG.dout(profile.entity.getVelocity().toString(),"effects");
	}
	
	public static void lightning(Block block, ConfigurationNode node, EffectDescriptor descriptor) {
		Location location = block.getLocation();
		if (node.getBoolean("damaging", false)) {
			location.getWorld().strikeLightning(location);
		} else {
			location.getWorld().strikeLightningEffect(location);
		}
	}

	public static void changeBlockDrops(BlockBreakEvent event, Block block, ConfigurationNode node) {
		ArrayList<ItemStack> defaults = new ArrayList<ItemStack>();
		defaults.add(MiscBukkit.getNaturalDrops(block));
		if (changeDrops(node,defaults,block.getLocation())) {
			event.setCancelled(true);
			block.setType(Material.AIR);
		}
	}
	
	public static void changeEntityDrops(EntityDeathEvent event, LivingEntity entity, ConfigurationNode node) {
		ArrayList<ItemStack> defaults = new ArrayList<ItemStack>();
		defaults.addAll(MiscBukkit.getNaturalDrops(entity));
		if (changeDrops(node,defaults,entity.getLocation())) {
			event.getDrops().clear();
		}
	}
	
	public static boolean changeDrops(ConfigurationNode node, ArrayList<ItemStack> defaults, Location location) {
		ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
		
		String mode = node.getString("mode");
		if (mode == null || mode.equalsIgnoreCase("multiply") || mode.equalsIgnoreCase("add")) {
			for (ItemStack drop : defaults) {
				drop.setAmount(drop.getAmount() * (1-node.getInt("factor", 1)));
				drops.add(drop);
			}
		}
		if (mode.equalsIgnoreCase("add") || mode.equalsIgnoreCase("replace")) {
			ArrayList<Material> materials = MiscBukkit.parseMaterialList(node.getStringList("items", new ArrayList<String>()));
			ArrayList<Integer> amounts = (ArrayList<Integer>) node.getIntList("amounts", new ArrayList<Integer>());
			if (materials.size() == amounts.size()) {
				for (int i = 0; i<materials.size();i++) {
					ItemStack drop = new ItemStack(materials.get(i),amounts.get(i) * node.getInt("factor", 1));
					drops.add(drop);
				}
			} else {
				SRPG.output("Error during drop change effect handling (different sizes for items/amounts), check your skill config for incorrect material names or wrong lists");
				return false;
			}
		}
		
		if (!node.getBoolean("all", true)) {
			ItemStack choice = drops.get(SRPG.generator.nextInt(drops.size()));
			drops.clear();
			drops.add(choice);
		}
		
		for (ItemStack item : drops) {
			if (item.getAmount() > 0) {
				location.getWorld().dropItemNaturally(location, item);
			}
		}
		
		if (mode.equalsIgnoreCase("replace")) {
			return true;
		}
		return false;
		
	}
}
