package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.Messager;
import com.behindthemirrors.minecraft.sRPG.ResolverActive;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;



public class ProfilePlayer extends ProfileNPC {
	
	// debug switch for player stuff
	public static boolean debug = false;
	
	public boolean suppressMessages = true;
	public boolean suppressRecalculation = true;
	
	// TODO: maybe change to read directly from config
	public static Integer chargeMax;
	public static Integer chargeTicks;
	
	public HashMap<StructureJob,Integer> jobXP;
	public HashMap<StructureJob,Boolean> jobAvailability;
	public HashMap<StructureActive,EffectDescriptor> actives = new HashMap<StructureActive, EffectDescriptor>();
	public ArrayList<StructureActive> validActives = new ArrayList<StructureActive>();
	public StructureActive currentActive;
	
	public HashMap<StructurePassive,EffectDescriptor> customizedPassives = new HashMap<StructurePassive, EffectDescriptor>();
	public HashMap<StructureActive,EffectDescriptor> customizedActives = new HashMap<StructureActive, EffectDescriptor>();
	
	public Integer id = 0;
	public Player player;
	public String name;
	public Integer hp;
	public Integer hp_max;
	public Integer charges;
	public Integer chargeProgress;
	
	public boolean prepared = false;
	private long abilityReadiedTimeStamp;
	public long abilityActivatedTimeStamp;
	
	public long sneakTimeStamp;
	
	public String locale;
	
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
		for (StructureJob otherJob : Settings.jobs.values()) {
			boolean previous = jobAvailability.containsKey(otherJob) ? jobAvailability.get(otherJob) : false;
			boolean now = otherJob.prerequisitesMet(this);
			if (!previous && now) {
				Messager.sendMessage(player, "job-unlocked",otherJob.signature);
			}
			jobAvailability.put(otherJob, now);
		}
		if (levelup) {
			if (!suppressMessages) {
				Messager.sendMessage(player, "levelup",currentJob.signature);
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
				Messager.sendMessage(player, "charge-acquired");
			}
		}
	}
	
	public boolean cycleActive() {
		if (!validActives.isEmpty()) {
			SRPG.output("valid actives available");
			int index = 0;
			boolean changed = true;
			if (prepared && ((System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500)) {
				index = (validActives.indexOf(currentActive) + 1)%validActives.size();
			} else if (validActives.contains(currentActive)) {
				index = validActives.indexOf(currentActive);
				changed = false;
			}
			currentActive = validActives.get(index);
			Messager.chargeDisplay(player, changed);
			SRPG.output("set current ability to "+currentActive.toString());
			return true;
		}
		return false;
	}
	
	public void prepare() {
		SRPG.output("trying to prepare");
		if (cycleActive()) {
			abilityReadiedTimeStamp = System.currentTimeMillis();
			prepared = true;
		}
	}
	
	// TODO: add cost deduction
	public void activate(CombatInstance combat, Material target) {
		SRPG.output("trying to activate combat ability");
		if (prepared && currentActive.combat && 
				(currentActive.versusMaterials.contains(target) || currentActive.versusMaterials.isEmpty()) &&
				(System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500 && 
				charges >= currentActive.cost) {
			ResolverActive.resolve(currentActive, combat, actives.get(currentActive));
		}
		if (currentActive.combat) {
			prepared = false;
		}
	}
	
	public boolean activate() {
		Block target = player.getTargetBlock(null, currentActive.range);
		boolean result = false;
		if (prepared && !currentActive.combat && 
				(currentActive.versusMaterials.contains(target.getType()) || currentActive.versusMaterials.isEmpty()) &&
				(System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500 && 
				charges >= currentActive.cost) {
			ResolverActive.resolve(currentActive, this, target, actives.get(currentActive));
			result = true;
		}
		if (!currentActive.combat) {
			prepared = false;
		}
		return result;
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
	
	public void recalculateActives() {
		actives.clear();
		if (currentJob != null) {
			int level = jobLevels.get(currentJob);
			for (Map.Entry<StructureActive,EffectDescriptor> entry : currentJob.getActives(level).entrySet()) {
				actives.put(entry.getKey(), entry.getValue().copy(level));
			}
		}
		validateActives();
	}
	
	public void validateActives(Material material) {
		SRPG.output("recalculating actives for "+material.toString());
		validActives.clear();
		for (StructureActive active : actives.keySet()) {
			if (active.validMaterials.isEmpty() || active.validMaterials.contains(material)) {
				validActives.add(active);
			}
		}
		SRPG.output(validActives.toString());
	}
	
	public void validateActives() {
		validateActives(player.getItemInHand().getType());
	}
	
	public void recalculate() {
		if (suppressRecalculation) {
			return;
		}
		
		ArrayList<HashMap<StructurePassive,EffectDescriptor>> traits = new ArrayList<HashMap<StructurePassive,EffectDescriptor>>();
		for (int i=0;i<3;i++) {
			traits.add(new HashMap<StructurePassive, EffectDescriptor>());
		}
		
		// add inherited passives
		ArrayList<StructureJob> parents = currentJob.getParents();
		// TODO: put the current job level into the descriptors
		for (StructureJob job : jobLevels.keySet()) {
			int level = jobLevels.get(job);
			int relation = -1;
			if (parents.contains(job)) {
				if (level >= job.maximumLevel) {
					relation = 0;
				} else {
					relation = 1;
				}
			} else if (level >= job.maximumLevel) {
				relation = 2;
			}
			if (relation >= 0) {
				for (Map.Entry<StructurePassive, EffectDescriptor> entry : job.traits.entrySet()) {
					EffectDescriptor descriptor = entry.getValue().copy(level);
					traits.get(relation).put(entry.getKey(), descriptor);
				}
			}
		}
		// TODO: add player-customized passives
		for (int i = 0;i<1;i++) {
			//current.addAll(something);
		}
		// build stats hashmap
		super.recalculate();
		for (int i=0;i<3;i++) {
			addCollection(traits.get(i),i+1);
		}
		recalculateActives();
		
		SRPG.output("stats: "+stats.toString());
	}

}
