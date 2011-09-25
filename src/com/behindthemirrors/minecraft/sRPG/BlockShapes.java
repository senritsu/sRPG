package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockShapes {
	
	public static ArrayList<ArrayList<Block>> line(Block block, BlockFace direction, int length) {
		ArrayList<ArrayList<Block>> blocks = new ArrayList<ArrayList<Block>>();
		blocks.add(new ArrayList<Block>());
		for (int i=0;i<length;i++) {
			block = block.getRelative(direction);
			blocks.get(0).add(block);
		}
		return blocks;
	}
	
	public static ArrayList<ArrayList<Block>> cross2D(Block block, BlockFace normal, int length) {
		ArrayList<BlockFace> ignore = new ArrayList<BlockFace>();
		ignore.add(normal);
		ignore.add(MiscGeometric.invert(normal));
		return cross3D(block, ignore, length);
	}
	
	public static ArrayList<ArrayList<Block>> cross3D(Block block, int length) {
		return cross3D(block, new ArrayList<BlockFace>(), length);
	}
	
	public static ArrayList<ArrayList<Block>> cross3D(Block block, ArrayList<BlockFace> ignore, int length) {
		ArrayList<ArrayList<Block>> blocks = new ArrayList<ArrayList<Block>>();
		for (BlockFace face : new BlockFace[] {BlockFace.UP,BlockFace.EAST,BlockFace.SOUTH,BlockFace.WEST,BlockFace.NORTH,BlockFace.DOWN}) {
			if (ignore.contains(face)) {
				continue;
			}
			blocks.add(new ArrayList<Block>());
			Block current = block;
			for (int i = 0;i<length;i++) {
				current = current.getRelative(face);
				blocks.get(blocks.size()-1).add(current);
			}
		}
		return blocks;
	}
	
	public static ArrayList<ArrayList<Block>> circle(Block block, BlockFace normal, int radius) {
		ArrayList<BlockFace> ignore = new ArrayList<BlockFace>();
		ignore.add(normal);
		ignore.add(MiscGeometric.invert(normal));
		return sphere(block, ignore, radius);
	}
	
	public static ArrayList<ArrayList<Block>> sphere(Block block, ArrayList<BlockFace> ignore, int radius) {
		ArrayList<ArrayList<Block>> blocks = new ArrayList<ArrayList<Block>>();
		radius += 1;
		for (int i=0;i<= radius;i++) {
			blocks.add(new ArrayList<Block>());
		}
		for (int x=-radius;x<=radius;x++) {
			for (int y=-radius;y<=radius;y++) {
				for (int z=-radius;z<=radius;z++) {
					Block currentBlock = block.getRelative(x, y, z);
					double distance = block.getLocation().distance(currentBlock.getLocation());
					if (distance <= radius-0.5 && !(
							x < 0 && ignore.contains(BlockFace.NORTH) ||
							x > 0 && ignore.contains(BlockFace.SOUTH) ||
							y < 0 && ignore.contains(BlockFace.DOWN) ||
							y > 0 && ignore.contains(BlockFace.UP) ||
							z < 0 && ignore.contains(BlockFace.EAST) ||
							z > 0 && ignore.contains(BlockFace.WEST))) {
						blocks.get((int)Math.round(distance)).add(currentBlock);
					}
				}
			}
		}
		return blocks;
	}
	
}
