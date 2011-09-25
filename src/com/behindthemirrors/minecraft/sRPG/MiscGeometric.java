package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.config.ConfigurationNode;

public class MiscGeometric {

	static ArrayList<BlockFace> orderedFaces = new ArrayList<BlockFace>();
	
	static {
		orderedFaces.addAll(Arrays.asList(new BlockFace[] {BlockFace.EAST,BlockFace.EAST_SOUTH_EAST,BlockFace.SOUTH_EAST,BlockFace.SOUTH_SOUTH_EAST,
				BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST,BlockFace.SOUTH_WEST,BlockFace.WEST_SOUTH_WEST,
				BlockFace.WEST,BlockFace.WEST_NORTH_WEST,BlockFace.NORTH_WEST,BlockFace.NORTH_NORTH_WEST,
				BlockFace.NORTH,BlockFace.NORTH_NORTH_EAST,BlockFace.NORTH_EAST,BlockFace.EAST_NORTH_EAST}));
	}
	
	public static HashMap<BlockFace,BlockFace> cardinalInversions = new HashMap<BlockFace, BlockFace>();
	
	static {
		cardinalInversions.put(BlockFace.UP, BlockFace.DOWN);
		cardinalInversions.put(BlockFace.DOWN, BlockFace.UP);
		cardinalInversions.put(BlockFace.EAST, BlockFace.WEST);
		cardinalInversions.put(BlockFace.SOUTH, BlockFace.NORTH);
		cardinalInversions.put(BlockFace.WEST, BlockFace.EAST);
		cardinalInversions.put(BlockFace.NORTH, BlockFace.SOUTH);
	}
	
	static HashMap<String,BlockFace> directionToFacing = new HashMap<String, BlockFace>();
	
	static {
		directionToFacing.put("forward", BlockFace.EAST); // +z
		directionToFacing.put("right", BlockFace.SOUTH); // -x
		directionToFacing.put("back", BlockFace.WEST); // -z
		directionToFacing.put("left", BlockFace.NORTH); // +x
		directionToFacing.put("up", BlockFace.UP); // +y
		directionToFacing.put("down", BlockFace.DOWN); // -y
	}
	
	public static Block offset(Location location, Block block, ConfigurationNode node) {
		try {
			ArrayList<Integer> offset = (ArrayList<Integer>)node.getIntList("offset", new ArrayList<Integer>());
			if (location != null && node.getBoolean("relative", false)) {
				offset = relativeOffset(offset, getFacing(location));
			}
			return block.getRelative(offset.get(0),offset.get(1) , offset.get(2));
		} catch (IndexOutOfBoundsException ex) {
		}
		return block;
	}
	
	public static BlockFace getFacing(Location location) {
		return MiscGeometric.getFacing(location, false);
	}

	public static BlockFace getFacing(Location location, boolean diagonals) {
		if (diagonals) {
			return null;
		} else {
			return MiscGeometric.angleToCardinalFace(location);
		}
	}
	
	public static BlockFace angleToCardinalFace(Location location) {
		double pitch = location.getPitch();
		BlockFace facing = null;
		if (pitch <= -30) {
			facing = BlockFace.UP;
		} else if (pitch >= 60) {
			facing = BlockFace.DOWN;
		} else {
			double yaw = location.getYaw() % 360;
			if (yaw < 0) {
				yaw += 360.0;
			} 
			if (yaw <= 45) {
				facing = BlockFace.WEST;
			} else if (yaw <= 135) {
				facing = BlockFace.NORTH;
			} else if (yaw <= 225) {
				facing = BlockFace.EAST;
			} else if (yaw <= 315) {
				facing = BlockFace.SOUTH;
			} else {
				facing = BlockFace.WEST;
			}
		}
		return facing;
	}
	
	public static ArrayList<Integer> relativeOffset (ArrayList<Integer> offset, BlockFace facing) {
		ArrayList<Integer> newOffset = new ArrayList<Integer>();
		if (facing == BlockFace.SOUTH) {
			newOffset.add(-offset.get(2));
			newOffset.add(offset.get(1));
			newOffset.add(offset.get(0));
		} else if (facing == BlockFace.WEST) {
			newOffset.add(-offset.get(0));
			newOffset.add(offset.get(1));
			newOffset.add(-offset.get(2));
		} else if (facing == BlockFace.NORTH) {
			newOffset.add(offset.get(2));
			newOffset.add(offset.get(1));
			newOffset.add(-offset.get(0));
		} else {
			newOffset = offset;
		}
		return newOffset;
	}
	
	// TODO: add proper handling for looking up/down with 3d structures
	
	public static BlockFace relativeFacing(String direction, Location location) {
		return relativeFacing(directionToFacing.get(direction), getFacing(location));
	}
	
	public static BlockFace relativeFacing(BlockFace facing, BlockFace relativeTo) {
		if (relativeTo == BlockFace.UP || relativeTo == BlockFace.DOWN) {
			return relativeTo;
		} else if (!orderedFaces.contains(facing) || !orderedFaces.contains(relativeTo)) {
			return facing;
		}
		return orderedFaces.get((orderedFaces.indexOf(facing)+orderedFaces.indexOf(relativeTo))%orderedFaces.size());
	}
	
	public static BlockFace invert(BlockFace facing) {
		return cardinalInversions.get(facing);
	}
	
}
