package org.yaosupdater.online;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;

import de.sUpdater.R.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaosupdater.preferences.Configuracion;
import org.yaosupdater.ui.Principal;
import org.yaosupdater.utils.Parche;
import org.yaosupdater.utils.Update;

import android.os.Looper;
import android.util.Log;


public class OnlineJSON {
	
	private Update miVersion;
	private ArrayList<Update> versiones;
	private String lastVersion;
	private JSONObject json;
	private String[] mirrors;
	private ArrayList<Parche> parches;
	private String device;
	
	public ArrayList<Update> getVersiones(){
		return versiones;
	}
	
	public ArrayList<Parche> getParches(){
		return parches;
	}
	
	public OnlineJSON(){
		this.json = checkJSON();
		String[] versionStr = getModVersion("ro.update.version").split("\\.");
		int[] version = new int[versionStr.length];
		for(int i = 0; i < version.length; i++){
			version[i] = Integer.parseInt(versionStr[i]); 
		}
		this.miVersion = new Update("sMIUI", "", "", version);
		buscarVersion();
	}
	
	public JSONObject getJson() {
		return json;
	}

	public void setJson(JSONObject json) {
		this.json = json;
	}
	
	public String[] getMirrors(){
		return mirrors;
	}

	public String getLastVersion() {
		return lastVersion;
	}
	public void setLastVersion(String lastVersion) {
		this.lastVersion = lastVersion;
	}
	public JSONObject checkJSON(){
		try {
			device = getModVersion("ro.product.device");
			URL edgeJson = new URL(Configuracion.direccionJSON+device+"/updater.json");
			URLConnection urlCon = edgeJson.openConnection();
			urlCon.setConnectTimeout(5000);
			urlCon.setReadTimeout(5000);
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
			StringBuilder builder = new StringBuilder();
			for(String line = null; (line = reader.readLine()) != null;){
				builder.append(line).append("\n");
			}
			json = new JSONObject(builder.toString());
			return json;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	public ArrayList<Update> buscarVersion(){
		if(json == null){
			return null;
		}
		versiones = new ArrayList<Update>();
		parches = new ArrayList<Parche>();
		JSONArray versions;
		ArrayList<Update> incrementales = new ArrayList<Update>();
		try {
			JSONArray jsonMirrors = json.getJSONArray("MirrorList");
			mirrors = new String[jsonMirrors.length()];
			for(int i = 0; i < jsonMirrors.length(); i++){
				mirrors[i] = (String) jsonMirrors.get(i);
			}
			JSONArray incrementals = this.json.getJSONArray("Incrementales");
			for(int i = 0; i < incrementals.length(); i++){
				String versionStr = incrementals.getJSONObject(i).getString("version");
				Log.d("MIUIESUPDATER",versionStr);
				String ArrayList[] = versionStr.split("\\.");
				int version[] = new int[ArrayList.length];
				for(int j = 0; j < ArrayList.length; j++){
					version[j] = Integer.parseInt(ArrayList[j]);
				}
				Update versionAux = new Update(incrementals.getJSONObject(i).getString("nombre"), incrementals.getJSONObject(i).getString("url"), incrementals.getJSONObject(i).getString("descUrl"), version);
				versionAux.setVersionForApply(incrementals.getJSONObject(i).getString("versionForApply"));
				String[] versionForApply = versionAux.getVersionForApply().split("\\.");
				int[] versionIndicada = new int[versionForApply.length];
				for(int j = 0; j < versionForApply.length;j++){
					versionIndicada[j] = Integer.parseInt(versionForApply[j]);
				}
				if(Arrays.equals(versionIndicada,miVersion.getVersion())){
					Log.d("MIUIESUPDATER","Algo hay que decir");
					if(miVersion.compareTo(versionAux) == -1){
						Log.d("MIUIESUPDATER","Y aquí aún más");
					}
						incrementales.add(versionAux);
				}
			}
			versions = this.json.getJSONArray("Actualizaciones");
			for(int i = 0; i < versions.length(); i++){
				String versionStr = versions.getJSONObject(i).getString("version");
				Log.d("MIUIESUPDATER",versionStr);
				String ArrayList[] = versionStr.split("\\.");
				int version[] = new int[ArrayList.length];
				for(int j = 0; j < ArrayList.length; j++){
					version[j] = Integer.parseInt(ArrayList[j]);
				}
				Update versionAux = new Update(versions.getJSONObject(i).getString("nombre"), versions.getJSONObject(i).getString("url"), versions.getJSONObject(i).getString("descUrl"), version);
				if(miVersion.compareTo(versionAux) == -1){
					versiones.add(versionAux);
				}
			}
			
			JSONArray patchs = this.json.getJSONArray("Parches");
			for(int i = 0; i < patchs.length(); i++){
				String[] patchMax = patchs.getJSONObject(i).getString("versionMax").split("\\.");
				String[] patchMin = patchs.getJSONObject(i).getString("versionMin").split("\\.");
				int[] patchMaxInt = new int[patchMax.length];
				for(int j=0; j < patchMax.length;j++){
					patchMaxInt[j] = Integer.parseInt(patchMax[j]);
				}
				int[] patchMinInt = new int[patchMin.length];
				for(int j=0; j < patchMin.length;j++){
					patchMinInt[j] = Integer.parseInt(patchMin[j]);
				}
				Parche parcheAux = new Parche(patchs.getJSONObject(i).getString("nombre"), patchs.getJSONObject(i).getString("url"),patchs.getJSONObject(i).getString("descUrl"),patchMaxInt,patchMinInt);
				if(parcheAux.aplicable(miVersion)){
					parches.add(parcheAux);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(versiones.size() > 0){
			lastVersion = versiones.get(0).getNombre();
		}
		incrementales = ordenarArrayLists(incrementales);
		for(int i= 0; i < versiones.size();i++){
			incrementales.add(0,versiones.get(i));
		}
		versiones = incrementales;
		return versiones;
	}
	
	public String getModVersion(String prop) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop "+prop);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        }
        catch (IOException ex) {
            Log.e("MIUIESUPDATER", "Unable to read sysprop "+prop, ex);
            return null;
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                }
                catch (IOException e) {
                    Log.e("MIUIESUPDATER", "Exception while closing InputStream", e);
                }
            }
        }
        return line;
    }
	
	private ArrayList<Update> ordenarArrayLists(ArrayList<Update> incrementales){
		Update updateAux;
		for(int i = 0; i < versiones.size(); i++){
			for(int j = 0; j < versiones.size()-1; j++){
				if(versiones.get(j).compareTo(versiones.get(j+1)) == -1){
					updateAux = (Update) versiones.get(j+1);
					versiones.set(j+1, versiones.get(j));
					versiones.set(j, updateAux);
				}
			}
		}
		for(int i = 0; i < incrementales.size(); i++){
			for(int j = 0; j < incrementales.size()-1; j++){
				if(incrementales.get(j).compareTo(incrementales.get(j+1)) == -1){
					updateAux = (Update) incrementales.get(j+1);
					incrementales.set(j+1, incrementales.get(j));
					incrementales.set(j, updateAux);
				}
			}
		}
		return incrementales;
	}
	
}
