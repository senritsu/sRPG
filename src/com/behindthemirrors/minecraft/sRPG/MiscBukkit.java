package com.behindthemirrors.minecraft.sRPG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureJob;


public class MiscBukkit {
	
	static HashMap<String,List<Material>> materialAliases = new HashMap<String, List<Material>>();
	static HashMap<Integer,String> SLIME_SIZES = new HashMap<Integer,String>();
	
	static {
		materialAliases.put("swords", Arrays.asList(new Material[] {Material.WOOD_SWORD,Material.STONE_SWORD,Material.IRON_SWORD,Material.DIAMOND_SWORD}));
		materialAliases.put("axes", Arrays.asList(new Material[] {Material.WOOD_AXE,Material.STONE_AXE,Material.IRON_AXE,Material.DIAMOND_AXE}));
		for (String string : new String[] {"pickaxes","picks"}) {
			materialAliases.put(string, Arrays.asList(new Material[] {Material.WOOD_PICKAXE,Material.STONE_PICKAXE,Material.IRON_PICKAXE,Material.DIAMOND_PICKAXE}));
		}
		for (String string : new String[] {"shovels","spades"}) {
			materialAliases.put(string, Arrays.asList(new Material[] {Material.WOOD_SPADE,Material.STONE_SPADE,Material.IRON_SPADE,Material.DIAMOND_SPADE}));
		}
		for (String string : new String[] {"empty","fists","unarmed","barehanded"}) {
			materialAliases.put(string, Arrays.asList(new Material[] {Material.AIR}));
		}
		// initialize slime int size to string mapping
		String[] sizes = {"small","normal","big","huge"};
		for (int i=1; i<5;i++) {
			MiscBukkit.SLIME_SIZES.put(i, sizes[i-1]);
		}
	}
	
	public static String getEntityName(Entity entity) {
		String name = entity.getClass().getSimpleName().toLowerCase().substring(5);
		if (name.equals("wolf")) {
			if (((Wolf)entity).getOwner() != null) {
				name += "-tamed";
			} else {
				name += "-wild";
			}
		} else if (name.equals("slime")) {
			name += "-" + SLIME_SIZES.get(((Slime)entity).getSize());
		}
		return name;
	}
	
	public static ItemStack getNaturalDrops(Block block) {
		ItemStack item = null;
		switch (block.getTypeId()) {
			case 0: ; break;
			case 1: item = new ItemStack(4, 1); break;
			case 2: item = new ItemStack(3, 1); break;
			case 7: ; break;
			case 8: ; break;
			case 9: ; break;
			case 10: ; break;
			case 11: ; break;
			case 13: if (SRPG.generator.nextDouble() < 0.1) {item = new ItemStack(318,1);} else {item = new ItemStack(13, 1);} break;
			case 16: item = new ItemStack(263, 1); break;
			case 17: item = new ItemStack(17, 1,(short)0,block.getData()); break;
			case 18: if (SRPG.generator.nextDouble() < 0.05) {item = new ItemStack(6,1,(short)0,block.getData());} break;
			case 20: ; break;
			case 21: item = new ItemStack(351,SRPG.generator.nextInt(5)+4); break;
			case 26: item = new ItemStack(355, 1); break;
			case 35: item = new ItemStack(35,1,(short)0,block.getData()); break;
			case 43: item = new ItemStack(44,1,(short)0,block.getData()); break;
			case 44: item = new ItemStack(44,1,(short)0,block.getData()); break;
			case 47: ; break;
			case 51: ; break;
			case 52: ; break;
			case 53: item = new ItemStack(5,1); break;
			case 55: item = new ItemStack(331,1); break;
			case 56: item = new ItemStack(264,1); break;
			case 59: item = new ItemStack(295,1); break;
			case 60: item = new ItemStack(3,1); break;
			case 62: item = new ItemStack(61,1); break;
			case 63: item = new ItemStack(323,1); break;
			case 64: item = new ItemStack(324,1); break;
			case 67: item = new ItemStack(4,1); break;
			case 68: item = new ItemStack(323,1); break;
			case 71: item = new ItemStack(330,1); break;
			case 73: item = new ItemStack(331,SRPG.generator.nextInt(2)+4); break;
			case 74: item = new ItemStack(331,SRPG.generator.nextInt(2)+4); break;
			case 75: item = new ItemStack(76,1); break;
			case 78: ; break;
			case 79: ; break;
			case 82: item = new ItemStack(337,4); break;
			case 83: item = new ItemStack(338,1); break;
			case 89: item = new ItemStack(348,1); break;
			case 90: ; break;
			case 93: item = new ItemStack(356,1); break;
			case 94: item = new ItemStack(356,1); break;
			default: new ItemStack(block.getTypeId(),1); break;
		}
		return item;
	}
	
	public static ArrayList<String> getChildren(HashMap<String,StructureJob> map, String parent) {
		ArrayList<String> children = new ArrayList<String>();
		for (String job : map.keySet()) {
			if (map.get(job).prerequisites.containsKey(parent)) {
				children.add(job);
			}
		}
		for (String child : children) {
			children.addAll(getChildren(map, child));
		}
		return children;
	}
	
	public static String stripPotency(String input) {
		if (input.contains("!")) {
			input = input.substring(0,input.indexOf("!"));
		}
		return input;
	}
	
