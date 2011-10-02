package com.behindthemirrors.minecraft.sRPG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;

public class MiscGeneric {

	private static final String[] RCODE = {"M", "CM", "D", "CD", "C", "XC", "L",
        "XL", "X", "IX", "V", "IV", "I"};
	private static final int[]    BVAL  = {1000, 900, 500, 400,  100,   90,  50,
        40,   10,    9,   5,   4,    1};

	
	public static String potencyToRoman(int binary) {
        String roman = " ";         // Roman notation will be accumualated here.
        
        // Loop from biggest value to smallest, successively subtracting,
        // from the binary value while adding to the roman representation.
        for (int i = 0; i < RCODE.length; i++) {
            while (binary >= BVAL[i]) {
                binary -= BVAL[i];
                roman  += RCODE[i];
            }
        }
        return binary > 1 && binary < 4000 ? roman : "";
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

	public static String join(List<String> list, String delimiter) {
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

	public static String repeat(String string, int n) {
	    final StringBuilder sb = new StringBuilder();
	    for(int i = 0; i < n; i++) {
	        sb.append(string);
	    }
	    return sb.toString();
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

}
