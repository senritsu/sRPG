package com.behindthemirrors.minecraft.sRPG.dataStructures;

import org.bukkit.block.BlockState;

public class BlockChangeDescriptor {
	public BlockState targetState;
	public int ticksToChange;
	public boolean revert = false;
	public int ticksToRevert;
	public ProfilePlayer cause;
	
	public boolean drop = false;
	public boolean protect = false;
	public boolean force = true;
	
	public BlockChangeDescriptor (BlockState state, int delay, ProfilePlayer profile) {
		this(state, delay);
		cause = profile;
		drop = true;
	}
	
	public BlockChangeDescriptor (BlockState state, int delay) {
		targetState = state;
		ticksToChange = delay;
	}
	
	public BlockChangeDescriptor (BlockState state, int delay, int revertDelay, ProfilePlayer profile) {
		this(state, delay, revertDelay);
		cause = profile;
		drop = true;
	}
	
	public BlockChangeDescriptor (BlockState state, int delay, int revertDelay) {
		this(state, delay);
		revert = true;
		protect = true;
		ticksToRevert = revertDelay;
	}
}
