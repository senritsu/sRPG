package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;


public class ProfilePlayer extends ProfileNPC {
	
	// debug switch for player stuff
	public static boolean debug = false;
	
	boolean suppressMessages = true;
	
	// TODO: maybe change to read directly from config
	static Integer chargeMax;
	static Integer chargeTicks;
	
	HashMap<StructureJob,Integer> jobLevels;
	HashMap<StructureJob,Integer> jobXP;
	StructureJob currentJob;
	StructureActive currentActive;
	
	Integer id = 0;
	Player player;
	String name;
	Integer hp;
	Integer hp_max;
	Integer charges;
	Integer chargeProgress;
	
	boolean prepared = false;
	private long abilityReadiedTimeStamp;
	
	long sneakTimeStamp;
	
	String locale;
	
	public void addXP(Integer amount) {
		if (jobLevels.get(currentJob) >= currentJob.maximumLevel && !(amount < 0)) {
			return;
		}
		
		jobXP.put(currentJob, jobXP.get(currentJob) + amount);
		checkLevelUp(currentJob);
		
		// debug message
		if (debug) {
			SRPG.output("adding "+amount.toString()+" xp to player "+name);
		}
		SRPG.profileManager.save(this,"xp");
	}
	
	public boolean checkLevelUp(StructureJob job) {
		Integer currentLevel = jobLevels.containsKey(job) ? jobLevels.get(job) : 0;
		Integer amount = jobXP.get(job); 
		// check for job levelup
		boolean done = false;
		boolean levelup = false;
		while (!done) {
			if (amount >= job.xpToNextLevel(currentLevel) && currentLevel < job.maximumLevel) {
				if (currentLevel+1 >= job.maximumLevel) {
					jobXP.put(job, job.xpToNextLevel(currentLevel));
				}
				currentLevel++;
				jobLevels.put(job,currentLevel);
				levelup = true;
			} else {
				break;
			}
		}
		if (levelup) {
			if (!suppressMessages) {
				MessageParser.sendMessage(player, "levelup",currentJob.signature);
			}
			recalculate();
		}
		return levelup;
		
	}
	
	public void addChargeTick() {
		chargeProgress++;
		if (chargeProgress >= chargeTicks) {
			if (charges < chargeMax) {
				chargeProgress -= chargeTicks;
				charges++;
			} else {
				chargeProgress--;
			}
			if (!suppressMessages) {
				MessageParser.sendMessage(player, "charge-acquired");
			}
		}
	}
	
	public boolean prepare() {
		if (currentActive != null ) {
			prepared = true;
			abilityReadiedTimeStamp = System.currentTimeMillis();
		} else {
			prepared = false;
		}
		return prepared;
	}
	
	public void activate(Material material) {
		if (prepared && (System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500 && charges >= currentActive.cost) {
			if (currentActive.activate(player,material)) {
				charges -= currentActive.cost;
			}
		}
		prepared = false;
	}

	public void changeJob(StructureJob job) {
		SRPG.output("checking prerequisites");
		if (job.prerequisitesMet(this)) {
			if (!jobLevels.containsKey(job)) {
				jobLevels.put(job,1);
			}
			SRPG.output("changing job to "+job.name);
			currentJob = job;
			if (!checkLevelUp(job)) {
				recalculate();
			}
			SRPG.profileManager.save(this,"job");
		}
	}
	
	void setAdditionalActive(StructureActive active) {
		
	}
	
	void setAdditionalPassive(StructurePassive passive) {
		
	}
	
	public void recalculate() {
		HashMap<StructurePassive,EffectDescriptor> current = new HashMap<StructurePassive, EffectDescriptor>();
		HashMap<StructurePassive,EffectDescriptor> mastered = new HashMap<StructurePassive, EffectDescriptor>();
		HashMap<StructurePassive,EffectDescriptor> inherited = new HashMap<StructurePassive, EffectDescriptor>();
		HashMap<StructurePassive,EffectDescriptor> both = new HashMap<StructurePassive, EffectDescriptor>();
		// add current job
		current.putAll(currentJob.getPassives(jobLevels.get(currentJob)));
		current.putAll(currentJob.traits);
		// add inherited passives
		ArrayList<StructureJob> parents = currentJob.getParents();
		for (StructureJob job : jobLevels.keySet()) {
			if (job == currentJob || jobLevels.get(job) == 0) {
				continue;
			} else if (parents.contains(job)) {
				if (jobLevels.get(job) >= job.maximumLevel) {
					both.putAll(job.traits);
				} else {
					inherited.putAll(job.traits);
				}
			} else if (jobLevels.get(job) >= job.maximumLevel) {
				mastered.putAll(job.traits);
			}
		}
		// TODO: add player-customized passives
		for (int i = 0;i<1;i++) {
			//current.addAll(something);
		}
		// build stats hashmap
		stats.clear();
		stats.put(null, new HashMap<Material, HashMap<String,Double>>());
		stats.get(null).put(null, new HashMap<String, Double>());
		addCollection(current);
		addCollection(mastered,3);
		addCollection(inherited,2);
		addCollection(both,1);
		super.recalculateBuffs();
		SRPG.output("stats: "+stats.toString());
	}

}
