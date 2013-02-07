package de.robv.android.xposed.mods.smileys;

public class Common {
	public static final String TAG = "smiley";
	public static final String MY_PACKAGE = Common.class.getPackage().getName();
	public static final String MMS_PACKAGE = "com.android.mms";
	
	public static final String PREF_FILE = "smileys";
	public static final String PREF_SELECTED_PACK = "selected_pack";
	
	public static final int REQUEST_CHOOSE_PACK = 1;
	public static final int REQUEST_PICK_SMILEY_FILE = 2;
	public static final int REQUEST_IMPORT_SMILEY_FILE = 3;
	
	public static boolean XPOSED_ACTIVE = false;
}
