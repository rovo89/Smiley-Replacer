package de.robv.android.xposed.mods.smileys;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticObjectField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.XResources;
import android.content.res.XResources.DrawableLoader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.mods.smileys.parser.DefaultSmileys;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser.Smiley;
import de.robv.android.xposed.mods.smileys.views.AutoHeightImageSpan;
import de.robv.android.xposed.mods.smileys.views.MovieDrawable;
import de.robv.android.xposed.mods.smileys.views.MovieSpan;

public class Mod implements IXposedHookInitPackageResources, IXposedHookLoadPackage {
	private static final int INITIAL_SMILEY_LIST_SIZE = 20;
	private static final int ANIMATION_REFRESH_RATE = 50;
	private static final int BACKGROUND_REFRESH_RATE = 1000;
	
	private boolean smileysLoaded = false;
	private ArrayList<String> codes;
	private ArrayList<Integer> resIds;
	private ArrayList<String> descriptions;
	private SparseArray<Smiley> resMap;
	
	private final WeakHashMap<TextView, Boolean> animationTextFields = new WeakHashMap<TextView, Boolean>();
	
	@Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(Common.MY_PACKAGE)) {
			setStaticBooleanField(findClass(Common.class.getName(), lpparam.classLoader), "XPOSED_ACTIVE", true);
			return;
		}
		
		if (!lpparam.packageName.equals(Common.MMS_PACKAGE))
			return;
		
		final XSharedPreferences pref = new XSharedPreferences(Common.MY_PACKAGE, Common.PREF_FILE);
		String filename = pref.getString(Common.PREF_SELECTED_PACK, null);
		if (filename == null) {
			Log.i(Common.TAG, "no smiley pack selected");
			return;
		}
		
		Smiley[] smileys = new SmileyPackParser(filename).getSmileys();
		
		codes = new ArrayList<String>(INITIAL_SMILEY_LIST_SIZE);
		resIds = new ArrayList<Integer>(INITIAL_SMILEY_LIST_SIZE);
		descriptions = new ArrayList<String>(INITIAL_SMILEY_LIST_SIZE);
		resMap = new SparseArray<Smiley>(INITIAL_SMILEY_LIST_SIZE / 2);
		
		for (Smiley smiley : smileys) {
			codes.add(smiley.getCode());
			int resId = XResources.getFakeResId("smiley:" + smiley.getImageName());
			resMap.put(resId, smiley);
			resIds.add(resId);
			descriptions.add(smiley.getDescription());
		}
		
		int[] resIdsArray = new int[resIds.size()];
		for (int i = 0; i < resIdsArray.length; i++)
			resIdsArray[i] = resIds.get(i);
			
		Class<?> clsSmileyParser = findClass(Common.MMS_PACKAGE + ".util.SmileyParser", lpparam.classLoader);
		setStaticObjectField(clsSmileyParser, "DEFAULT_SMILEY_RES_IDS", resIdsArray);
		
		smileysLoaded = true;
		
		findAndHookMethod(clsSmileyParser, "addSmileySpans", CharSequence.class, new XC_MethodReplacement() {
			@SuppressWarnings("unchecked")
            @Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				CharSequence text = (CharSequence) param.args[0];
				Pattern mPattern = (Pattern) getObjectField(param.thisObject, "mPattern");
				HashMap<String, Integer> mSmileyToRes = (HashMap<String, Integer>) getObjectField(param.thisObject, "mSmileyToRes");
				
		        SpannableStringBuilder builder = new SpannableStringBuilder(text);

		        Matcher matcher = mPattern.matcher(text);
		        while (matcher.find()) {
		            int resId = mSmileyToRes.get(matcher.group());
		            ReplacementSpan span = getReplacementSpan(resMap.get(resId));
		            if (span == null)
		            	continue;
		            
		            builder.setSpan(span,
		                            matcher.start(), matcher.end(),
		                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }

		        return builder;
			}
		});
		
		findAndHookMethod(TextView.class, "sendOnTextChanged", CharSequence.class, int.class, int.class, int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TextView tv = (TextView) param.thisObject;
				CharSequence text = (CharSequence) param.args[0];
				if (MovieSpan.hasMovieSpans(text)) {
					animationTextFields.put(tv, Boolean.TRUE);
				} else {
					animationTextFields.remove(tv);
				}
			}
		});
		
        final Handler handler = new Handler();
        handler.post(new Runnable() {
        	@Override
        	public void run() {
        		//Log.d("smileystart", "Time: " +  SystemClock.uptimeMillis());
        		final long now = SystemClock.uptimeMillis();
        		boolean hasActiveWindows = false;
        		for (TextView tv : animationTextFields.keySet()) {
        			if (tv.hasWindowFocus()) {
        				tv.postInvalidate();
        				hasActiveWindows = true;
        			}
        		}
        		long rate = hasActiveWindows ? ANIMATION_REFRESH_RATE : BACKGROUND_REFRESH_RATE; 
        		handler.postAtTime(this, now + rate - (now % rate));
        	}
        });
    }
	
	private ReplacementSpan getReplacementSpan(Smiley smiley) throws IOException {
		if (smiley == null)
			return null;
		
		if (smiley.getImageName().endsWith(".gif")) {
			return new MovieSpan(smiley.getImageAsMovie());
		} else {
			return new AutoHeightImageSpan(smiley.getImageAsDrawable());
		}
	}
	
	private Drawable getDrawable(Smiley smiley) throws IOException {
		if (smiley == null)
			return null;
		
		if (smiley.getImageName().endsWith(".gif")) {
			return new MovieDrawable(smiley.getImageAsMovie());
		} else {
			return smiley.getImageAsDrawable();
		}
	}

	@Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (!smileysLoaded || !resparam.packageName.equals(Common.MMS_PACKAGE))
			return;
		
		final XResources res = resparam.res;
		DefaultSmileys.initOrigNames(res);
		
		String[] descriptionsArray = new String[descriptions.size()];
		for (int i = 0; i < descriptionsArray.length; i++) {
			descriptionsArray[i] = DefaultSmileys.maybeReplaceDescription(descriptions.get(i));
		}
		String[] codesArray = codes.toArray(new String[codes.size()]);
		
		res.setReplacement(Common.MMS_PACKAGE, "array", "default_smiley_names", descriptionsArray);
		res.setReplacement(Common.MMS_PACKAGE, "array", "default_smiley_texts", codesArray);
		
		for (int i = 0; i < resMap.size(); i++)
			res.setReplacement(resMap.keyAt(i), callbackReplaceSmileyDrawable);
    }
	
	private DrawableLoader callbackReplaceSmileyDrawable = new DrawableLoader() {
		@Override
		public Drawable newDrawable(XResources res, int id) throws Throwable {
			return getDrawable(resMap.get(id));
		}
	};
}
