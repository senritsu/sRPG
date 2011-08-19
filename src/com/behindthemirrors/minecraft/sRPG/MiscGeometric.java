package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

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
		directionToFacing.put("forward", BlockFace.EAST);
		directionToFacing.put("right", BlockFace.SOUTH);
		directionToFacing.put("back", BlockFace.WEST);
		directionToFacing.put("left", BlockFace.NORTH);
		directionToFacing.put("up", BlockFace.UP);
		directionToFacing.put("down", BlockFace.DOWN);
	}
	
	public static BlockFace getEntityFacing(LivingEntity entity) {
		return MiscGeometric.getEntityFacing(entity, false);
	}

	public static BlockFace getEntityFacing(LivingEntity entity, boolean diagonals) {
		if (diagonals) {
			return null;
		} else {
			return MiscGeometric.angleToCardinalFace(entity.getLocation());
		}
	}
	
	public static BlockFace angleToCardinalFace(Location location) {
		double pitch = location.getPitch();
		BlockFace facing = null;
		SRPG.output("pitch: "+pitch);
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
	
	public static BlockFace relativeFacing(String direction, LivingEntity entity) {
		return relativeFacing(directionToFacing.get(direction), getEntityFacing(entity));
	}
	
	public static BlockFace relativeFacing(BlockFace facing, BlockFace relativeTo) {
		SRPG.output("trying to get relative facing: "+facing.toString()+" > "+relativeTo.toString());
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
