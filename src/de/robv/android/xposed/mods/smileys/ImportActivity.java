package de.robv.android.xposed.mods.smileys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ImportActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import);
		
		Uri uri = getIntent().getData();
		final File file = (uri != null) ? new File(uri.getPath()) : null;
		
		if (file != null) {
			Button btnStartImport = (Button) findViewById(R.id.btn_start_import);
			btnStartImport.setEnabled(true);
			btnStartImport.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String targetFileName = file.getName();
					if (!targetFileName.endsWith(".smileys.zip"))
						targetFileName += ".smileys.zip";
					
					final File targetFile = new File(getFilesDir(), targetFileName);
					if (!targetFile.exists()) {
						startImport(file, targetFile);
					} else {
						new AlertDialog.Builder(ImportActivity.this)
							.setTitle(R.string.alert_title_file_exists)
							.setMessage(R.string.alert_message_file_exists)
							.setIconAttribute(android.R.attr.alertDialogIcon)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									startImport(file, targetFile);
								}
							})
							.setNegativeButton(android.R.string.no, null)
							.create()
							.show();
					}
				}
			});
		}
		
		if (savedInstanceState == null) {
	        FragmentTransaction ft = getFragmentManager().beginTransaction();
	        Fragment newFragment = PreviewFragment.newInstance(file);
	        ft.replace(R.id.preview_fragment, newFragment);
	        ft.commit();
		}
	}
	
	private void startImport(final File source, final File target) {
		try {
			copyFile(source, target);
			target.setReadable(true, false);
			
		} catch (IOException e) {
			new AlertDialog.Builder(ImportActivity.this)
				.setTitle(R.string.alert_title_import_failed)
				.setMessage(e.getMessage())
				.setIconAttribute(android.R.attr.alertDialogIcon)
				.create()
				.show();
			return;
		}
		
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE)
					source.delete();
				
				setResult(Activity.RESULT_OK);
				finish();
			}
		};
			
		new AlertDialog.Builder(ImportActivity.this)
			.setTitle(R.string.alert_title_import_success)
			.setMessage(R.string.alert_message_import_success)
			.setPositiveButton(android.R.string.yes, listener)
			.setNegativeButton(android.R.string.no, listener)
			.create()
			.show();
	}
	
	private static void copyFile(File source, File target) throws IOException {
        InputStream inStream = new FileInputStream(source);
        OutputStream outStream = new FileOutputStream(target);

        byte buffer[] = new byte[1024];
        int length = 0;
        while ((length = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, length);
        }

        inStream.close();
        outStream.close();
	}
}
