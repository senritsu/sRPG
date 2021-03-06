package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericGradient;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericTexture;
import org.getspout.spoutapi.gui.RenderPriority;
import org.getspout.spoutapi.gui.WidgetAnchor;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.Messager;
import com.behindthemirrors.minecraft.sRPG.MiscGeneric;
import com.behindthemirrors.minecraft.sRPG.ResolverActive;
import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;



public class ProfilePlayer extends ProfileNPC {
	
	public boolean suppressMessages = true;
	public boolean suppressRecalculation = true;
	
	// TODO: maybe change to read directly from config
	public static Integer chargeMax;
	public static Integer ticksPerCharge;
	
	public HashMap<StructureJob,Integer> jobXP = new HashMap<StructureJob, Integer>();
	public HashMap<StructureJob,Boolean> jobAvailability = new HashMap<StructureJob, Boolean>();
	public HashMap<StructureActive,EffectDescriptor> actives = new HashMap<StructureActive, EffectDescriptor>();
	public ArrayList<StructureActive> validActives = new ArrayList<StructureActive>();
	public StructureActive currentActive;
	
	public int passiveSlots = 0;
	public HashMap<StructurePassive,EffectDescriptor> customizedPassives = new HashMap<StructurePassive, EffectDescriptor>();
	public int activeSlots = 0;
	public HashMap<StructureActive,EffectDescriptor> customizedActives = new HashMap<StructureActive, EffectDescriptor>();
	
	public Integer id = 0;
	public Player player;
	public String name;
	public Integer hp;
	public Integer hp_max;
	public Integer charges;
	public Integer chargeProgress;
	
	public boolean locked = false;
	public boolean prepared = false;
	private long abilityReadiedTimeStamp;
	public long abilityActivatedTimeStamp;
	
	public long sneakTimeStamp;
	
	public String locale;
	
	public GenericGradient xpbar;
	public GenericLabel levelDisplay;
	public ArrayList<GenericTexture> chargeDisplay;
	
	public void addXP(Integer amount) {
		if (jobLevels.get(currentJob) >= currentJob.maximumLevel && !(amount < 0)) {
			return;
		}
		
		jobXP.put(currentJob, jobXP.get(currentJob) + amount);
		checkLevelUp(currentJob);
		
		// debug message
		SRPG.dout("adding "+amount.toString()+" xp to player "+name,"player");
		SRPG.profileManager.save(this,"xp");
	}
	
	public void setXPBar(double percentage) {
		xpbar.setWidth((int)(126*percentage));
		if (percentage >= 1.0) {
			xpbar.setTopColor(new Color(0.85F,0.7F,0.0F,1.0F));
			xpbar.setBottomColor(new Color(0.6F,0.4F,0.0F,1.0F));
		} else {
			xpbar.setTopColor(new Color(0.25F,0.4F,1.0F,1.0F));
			xpbar.setBottomColor(new Color(0.15F,0.325F,0.4F,1.0F));
		}
		xpbar.setDirty(true);
	}
	
	public void setLevelDisplay(int level) {
		if (level >= currentJob.maximumLevel) {
			levelDisplay.setText(ChatColor.GOLD+""+level);
		} else {
			levelDisplay.setText(""+level);
		}
		levelDisplay.setDirty(true);
	}
	
	public boolean checkLevelUp(StructureJob job) {
		Integer oldLevel = jobLevels.containsKey(job) ? jobLevels.get(job) : 0;
		Integer currentLevel = oldLevel;
		Integer amount = jobXP.get(job); 
		// check for job levelup
		boolean done = false;
		boolean levelChanged = false;
		while (!done) {
			if (amount >= job.xpToNextLevel(currentLevel) && currentLevel < job.maximumLevel) {
				if (currentLevel+1 >= job.maximumLevel) {
					jobXP.put(job, job.xpToNextLevel(currentLevel));
				}
				currentLevel++;
				jobLevels.put(job,currentLevel);
				levelChanged = true;
			} else if (currentLevel > 0 && !(amount >= job.xpToNextLevel(currentLevel-1))) {
				currentLevel--;
				jobLevels.put(job,currentLevel);
				levelChanged = true;
			} else {
				break;
			}
		}
		if (levelChanged) {
			if (!suppressMessages) {
				Messager.sendMessage(player, "levelup",currentJob.signature);
				if (oldLevel < currentLevel) {
					SpoutManager.getSoundManager().playCustomSoundEffect(SRPG.plugin, SpoutManager.getPlayer(player), "http://www.behindthemirrors.com/files/minecraft/srpg/level.ogg", false);
				}
			}
			recalculate();
		}
		// set xp bar
		double toLast = currentJob.xpToNextLevel(currentLevel-1);
		double toNext = currentJob.xpToNextLevel(currentLevel) - toLast;
		setXPBar(toNext <= 0 || currentLevel >= job.maximumLevel ? 1.0 : (amount-toLast) / toNext);
		setLevelDisplay(currentLevel);
		// check for unlocked jobs
		for (StructureJob otherJob : Settings.jobs.values()) {
			boolean previous = jobAvailability.containsKey(otherJob) ? jobAvailability.get(otherJob) : false;
			boolean now = otherJob.prerequisitesMet(this);
			if (!previous && now && !suppressMessages) {
				Messager.sendMessage(player, "job-unlocked",otherJob.signature);
			}
			jobAvailability.put(otherJob, now);
		}
		return levelChanged;
		
	}
	
