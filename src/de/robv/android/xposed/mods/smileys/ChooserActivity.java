package de.robv.android.xposed.mods.smileys;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser;
import de.robv.android.xposed.mods.smileys.parser.SmileyPackParser.Smiley;
import de.robv.android.xposed.mods.smileys.views.MovieDrawable;

public class ChooserActivity extends Activity {
	private SmileyPackAdapter adapter = null;
	private SharedPreferences pref = null;

	@SuppressLint("WorldReadableFiles")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_chooser);
	    
	    pref = getSharedPreferences(Common.PREF_FILE, Context.MODE_WORLD_READABLE);
	    
	    ListView lv = (ListView) findViewById(R.id.smiley_list);
	    adapter = new SmileyPackAdapter(this);
	    adapter.setNotifyOnChange(false);
	    lv.setAdapter(adapter);
	    reloadSmileys();
	    
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String filename = adapter.getItem(position).getFile().getAbsolutePath();
				pref.edit().putString(Common.PREF_SELECTED_PACK, filename).commit();
				setResult(Activity.RESULT_OK);
				finish();
			}
		});
		
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				final SmileyPackParser item = adapter.getItem(position);
				new AlertDialog.Builder(ChooserActivity.this)
					.setTitle(R.string.alert_title_delete)
					.setMessage(getString(R.string.alert_message_delete, item.getTitle()))
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							item.getFile().delete();
							reloadSmileys();
						}
					})
					.setNegativeButton(android.R.string.no, null)
					.create()
					.show();
	            return true;
            }
		});
	}
	
	private void reloadSmileys() {
		adapter.clear();
		for (File file : getFilesDir().listFiles()) {
			if (!file.getName().endsWith(".smileys.zip") || !file.isFile() || !file.canRead())
				continue;
			
			try {
	            SmileyPackParser parser = new SmileyPackParser(file);
	            adapter.add(parser);
            } catch (IOException e) {
            	Log.e(Common.TAG, "could not read smiley pack", e);
            }
			
		}
		adapter.notifyDataSetChanged();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chooser, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_add:
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("application/zip");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                
                Intent chooser = Intent.createChooser(intent, getString(R.string.title_chooser_select_smiley_pack));
                startActivityForResult(chooser, Common.REQUEST_PICK_SMILEY_FILE);
				return true;
		
			case R.id.menu_reload:
				reloadSmileys();
				return true;
				
			case R.id.menu_reset:
				pref.edit().remove(Common.PREF_SELECTED_PACK).commit();
				setResult(Activity.RESULT_OK);
				finish();
				return true;
		
			default:
				return super.onOptionsItemSelected(item);
		}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        	case Common.REQUEST_PICK_SMILEY_FILE:
        		if (resultCode == Activity.RESULT_CANCELED || data == null)
        			return;
        	
				Intent intent = new Intent(this, ImportActivity.class);
				intent.setData(data.getData());
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); 
				startActivityForResult(intent, Common.REQUEST_IMPORT_SMILEY_FILE);
				break;
				
        	case Common.REQUEST_IMPORT_SMILEY_FILE:
				if (resultCode == Activity.RESULT_OK)
					reloadSmileys();
        		break;
        }
    }
	
	private class SmileyPackAdapter extends ArrayAdapter<SmileyPackParser> {
		public SmileyPackAdapter(Context context) {
	        super(context, R.layout.list_item_smileypack, android.R.id.text1);
        }
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
		    View view = super.getView(position, convertView, parent);
		    
		    SmileyPackParser parser = getItem(position);
		    
		    Smiley[] smileys = parser.getSmileys();
		    Drawable icon = null;
		    if (smileys.length > 0) {
		    	try {
			    	if (smileys[0].getImageName().endsWith(".gif")) {
		                    icon = new MovieDrawable(smileys[0].getImageAsMovie());
			    	} else {
			    		icon = smileys[0].getImageAsDrawable();
			    	}
		    	} catch (IOException e) {
		    		Log.e(Common.TAG, "could not load first smiley", e);
		    	}
		    }
		    if (icon == null)
		    	icon = getResources().getDrawable(R.drawable.ic_unknown);
		    
	    	((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(icon);
	    	String summary = parser.getSummary();
    		((TextView) view.findViewById(android.R.id.text2)).setText(summary);
    		((TextView) view.findViewById(android.R.id.text2)).setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
		    
		    return view;
		}
	}
}
