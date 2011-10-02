package org.miuigermany.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import org.miuigermany.R;
import org.miuigermany.ui.Principal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ApplyUpdate {
	
	private static String file;
	private static boolean acceptNoMD5 = false;
	//Dispositivos que necesitan de un pequeño hack para flashear directamente:
	
	public static void aplicarUpdate(String archivo, boolean md5){
		//Si la verificación de md5 está activada, se llama al método de verificación
		if(md5){
			//Si el método de verificación devuelve false, volvemos atrás
			if(!comprobarMD5SUM(archivo)){
				return;
			}
		}
		Handler dialogHandler = new Handler(){
			@Override
			public void handleMessage(final Message msg) {
				super.handleMessage(msg);
				switch(msg.what){
				case(1):
					AlertDialog.Builder alertBuilder = new AlertDialog.Builder(Principal.myContext());
					alertBuilder.setMessage(Principal.myContext().getString(R.string.alert_phoneWillReboot));
					alertBuilder.setPositiveButton(Principal.myContext().getString(R.string.confirm), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String dispositivo = reconocerDispositivo();
							try {
								String archivo = file;
								//Creamos el proceso su y hacemos un buffer os para escribir en una consola de linux
								SharedPreferences sp = Principal.myContext().getSharedPreferences("variables", Principal.MODE_PRIVATE);
								String recovery = sp.getString("recovery", "ninguno");
								Process su = Runtime.getRuntime().exec("su");
								DataOutputStream os = new DataOutputStream(su.getOutputStream());
								archivo = archivo.replace("/sdcard/", "");
								Log.d("ARCHIVO", "/sdcard/"+archivo);
								Log.d("DISPOSITIVO",dispositivo);
								//Para estos dispositivos es necesaria un pequeño hack, de lo contrario, el recovery no aplica la actualización
								if(dispositivo.equals("m1ref")){
									FileInputStream fis = new FileInputStream(new File("/sdcard/"+archivo));
									ZipInputStream zin = new ZipInputStream(fis);
									ZipEntry entry;
									while((entry = zin.getNextEntry()) != null){
										Log.d("ZIP", "Extracting: "+entry);
										int buffer = 2048;
										FileOutputStream fos = new FileOutputStream("/sdcard/"+entry.getName());
										BufferedOutputStream dest = new BufferedOutputStream(fos, buffer);
										int count;
										byte data[] = new byte[buffer];
										while((count = zin.read(data, 0, buffer)) != -1){
											dest.write(data, 0, count);
										}
										dest.flush();
										dest.close();
									}
									zin.close();
									os.writeBytes("reboot\n");
									os.flush();
								}else{
									os.writeBytes("echo boot-recovery > /cache/recovery/command\n");
									os.writeBytes("echo --update_package=SDCARD:"+archivo+" >> /cache/recovery/command\n");
									os.flush();
									if(recovery.equals("cwm")){
										os.writeBytes("rm /cache/recovery/command\n");
										os.writeBytes("echo 'install_zip(\"/sdcard/"+archivo+"\");' > /cache/recovery/extendedcommand\n");
										os.writeBytes("echo 'install_zip(\"/sdcard/"+archivo+"\");' > /sdcard/MIUIUpdater/lastCommand\n");
										os.writeBytes("exit\n");
										os.flush();
										try {
											if((su.waitFor() == 127) || (su.waitFor() == 255)){
												Toast.makeText(Principal.myContext(), Principal.myContext().getText(R.string.error_su), Toast.LENGTH_SHORT).show();
												Log.e("MIUIUPDATER","Cannot get root access");
												su.destroy();
											}else{
											  	su.destroy();
												su = Runtime.getRuntime().exec("su");
												os = new DataOutputStream(su.getOutputStream());
												os.writeBytes("reboot recovery\n");
												os.flush();
											}
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}else{
										os.writeBytes("reboot recovery\n");
									}
								}
								//Se ejecutan las líneas
								os.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
					alertBuilder.setTitle(Principal.myContext().getString(R.string.warning));
					alertBuilder.setNegativeButton(Principal.myContext().getString(R.string.cancel), null);
					AlertDialog dialog = alertBuilder.create();
					dialog.show();
					break;
				}
			}
		};
		file = archivo;
		dialogHandler.sendEmptyMessage(1);
	}
	
	private static String reconocerDispositivo(){
		String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop ro.product.device");
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
            return line;
        }catch (IOException ex) {
            Log.e("MIUIUPDATER", "Unable to read sysprop ro.product.device", ex);
            return null;
        }
	}
	
	private static boolean comprobarMD5SUM(final String archivo){
		//Se lee el archivo .md5sum
		File md5File = new File(archivo+".md5sum");
		try {
			FileInputStream fis = new FileInputStream(md5File);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String md5 = br.readLine();
			//Se leen los 32 primeros caracteres (la cadena md5 generada)
			md5 = md5.substring(0, 32);
			File archivoFile = new File(archivo);
			//Comprobamos que la cadena leída sea la misma que la generada al comprobar el archivo descargado
			if(MD5.comprobarMD5(md5, archivoFile)){
				return true;
			}else{
				Handler handler = new Handler(){
					@Override
			        public void handleMessage(Message msg) {
			            super.handleMessage(msg);
	
			            switch (msg.what) {
			            case 1:
			            	Toast.makeText(Principal.myContext(), Principal.myContext().getString(R.string.error_fileCorrupt), Toast.LENGTH_LONG).show();
			            	break;
			            }
					}
				};
				handler.sendEmptyMessage(1);
			}
		} catch (FileNotFoundException e) {
			//En caso de que no se encuentre el archivo
			final Handler fnfHandler = new Handler(){
				@Override
				public void handleMessage(final Message msg) {
					super.handleMessage(msg);
					AlertDialog.Builder alert = new AlertDialog.Builder(Principal.myContext());
					alert.setTitle(Principal.myContext().getString(R.string.alert_md5NotFound));
					alert.setMessage(Principal.myContext().getString(R.string.alert_md5NotFoundSummary));
					alert.setPositiveButton(Principal.myContext().getString(R.string.yes), new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Si se selecciona "sí", aplicamos la actualización aún sin tener el md5
							aplicarUpdate(archivo, false);
						}
					});
					alert.setNegativeButton(Principal.myContext().getString(R.string.no), new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							acceptNoMD5 = false;
						}
					});
					alert.show();
				}
			};
			Thread fnfThread = new Thread(new Runnable() {
				@Override
				public void run() {
					fnfHandler.sendEmptyMessage(0);
				}
			});
			fnfThread.start();
			
			if(acceptNoMD5){
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
}
