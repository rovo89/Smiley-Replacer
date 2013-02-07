package de.robv.android.xposed.mods.smileys;

import java.io.File;
import java.io.IOException;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.robv.android.xposed.mods.smileys.parser.DefaultSmileys;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser.Smiley;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser.SmileyGroup;
import de.robv.android.xposed.mods.smileys.views.MovieDrawable;

public class PreviewFragment extends Fragment {
	private SmileyPackParser parser;
	private String error = null;
	
	public PreviewFragment() {}
	
	public static PreviewFragment newInstance(File file) {
		Bundle args = new Bundle();
		if (file != null)
			args.putString("filename", file.getAbsolutePath());
		
		PreviewFragment fragment = new PreviewFragment();
		fragment.setArguments(args);
		return fragment;
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!DefaultSmileys.hasOrigNames()) {
			try {
				Resources mmsRes = getActivity().getPackageManager().getResourcesForApplication(Common.MMS_PACKAGE);
				DefaultSmileys.initOrigNames(mmsRes);
				SmileyPackParser.setLocale(mmsRes.getConfiguration().locale);
			} catch (NameNotFoundException e) {
				Log.e(Common.TAG, "could not load default descriptions from " + Common.MMS_PACKAGE, e);
			}
		}
		
        Bundle args = getArguments();
        if (args != null) {
            String filename = args.getString("filename");
            if (filename != null) {
	    		try {
	    	        parser = new SmileyPackParser(filename);
	            } catch (IOException e) {
	            	Log.e(Common.TAG, "could not load smiley pack", e);
	            	error = e.getLocalizedMessage();
	            }
            }
        }
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (parser == null) {
			View view = inflater.inflate(R.layout.fragment_preview_empty, container, false);
			if (error != null)
				((TextView) view.findViewById(R.id.title)).setText(R.string.preview_error_occured + "\n" + error);
			return view;
		}
		
		ListView lv = (ListView) inflater.inflate(R.layout.fragment_preview, container, false);
		View header = inflater.inflate(R.layout.fragment_preview_header, lv, false);
		
        TextView viewTitle = ((TextView) header.findViewById(R.id.title));
        TextView viewDescription = ((TextView) header.findViewById(R.id.description));
        viewTitle.setText(parser.getTitle());
        String summary = parser.getSummary();
        if (!summary.isEmpty())
        	viewDescription.setText(summary);
        else
        	viewDescription.setVisibility(View.GONE);
        lv.addHeaderView(header, null, false);
        
	    lv.setAdapter(new SmileyAdapter(getActivity(), parser.getSmileyGroups()));
        return lv;
	}
	
	private class SmileyAdapter extends ArrayAdapter<SmileyGroup> {
		private LayoutInflater mInflater;
		
		public SmileyAdapter(Context context, SmileyGroup[] smileys) {
	        super(context, 0, smileys);
	        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        
        }
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = (convertView == null) ? mInflater.inflate(R.layout.list_item_smiley, parent, false) : convertView;
			
		    SmileyGroup group = getItem(position);
		    Smiley base = group.getBase();
		    
		    String desc = DefaultSmileys.maybeReplaceDescription(base.getDescription());
		    
	    	StringBuffer codeString = null;
	    	for (String code : group.getCodes()) {
	    		if (codeString == null)
	    			codeString = new StringBuffer();
	    		else
	    			codeString.append("  ");
	    		
	    		codeString.append(code);
	    	}
		    
		    Drawable icon = null;
	    	try {
		    	if (base.getImageName().endsWith(".gif")) {
	                    icon = new MovieDrawable(base.getImageAsMovie());
		    	} else {
		    		icon = base.getImageAsDrawable();
		    	}
	    	} catch (IOException e) {
	    		Log.e(Common.TAG, "could not load smiley", e);
	    	}
		    if (icon == null)
		    	icon = getResources().getDrawable(R.drawable.ic_unknown);
		    
	    	((TextView) view.findViewById(R.id.title)).setText(desc);
	    	((TextView) view.findViewById(R.id.codes)).setText(codeString);
	    	((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(icon);
		    return view;
		}
		
		@Override
		public boolean isEnabled(int position) {
		    return false;
		}
	}
}
