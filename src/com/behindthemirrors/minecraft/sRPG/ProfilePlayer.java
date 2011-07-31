package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;


public class ProfilePlayer extends ProfileNPC {
	
	// debug switch for player stuff
	public static boolean debug = false;
	
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
		// TODO: maybe move the permission check before the actual xp calculations
		if (!player.hasPermission("srpg.xp")) {
			return;
		}
		if (jobLevels.get(currentJob) >= currentJob.maximumLevel) {
			return;
		}
		
		jobXP.put(currentJob, jobXP.get(currentJob) + amount);
		checkLevelUp(currentJob);
		
		recalculate();
		MessageParser.sendMessage(player, "levelup");
		// debug message
		if (debug) {
			SRPG.output("adding "+amount.toString()+" xp to player "+name);
		}
	}
	
	public void checkLevelUp(StructureJob job) {
		Integer currentLevel = jobLevels.containsKey(job) ? jobLevels.get(job) : 0;
		Integer amount = jobXP.get(job); 
		// check for job levelup
		int i = 0;
		boolean done = false;
		while (!done) {
			SRPG.output("level up check iteration "+i);
			SRPG.output("comparing "+amount+" to "+job.xpToNextLevel(currentLevel)+" for current level "+currentLevel);
			if (amount >= job.xpToNextLevel(currentLevel)) {
				if (currentLevel+1 >= job.maximumLevel) {
					jobXP.put(job, job.xpToNextLevel(currentLevel));
					done = true;
				}
				jobLevels.put(job,currentLevel + 1);
				if (amount <= 0) {
					done = true;
				}
			} else {
				done = true;
			}
			// debug
			i++;
			if (i > 10) {
				break;
			}
		}
		
	}
	
	public void addChargeTick(String skillname) {
		chargeProgress++;
		if (chargeProgress >= chargeTicks) {
			if (charges < chargeMax) {
				chargeProgress -= chargeTicks;
				charges++;
			} else {
				chargeProgress--;
			}
			MessageParser.sendMessage(player, "charge-acquired");
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
				jobLevels.put(job,0);
			}
			SRPG.output("changing job");
			currentJob = job;
			checkLevelUp(job);
			recalculate();
		}
	}
	
	void setAdditionalActive(StructureActive active) {
		
	}
	
	void setAdditionalPassive(StructurePassive passive) {
		
	}
	
	void recalculate() {
		SRPG.output(jobXP.toString());
		SRPG.output(jobLevels.toString());
		SRPG.output(currentJob.toString());
	}

}
