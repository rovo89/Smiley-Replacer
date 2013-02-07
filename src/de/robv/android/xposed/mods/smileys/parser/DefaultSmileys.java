package de.robv.android.xposed.mods.smileys.parser;

import java.util.HashMap;

import android.content.res.Resources;
import de.robv.android.xposed.mods.smileys.Common;

public final class DefaultSmileys {
	protected static String[] origNames = null;
	public static final HashMap<String, Integer> IDs = new HashMap<String, Integer>();
	static {
		IDs.put("HAPPY", 0);
		IDs.put("SAD", 1);
		IDs.put("WINKING", 2);
		IDs.put("TONGUE_STICKING_OUT", 3);
		IDs.put("SURPRISED", 4);
		IDs.put("KISSING", 5);
		IDs.put("YELLING", 6);
		IDs.put("COOL", 7);
		IDs.put("MONEY_MOUTH", 8);
		IDs.put("FOOT_IN_MOUTH", 9);
		IDs.put("EMBARRASSED", 10);
		IDs.put("ANGEL", 11);
		IDs.put("UNDECIDED", 12);
		IDs.put("CRYING", 13);
		IDs.put("LIPS_ARE_SEALED", 14);
		IDs.put("LAUGHING", 15);
		IDs.put("WTF", 16);
		IDs.put("HEART", 17);
		IDs.put("MAD", 18);
		IDs.put("SMIRK", 19);
		IDs.put("POKERFACE", 20);
	}
	
	public static void initOrigNames(Resources res) {
		if (origNames != null)
			return;
		
		origNames = res.getStringArray(res.getIdentifier("default_smiley_names", "array", Common.MMS_PACKAGE));
    }
	
	public static boolean hasOrigNames() {
		return (origNames != null);
	}
	
	public static String maybeReplaceDescription(String desc) {
		if (!desc.startsWith("DEFAULT:"))
			return desc;
		
		desc = desc.substring(8);
		if (origNames == null || !DefaultSmileys.IDs.containsKey(desc))
			return desc;
		
		int id = DefaultSmileys.IDs.get(desc);
		if (id > origNames.length)
			return desc;
		
		return origNames[id];
	}
}