	public void addChargeTicks(int i) {
		if (i <= 0) {
			return;
		}
		chargeProgress += i;
		while (chargeProgress >= ticksPerCharge) {
			if (charges < chargeMax) {
				chargeProgress -= ticksPerCharge;
				charges++;
				updateChargeDisplay();
				if (!suppressMessages) {
					if (SpoutManager.getPlayer(player).isSpoutCraftEnabled()) {
						SpoutManager.getSoundManager().playCustomSoundEffect(SRPG.plugin, SpoutManager.getPlayer(player), "http://www.behindthemirrors.com/files/minecraft/srpg/charge.ogg", false);
					} else {
						Messager.sendMessage(player, "charge-acquired");
					}
					
				}
			} else {
				chargeProgress = ticksPerCharge-1;
			}
		}
	}
	
	public void updateChargeDisplay() {
		GenericTexture texture;
		for (int i = 0;i<10;i++) {
			texture = chargeDisplay.get(i);
			if (i < charges) {
				texture.setVisible(true);
			} else {
				texture.setVisible(false);
			}
			texture.setDirty(true);
		}
	}
	
	public boolean cycleActive() {
		if (!validActives.isEmpty()) {
			int index = 0;
			boolean changed = true;
			if (prepared && ((System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500)) {
				index = (validActives.indexOf(currentActive) + 1)%validActives.size();
			} else if (validActives.contains(currentActive)) {
				index = validActives.indexOf(currentActive);
				changed = false;
			}
			currentActive = validActives.get(index);
			if (SpoutManager.getPlayer(player).isSpoutCraftEnabled()) {
				String name = Messager.localizedActive(currentActive.signature, this)+MiscGeneric.potencyToRoman(actives.get(currentActive).potency);
				if (name.length() >= 26) {
					name = name.substring(0, 23) + "...";
				}
				String header = " ";
				try {
					header = Messager.parseMessage(player, "active-changed-header", "", false).get(0);
				} catch (IndexOutOfBoundsException ex) {
				}
				Material displayMaterial = player.getItemInHand().getType();
				if (displayMaterial == Material.AIR) {
					displayMaterial = Material.STONE; // TODO: remove hackish fix
				}
				SpoutManager.getPlayer(player).sendNotification(header, name, displayMaterial);
			} else {
				Messager.chargeDisplay(player, changed);
			}
			return true;
		}
		return false;
	}
	
	public boolean prepare() {
		if (!locked && cycleActive()) {
			abilityReadiedTimeStamp = System.currentTimeMillis();
			prepared = true;
			return true;
		}
		return false;
	}
	
	// TODO: add cost deduction
	public boolean activate(CombatInstance combat, Material target) {
		if (currentActive == null) {
			return false;
		}
		boolean result = false;
		if (prepared && currentActive.combat && 
				(currentActive.versusMaterials.contains(target) || currentActive.versusMaterials.isEmpty()) &&
				(System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500 && 
				charges >= currentActive.cost) {
			ArgumentsActive arguments = new ArgumentsActive(currentActive, this, actives.get(currentActive));
			arguments.target = this == combat.attacker ? combat.defender : this;
			arguments.combat = combat;
			ResolverActive.resolve(arguments);
			Messager.announce(currentActive, this); 
			result = true;
		}
		if (currentActive.combat) {
			prepared = false;
		}
		return result;
	}
	
	public boolean activate() {
		if (currentActive == null) {
			return false;
		}
		Block targetBlock = player.getTargetBlock(null, currentActive.range);
		List<Block> los = player.getLineOfSight(null, currentActive.range);
		LivingEntity target = null;
		double distance = 0;
		for (Entity entity : player.getNearbyEntities(currentActive.range,currentActive.range,currentActive.range)) {
			if (entity instanceof LivingEntity) {
				if (los.contains(entity.getLocation().getBlock())) {
					double d = entity.getLocation().distance(this.entity.getLocation());
					if (target == null || d < distance) {
						target = (LivingEntity)entity;
						distance = d;
					}
				}
			}
		}
		boolean result = false;
		if (prepared && !currentActive.combat && 
				currentActive.validVs(targetBlock.getType()) &&
				(System.currentTimeMillis() - abilityReadiedTimeStamp) < 1500 && 
				charges >= currentActive.cost) {
			SRPG.dout(actives.toString());
			ArgumentsActive arguments = new ArgumentsActive(currentActive, this, actives.get(currentActive));
			arguments.targetBlock = targetBlock;
			arguments.target = SRPG.profileManager.get(target);
			ResolverActive.resolve(arguments);
			Messager.announce(currentActive, this); 
			result = true;
			// hack
			charges -= currentActive.cost;
		}
		if (!currentActive.combat) {
			prepared = false;
		}
		updateChargeDisplay();
		return result;
	}

	public void changeJob(StructureJob job) {
		SRPG.dout("checking prerequisites for player "+name,"player");
		if (job.prerequisitesMet(this)) {
			if (!jobLevels.containsKey(job)) {
				jobLevels.put(job,1);
			}
			SRPG.dout("changing job to "+job.name,"player");
			currentJob = job;
			if (!checkLevelUp(job)) {
				recalculate();
			}
			SRPG.profileManager.save(this,"job");
		}
	}
	
	void setAdditionalActive(StructureJob job, StructureActive active) {
		if (!(customizedActives.containsKey(active) || customizedActives.size() >= activeSlots)) {
			int level = jobLevels.get(job);
			customizedActives.put(active, job.getActives(level).get(active).copy(level));
		}
		recalculateActives();
	}
	
	void removeAdditionalActive(StructureActive active) {
		customizedActives.remove(active);
		recalculateActives();
	}
	
	void setAdditionalPassive(StructureJob job, StructurePassive passive) {
		if (!(customizedPassives.containsKey(passive) || customizedPassives.size() >= passiveSlots)) {
			int level = jobLevels.get(job);
			customizedPassives.put(passive, job.getActives(level).get(passive).copy(level));
		}
		recalculate();
	}
	
	void removeAdditionalPassive(StructurePassive passive) {
		customizedPassives.remove(passive);
		recalculate();
	}
	
	
	public void recalculateActives() {
		actives.clear();
		if (currentJob != null) {
			int level = jobLevels.get(currentJob);
			for (Map.Entry<StructureActive,EffectDescriptor> entry : currentJob.getActives(level).entrySet()) {
				actives.put(entry.getKey(), entry.getValue().copy(level));
			}
			actives.putAll(customizedActives);
		}
		validateActives();
	}
	
	public void validateActives(Material material) {
		validActives.clear();
		for (StructureActive active : actives.keySet()) {
			if (active.validMaterials.isEmpty() || active.validMaterials.contains(material)) {
				validActives.add(active);
			}
		}
		Collections.sort(validActives);
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
		passives.putAll(customizedPassives);
		addCollection(customizedPassives);
		for (int i=0;i<3;i++) {
			addCollection(traits.get(i),i+1);
		}
		recalculateActives();
		
		SRPG.dout("recalculated stats: "+stats.toString(),"player");
	}
	
	public void initializeHUD() {
		//background
		GenericGradient gradient = new GenericGradient();
		gradient.setTopColor(new Color(0.11F,0.08F,0.066F,1.0F));
		gradient.setBottomColor(new Color(0.11F,0.08F,0.066F,1.0F));
		gradient.setWidth(126).setHeight(2);
		gradient.setX(150).setY(5); // middle x : 213
		gradient.setPriority(RenderPriority.Highest);
		SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,gradient);
		//fill
		gradient = new GenericGradient();
		gradient.setWidth(0).setHeight(2);
		gradient.setX(150).setY(5); // middle x : 213
		gradient.setPriority(RenderPriority.High);
		SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,gradient);
		xpbar = gradient;
		//border
		GenericTexture texture = new GenericTexture();
		texture.setUrl("http://www.behindthemirrors.com/files/minecraft/srpg/xpbar_background.png");
		texture.setWidth(128).setHeight(8);
		texture.setX(149).setY(4); // middle x : 213
		SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,texture);
		//level background/border
		texture = new GenericTexture();
		texture.setUrl("http://www.behindthemirrors.com/files/minecraft/srpg/level_display.png");
		texture.setWidth(16).setHeight(16);
		texture.setX(205).setY(-1); // middle x : 213
		texture.setPriority(RenderPriority.Low);
		SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,texture);
		// level text
		GenericLabel label = new GenericLabel();
		label.setAlign(WidgetAnchor.CENTER_CENTER);
		label.setX(213).setY(8);
		label.setPriority(RenderPriority.Lowest);
		SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,label);
		levelDisplay = label;
		// charges
		chargeDisplay = new ArrayList<GenericTexture>();
		for (int i=0;i<10;i++) {
			// border/background
			texture = new GenericTexture();
			texture.setUrl("http://www.behindthemirrors.com/files/minecraft/srpg/charge_background.png");
			texture.setWidth(8).setHeight(8);
			texture.setX(170+i*7+(i>4?17:0)).setY(9); // middle x : 213
			SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,texture);
			// fill
			texture = new GenericTexture();
			texture.setUrl("http://www.behindthemirrors.com/files/minecraft/srpg/charge_fill.png");
			texture.setWidth(8).setHeight(8);
			texture.setX(170+i*7+(i>4?17:0)).setY(9); // middle x : 213
			texture.setVisible(false);
			texture.setPriority(RenderPriority.Low);
			SpoutManager.getPlayer(player).getMainScreen().attachWidget(SRPG.plugin,texture);
			chargeDisplay.add(texture);
		}
	}

}
