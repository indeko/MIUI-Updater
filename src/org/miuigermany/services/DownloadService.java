package org.miuigermany.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.miuigermany.R;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DownloadService extends Service{

	private DownloadManager dm;
	private String nombre;
	private String archivo;
	private String[] mirrors;
	private long enqueue;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startid){
		super.onStart(intent, startid);
		dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		Bundle bundle = intent.getExtras();
		nombre = (String) bundle.get("nombre");
        archivo = (String) bundle.get("archivo");
        mirrors = (String[]) bundle.get("mirrors");
        URL url;
        int j = 0;
        boolean found = false;
        while((j < mirrors.length)&&(!found)){
			try {
				url = new URL(mirrors[j]+archivo);
				URLConnection urlCon = url.openConnection();
	        	InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
	        	found = true;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				Log.d("MIUIUpdater", "File "+mirrors[j]+archivo+" was not found.");
				found = false;
			}
			j++;
        }	
        j--;
        SharedPreferences sp = getSharedPreferences("variables", MODE_PRIVATE);
        Request request = new Request(Uri.parse(mirrors[j]+archivo));
			URLConnection md5Url;
			try {
				md5Url = new URL(mirrors[j]+archivo+".md5sum").openConnection();
				BufferedReader br = new BufferedReader(new InputStreamReader(md5Url.getInputStream()));
				Log.d("MIUIUPDATER",sp.getString("directorio", "")+archivo+".md5sum");
				File md5sum = new File(sp.getString("directorio", "")+archivo+".md5sum");
				md5sum.createNewFile();
				String linea = br.readLine();
				if(linea != null){
					Log.d("MIUIUPDATER",linea);
					BufferedWriter bw = new BufferedWriter(new FileWriter(md5sum));
					bw.write(linea);
					bw.close();
				}
				br.close();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
        request.setDestinationUri(Uri.parse("file:///mnt"+sp.getString("directorio", "")+archivo+".part"));
        enqueue = dm.enqueue(request);
        Toast.makeText(getApplicationContext(), getString(R.string.downloadingUpdate), Toast.LENGTH_SHORT).show();
        final Intent i = new Intent();
        i.putExtra("estado", "descargando");
        i.setAction("descargando");
        sendBroadcast(i);
        BroadcastReceiver receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if(dm.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
					Query query = new Query();
					query.setFilterById(enqueue);
					Cursor c = dm.query(query);
					if(c.moveToFirst()){
						int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if(DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)){
							Toast.makeText(getApplicationContext(), getString(R.string.downloadComplete), Toast.LENGTH_SHORT).show();
							i.putExtra("archivo", archivo);
							i.putExtra("estado", "finalizado");
							sendBroadcast(i);
						}else{
							Toast.makeText(getApplicationContext(), getString(R.string.error_downloadFailed), Toast.LENGTH_SHORT).show();
							i.putExtra("archivo", archivo);
							i.putExtra("estado", "error");
							sendBroadcast(i);
						}
					}
						
				}
			}	
		};
		registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

}
