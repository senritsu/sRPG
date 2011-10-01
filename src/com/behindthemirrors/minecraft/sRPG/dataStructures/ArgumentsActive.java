package com.behindthemirrors.minecraft.sRPG.dataStructures;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.Settings;

public class ArgumentsActive {
	
	public StructureActive active;
	public EffectDescriptor descriptor;
	public ProfileNPC source;
	public ProfileNPC target;
	public Block sourceBlock;
	public Block targetBlock;
	public Location location;
	
	public CombatInstance combat;
	
	public ArgumentsActive(String name, ProfileNPC source, EffectDescriptor descriptor) {
		this(Settings.actives.get(name),source,descriptor);
	}
	
	public ArgumentsActive(StructureActive active, ProfileNPC source, EffectDescriptor descriptor) {
		this.active = active;
		this.source = source;
		this.descriptor = descriptor;
	}
	
	public void complete() {
		if (sourceBlock == null) {
			sourceBlock = source.blockStandingOn();
		}
		if (targetBlock == null) {
			if (target != null) {
				targetBlock = target.blockStandingOn();
			} else {
				targetBlock = sourceBlock;
			}
		}
		if (location == null) {
			if (target != null) {
				location = target.entity.getLocation();
			} else {
				location = source.entity.getLocation();
			}
		}
	}
}
