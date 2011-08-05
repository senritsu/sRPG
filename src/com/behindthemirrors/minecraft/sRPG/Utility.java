package com.behindthemirrors.minecraft.sRPG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

public class Utility {
	
	static HashMap<String,List<Material>> materialGroupNames = new HashMap<String, List<Material>>();
	static HashMap<Integer,String> SLIME_SIZES = new HashMap<Integer,String>();
	
	static {
		materialGroupNames.put("swords", Arrays.asList(new Material[] {Material.WOOD_SWORD,Material.STONE_SWORD,Material.IRON_SWORD,Material.DIAMOND_SWORD}));
		// initialize slime int size to string mapping
		String[] sizes = {"small","normal","big","huge"};
		for (int i=1; i<5;i++) {
			Utility.SLIME_SIZES.put(i, sizes[i-1]);
		}
	}
	
	public static String join(ArrayList<String> list, String delimiter) {
		StringBuilder str = new StringBuilder();
		Boolean first = true;
		for (String entry : list) {
			if (!first) {
				str.append(delimiter);
			} else {
				first = false;
			}
			str.append(entry);
		}
		return str.toString();
	}
	
	public static File createDefaultFile(File file, String description, String defaultFileName) {
        if (!file.exists()) {
        	new File(file.getParent()).mkdirs();
            InputStream input = SRPG.class.getResourceAsStream("/defaults/" + defaultFileName);
            if (input != null) {
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(file);
                    byte[] buf = new byte[8192];
                    int length = 0;
                    while ((length = input.read(buf)) > 0) {
                        output.write(buf, 0, length);
                    }
                    
                    SRPG.output("Created " + description);
                } catch (IOException e) {
                    e.printStackTrace();
                    SRPG.output("Error creating " + description);
                } finally {
                    try {
                        if (input != null)
                            input.close();
                    } catch (IOException e) {}

                    try {
                        if (output != null)
                            output.close();
                    } catch (IOException e) {}
                }
            } 
        }
        return file;
	}
	
	public static String getEntityName(Entity entity) {
		String name = entity.getClass().getSimpleName().toLowerCase().substring(5);
		// TODO: implement tamed check for wolf
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
	
	public static String repeat(String string, int n) {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++) {
            sb.append(string);
        }
        return sb.toString();
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
	
	public static void putAll(HashMap<StructurePassive, EffectDescriptor> current, ArrayList<StructurePassive> arrayList, EffectDescriptor value) {
		for (StructurePassive key : arrayList) {
			current.put(key, value);
		}
	}
	
	public static void removeAll(ArrayList<String> list,String item) {
		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().equals(item)) {
				iterator.remove();
			}
		}
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
	
	public static ArrayList<Material> parseMaterialList(List<String> list) {
		ArrayList<Material> materials = new ArrayList<Material>();
		for (String entry : list) {
			try {
				materials.add(Material.getMaterial(Integer.parseInt(entry)));
			} catch (NumberFormatException ex) {
				// hack
				if (entry.equalsIgnoreCase("bow")) {
					materials.add(Material.BOW);
				// hack end
				} else if (materialGroupNames.containsKey(entry.toLowerCase())) {
					materials.addAll(materialGroupNames.get(entry.toLowerCase()));
				} else if (Material.getMaterial(entry) != null) {
					materials.add(Material.getMaterial(entry));
				}
			}
		}
		if (list.isEmpty()) {
			materials.add(null);
		}
		return materials;
	}

}
