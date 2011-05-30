package com.behindthemirrors.minecraft.sRPG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Slime;

public class Utility {
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
	
	public static File createDefaultFile(File file, String description) {
        if (!file.exists()) {
        	new File(file.getParent()).mkdirs();
            InputStream input = SRPG.class.getResourceAsStream("/defaults/" + file.getName());
            if (input != null) {
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(file);
                    byte[] buf = new byte[8192];
                    int length = 0;
                    while ((length = input.read(buf)) > 0) {
                        output.write(buf, 0, length);
                    }
                    
                    SRPG.output("Created " + description + " from default");
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
			name += ".tamed";
		} else if (name.equals("slime")) {
			name += "." + Settings.SLIME_SIZES.get(((Slime)entity).getSize());
		}
		return name;
	}
	
	public static String repeat(String string, int n) {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++) {
            sb.append(string);
        }
        return sb.toString();
    }
	
	}
