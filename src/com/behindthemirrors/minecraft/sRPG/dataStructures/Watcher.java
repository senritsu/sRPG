package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;

public class Watcher {
	public static ArrayList<Material> trackingMaterials = new ArrayList<Material>();
	public static ArrayList<Block> userPlacedBlocks = new ArrayList<Block>();
	
	public static ArrayList<Block> protectedBlocks = new ArrayList<Block>();
	public static ArrayList<Item> protectedItems = new ArrayList<Item>();
	
	static HashMap<Block,Integer> blockTimers = new HashMap<Block, Integer>();
	static HashMap<Item,Integer> itemTimers = new HashMap<Item, Integer>();
	
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
	}
	
}
