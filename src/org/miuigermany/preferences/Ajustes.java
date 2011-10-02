package org.miuigermany.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.miuigermany.R;
import org.miuigermany.ui.Principal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Ajustes extends PreferenceActivity{
	
	private String TAG = "MIUIUpdater-Preferences";
	private SharedPreferences sp;
	private Editor editor;
	
	@Override
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	    CheckBoxPreference check_init = (CheckBoxPreference) findPreference("check_init");
	    sp = getSharedPreferences("variables", MODE_PRIVATE);
	    editor = sp.edit();
	    ListPreference choiceRecovery = (ListPreference) findPreference("recovery");
	    String recovery = sp.getString("recovery", "ninguno");
	    if(recovery.equals("cwm")){
	    	choiceRecovery.setSummary("ClockworkMod");
	    }else if(recovery.equals("amonra")){
	    	choiceRecovery.setSummary("AmonRA");
	    }
	    check_init.setDefaultValue(sp.getBoolean("check_init", true));
	    check_init.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean  onPreferenceChange(Preference preference, Object newValue) {
				if(preference.equals(findPreference("check_init"))){	
					if(newValue.equals(false)){
						editor.putBoolean("check_init", false);			
					}else{
						editor.putBoolean("check_init", true);
					}
					editor.commit();
					Log.d(TAG,"Check_init "+newValue.toString());
					return true;
				}
				return false;
			}
		});
	    CheckBoxPreference check_md5 = (CheckBoxPreference) findPreference("force_md5");
	    check_md5.setDefaultValue(sp.getBoolean("md5", true));
	    check_md5.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if(newValue.equals(true)){
					editor.putBoolean("md5", true);
				}else{
					editor.putBoolean("md5", false);
				}
				editor.commit();
				return true;
			}
	    	
	    });
	    final ListPreference folderList = (ListPreference) findPreference("folder_select");
	    folderList.setSummary(sp.getString("directorio",""));
	    File directorio = new File("/sdcard/");
	    File[] archivos = directorio.listFiles();
	    ArrayList<String> carpetas = new ArrayList<String>();
	    for(int i = 0; i < archivos.length; i++){
	    	if(archivos[i].isDirectory()){
	    		carpetas.add(archivos[i].getName());
	    	}
	    }
	    String[] listaFinal = (String[]) carpetas.toArray(new String[carpetas.size()]);
	    folderList.setEntryValues(listaFinal);
	    Arrays.sort(listaFinal);
	    folderList.setEntries(listaFinal);
	    folderList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Editor editor = sp.edit();
				editor.putString("directorio", "/sdcard/"+(String) newValue+"/");
				editor.commit();
				Log.d("NEW DIRECTORY", sp.getString("directorio", ""));
				folderList.setSummary("/sdcard/"+ (String) newValue);
				return true;
			}
		});
	    CharSequence[] items = Principal.myContext().getResources().getStringArray(R.array.recoveries);
	    choiceRecovery.setDefaultValue(recovery);
	    choiceRecovery.setSummary(Principal.myContext().getText(R.string.changeRecoverySummary));
	    choiceRecovery.setEntries(items);
	    choiceRecovery.setEntryValues(items);
	    choiceRecovery.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Editor editor = sp.edit();
				if(((String) newValue).equals("ClockworkMod")){
					editor.putString("recovery", "cwm");
				}else if (((String) newValue).equals("AmonRA")){
					editor.putString("recovery", "amonra");
				}
				editor.commit();
				return true;
			}
		});
	  }

}
