package com.behindthemirrors.minecraft.sRPG;

public class EffectDescriptor {
	
	Integer level;
	Integer maxlevel;
	Integer duration;
	Integer potency = 1;
	
	public EffectDescriptor () {
		level = 1;
		maxlevel = 1;
	}
	
	public EffectDescriptor (String signature) {
		this();
		this.potency = Utility.parsePotency(signature);
	}
	
	public EffectDescriptor (Integer duration) {
		level = 1;
		maxlevel = 1;
		this.duration = duration;
	}
	
	public EffectDescriptor(String signature, Integer level, Integer maxlevel) {
		this(level, maxlevel);
		this.potency = Utility.parsePotency(signature);
	}
	
	public EffectDescriptor (Integer level, Integer maxlevel) {
		this.level = level;
		this.maxlevel = maxlevel;
	}
	
	public EffectDescriptor (Integer level, Integer maxlevel, Integer duration) {
		this(level,maxlevel);
		this.duration = duration;
	}
	
	public EffectDescriptor (ProfilePlayer profile, StructureJob job) {
		
	}
	
	public EffectDescriptor (ProfilePlayer profile, StructureJob job, Integer duration) {
		this(profile,job);
		this.duration = duration;
	}

}