	public static Integer parsePotency(String input) {
		if (input.contains("!")) {
			try { 
				return Integer.parseInt(input.substring(input.indexOf("!")+1));
			} catch (NumberFormatException ex) {
			}
		}
		return 1;
	}
	
	public static String parseSingularPlural(String input, Integer amount) {
		String singularEnding = input.substring(input.indexOf("(")+1, input.indexOf("|"));
		String pluralEnding = input.substring(input.indexOf("|")+1, input.indexOf(")"));
		return input.substring(0,input.indexOf("(")) + ((amount > 1 || amount == 0) ? pluralEnding : singularEnding);
	}
	
	public static Material parseMaterial(String string) {
		Material material;
		try {
			material = Material.getMaterial(Integer.parseInt(string));
		} catch (NumberFormatException ex) {
			material = Material.getMaterial(string);
		}
		return material != null ? material : Material.AIR;
	}
	
	public static ArrayList<Material> parseMaterialList(List<String> list) {
		HashSet<Material> materials = new HashSet<Material>();
		for (String entry : list) {
			try {
				materials.add(Material.getMaterial(Integer.parseInt(entry)));
			} catch (NumberFormatException ex) {
				// hack
				if (entry.equalsIgnoreCase("bow")) {
					materials.add(Material.BOW);
				// hack end
				} else if (materialAliases.containsKey(entry.toLowerCase())) {
					materials.addAll(materialAliases.get(entry.toLowerCase()));
				} else if (Material.getMaterial(entry.toUpperCase()) != null) {
					materials.add(Material.getMaterial(entry.toUpperCase()));
				}
			}
		}
		if (list.isEmpty()) {
			materials.add(null);
		}
		return new ArrayList<Material>(materials);
	}

	public static Double getArmorFactor(ProfileNPC profile) {
		return profile instanceof ProfilePlayer ? getArmorFactor(((ProfilePlayer)profile).player) : 1.0;
	}
	
	public static Double getArmorFactor(Player player) {
		double durability = 0.0;
		double maxdurability = 0.0;
		double basesum = 0.0;
		double reworked = 0.0;
		for (ItemStack item : player.getInventory().getArmorContents()) {
			double current = item.getDurability();
			double max = 0.0;
			double base = 0.0;
			double materialFactor = 0.0;
			boolean armor = true;
			switch (item.getTypeId()) {
			// leather
			case 298 : base = 1.5;max = 34;materialFactor = Settings.ARMOR_FACTORS.get(0); break;
			case 299 : base = 4;max = 49;materialFactor = Settings.ARMOR_FACTORS.get(0); break;
			case 300 : base = 3;max = 46;materialFactor = Settings.ARMOR_FACTORS.get(0); break;
			case 301 : base = 1.5;max = 40;materialFactor = Settings.ARMOR_FACTORS.get(0); break;
			// chainmail
			case 302 : base = 1.5;max = 67;materialFactor = Settings.ARMOR_FACTORS.get(1); break;
			case 303 : base = 4;max = 96;materialFactor = Settings.ARMOR_FACTORS.get(1); break;
			case 304 : base = 3;max = 92;materialFactor = Settings.ARMOR_FACTORS.get(1); break;
			case 305 : base = 1.5;max = 79;materialFactor = Settings.ARMOR_FACTORS.get(1); break;
			// iron
			case 306 : base = 1.5;max = 136;materialFactor = Settings.ARMOR_FACTORS.get(2); break;
			case 307 : base = 4;max = 192;materialFactor = Settings.ARMOR_FACTORS.get(2); break;
			case 308 : base = 3;max = 184;materialFactor = Settings.ARMOR_FACTORS.get(2); break;
			case 309 : base = 1.5;max = 160;materialFactor = Settings.ARMOR_FACTORS.get(2); break;
			// diamond
			case 310 : base = 1.5;max = 272;materialFactor = Settings.ARMOR_FACTORS.get(3); break;
			case 311 : base = 4;max = 384;materialFactor = Settings.ARMOR_FACTORS.get(3); break;
			case 312 : base = 3;max = 368;materialFactor = Settings.ARMOR_FACTORS.get(3); break;
			case 313 : base = 1.5;max = 320;materialFactor = Settings.ARMOR_FACTORS.get(3); break;
			// gold
			case 314 : base = 1.5;max = 68;materialFactor = Settings.ARMOR_FACTORS.get(4); break;
			case 315 : base = 4;max = 96;materialFactor = Settings.ARMOR_FACTORS.get(4); break;
			case 316 : base = 3;max = 92;materialFactor = Settings.ARMOR_FACTORS.get(4); break;
			case 317 : base = 1.5;max = 80;materialFactor = Settings.ARMOR_FACTORS.get(4); break;
			default : armor = false;
			}
			if (armor) {
				basesum += base;
				maxdurability += max;
				durability += current;
				reworked += base * materialFactor * current / (max > 0 ? max : 1.0);
			}
		}
		reworked *= 0.1;
		double original = 0.08 * basesum * durability / (maxdurability > 0 ? maxdurability : 1.0);
		SRPG.output("original armor mitigation: "+original);
		SRPG.output("reworked armor mitigation: "+reworked);
		
		return (1.0 - reworked) / (1.0 - original);
	}
}
