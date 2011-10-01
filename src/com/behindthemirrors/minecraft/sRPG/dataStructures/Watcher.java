package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.ResolverPassive;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;

public class Watcher {
	public static ArrayList<Material> blocksToWatch = new ArrayList<Material>();
	public static ArrayList<Block> watchedBlocks = new ArrayList<Block>();
	public static boolean ignoreWatchedBlocks;
	
	public static ArrayList<Block> protectedBlocks = new ArrayList<Block>();
	public static ArrayList<Item> protectedItems = new ArrayList<Item>();
	
	static HashMap<Block,Integer> blockTimers = new HashMap<Block, Integer>();
	static HashMap<Item,Integer> itemTimers = new HashMap<Item, Integer>();
	
	static HashMap<World,Long> previousTime = new HashMap<World, Long>();
	static ArrayList<HashMap<ProfileNPC,ArrayList<TriggerEffect>>> registeredTriggerEffects = new ArrayList<HashMap<ProfileNPC,ArrayList<TriggerEffect>>>();
	
	static {
		registeredTriggerEffects.add(new HashMap<ProfileNPC, ArrayList<TriggerEffect>>());
		registeredTriggerEffects.add(new HashMap<ProfileNPC, ArrayList<TriggerEffect>>());
	}
	
	public static void addWorlds(List<World> worlds) {
		for (World world : worlds) {
			if (!Settings.worldBlacklist.contains(world)) {
				previousTime.put(world, world.getTime());
			}
		}
	}
	
	public static boolean protect(Block block) {
		if (!protectedBlocks.contains(block)) {
			protectedBlocks.add(block);
			return true;
		}
		return false;
	}
	
	public static boolean protect(Block block, int duration) {
		if (duration > 0) {
			if (protect(block)) {
				blockTimers.put(block, duration);
				return true;
			}
			return false;
		}
		return true;
	}
	
	public static boolean isProtected(Block block) {
		if (protectedBlocks.contains(block)) {
			return true;
		}
		return false;
	}
	
	public static void release(Block block) {
		protectedBlocks.remove(block);
		blockTimers.remove(block);
	}
	
	public static boolean protect(Item item) {
		if (!protectedItems.contains(item)) {
			protectedItems.add(item);
			return true;
		}
		return false;
	}
	
	public static boolean protect(Item item, int duration) {
		if (duration > 0) {
			if (protect(item)) {
				itemTimers.put(item, duration);
				return true;
			}
			return false;
		}
		return true;
	}
	
	public static boolean isProtected(Item item) {
		if (protectedItems.contains(item)) {
			return true;
		}
		return false;
	}
	
	public static void release(Item item) {
		protectedItems.remove(item);
		itemTimers.remove(item);
	}
	
	public static void tick() {
		Iterator<Entry<Item,Integer>> itemIterator = itemTimers.entrySet().iterator();
		while (itemIterator.hasNext()) {
			Entry<Item,Integer> entry = itemIterator.next();
			entry.setValue(entry.getValue()-1);
			if (entry.getValue() < 0) {
				itemIterator.remove();
				protectedItems.remove(entry.getKey());
			}
		}
		Iterator<Entry<Block,Integer>> blockIterator = blockTimers.entrySet().iterator();
		while (blockIterator.hasNext()) {
			Entry<Block,Integer> entry = blockIterator.next();
			entry.setValue(entry.getValue()-1);
			if (entry.getValue() < 0) {
				blockIterator.remove();
				protectedBlocks.remove(entry.getKey());
			}
		}
		checkTimedTriggers();
	}
	
	public static void register(ProfileNPC profile, TriggerEffect effect, Integer index) {
		if (!registeredTriggerEffects.get(index).containsKey(profile)) {
			registeredTriggerEffects.get(index).put(profile, new ArrayList<TriggerEffect>());
		}
		registeredTriggerEffects.get(index).get(profile).add(effect);
	}

	
	public void clear(ProfileNPC profile, Integer index) {
		if (registeredTriggerEffects.get(index).containsKey(profile)) {
			registeredTriggerEffects.get(index).remove(profile);
		}
	}
	
	public static void checkTriggers(ProfileNPC profile,ArrayList<String> triggers) {
		checkTriggers(profile,triggers, null, null, null);
	}
	
	public static void checkTriggers(ProfileNPC profile,ArrayList<String> triggers, ProfileNPC target, CombatInstance combat) {
		checkTriggers(profile,triggers, target, null, combat);
	}
	
	public static void checkTriggers(ProfileNPC profile,ArrayList<String> triggers, Block block) {
		checkTriggers(profile,triggers, null, block, null);
	}
	
	public static void checkTriggers(ProfileNPC profile,ArrayList<String> triggers, ProfileNPC target, Block block, CombatInstance combat) {
		for (HashMap<ProfileNPC,ArrayList<TriggerEffect>> effectMap : registeredTriggerEffects) {
			if (effectMap.containsKey(profile)) {
				checkOne(profile,effectMap.get(profile),triggers,target, block, combat);
			}
		}
	}
	
	public static void checkTimedTriggers() {
		for (World world : SRPG.plugin.getServer().getWorlds()) {
			ArrayList<String> triggers = new ArrayList<String>();
			if (!Settings.worldBlacklist.contains(world)) {
				long previous = previousTime.get(world);
				long time = world.getTime();
				if (time > 23000) {
					time -= 24000;
				}
				if (previous < 0 && time > 0) {
					triggers.add("daybreak");
				} else if (previous < 13000 && time > 13000) {
					triggers.add("nightfall");
				} else if (previous < 6500 && time > 6500) {
					triggers.add("noon");
				} else if (previous < 18500 && time > 18500) {
					triggers.add("midnight");
				}
				previousTime.put(world, time);
			}
			checkAll(triggers);
		}
	}
	
	public static void checkAll(ArrayList<String> triggers) {
		for (HashMap<ProfileNPC,ArrayList<TriggerEffect>> effectMap : registeredTriggerEffects) {
			for (Entry<ProfileNPC,ArrayList<TriggerEffect>> entry : effectMap.entrySet()) {
				ProfileNPC profile = entry.getKey(); 
				checkOne(profile,entry.getValue(),triggers, null, profile.blockStandingOn(), null);
			}
		}
	}
	
	public static void checkOne(ProfileNPC profile, ArrayList<TriggerEffect> effects, ArrayList<String> triggers, ProfileNPC target, Block block, CombatInstance combat) {
		for (TriggerEffect effect : effects) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.addAll(triggers);
			temp.retainAll(effect.triggers);
			if (!temp.isEmpty()) {
				ResolverPassive.resolve(profile,effect, target, block, combat);
			}
		}
	}
	
	public static void watch(Block block) {
		watchedBlocks.add(block);
		if (watchedBlocks.size() > 1200) {
			watchedBlocks = (ArrayList<Block>) watchedBlocks.subList(200, watchedBlocks.size()-1);
		}
	}
	
	public static boolean givesOk(Block block) {
		if (watchedBlocks.contains(block)) {
			return false;
		} else {
			return true;
		}
	}
	
}
