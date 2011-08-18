package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockShapes {
	
	public static ArrayList<ArrayList<Block>> line(Block block, BlockFace direction, int length) {
		SRPG.output("line: "+block.toString()+" > "+direction.toString()+ " > "+length);
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
}
