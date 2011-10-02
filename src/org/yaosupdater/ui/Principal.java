package org.yaosupdater.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.json.JSONException;
import de.sUpdater.R;
import org.yaosupdater.online.OnlineJSON;
import org.yaosupdater.preferences.Ajustes;
import org.yaosupdater.preferences.Configuracion;
import org.yaosupdater.services.DownloadService;
import org.yaosupdater.utils.ApplyUpdate;
import org.yaosupdater.utils.Parche;
import org.yaosupdater.utils.Update;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class Principal extends Activity implements OnGestureListener {
	
	private final int YOURAPP_NOTIFICATION_ID = 9224;
	private NotificationManager notify;
	private static Context context;
	ArrayList<Update> versiones = new ArrayList<Update>();
	ArrayList<Parche> parches = new ArrayList<Parche>();
	private String directorio;
	OnlineJSON json;
	private GestureDetector gesturedetector;
	private int viewNum;
	private boolean md5;
	private String tipoUpdate = "completo";
	private int nUpdate = 0;
	private int nPatch = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
        	final Toast not_sd = Toast.makeText(getBaseContext(), getString(R.string.error_couldNotAccessSD),Toast.LENGTH_SHORT);
        	not_sd.show();
        	finish();
        }else{
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.main);
	        viewNum = 0;
	        final ImageView dot1 = (ImageView) findViewById(R.id.dot1);
	        setHighlighted(dot1);
	        Principal.context = Principal.this;
	        final SharedPreferences sp = getSharedPreferences("variables", MODE_PRIVATE);
	        final String recovery = sp.getString("recovery","ninguno");
	        if(recovery.equals("ninguno")){
	        	final CharSequence[] items = context.getResources().getStringArray(R.array.recoveries);
	        	final Editor editor = sp.edit();
	        	editor.putString("recovery", "cwm");
				editor.commit();
	        	final AlertDialog.Builder dialogo = new AlertDialog.Builder(context);
	        	dialogo.setCancelable(false);
	        	dialogo.setTitle(getText(R.string.recoveryInUse));
	        	dialogo.setPositiveButton(getText(R.string.confirm), null);
	        	dialogo.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
					
					
					public void onClick(final DialogInterface dialog, final int which) {
						
						switch(which){
						case(0):
							editor.putString("recovery", "cwm");
							editor.commit();
						break;
						case(1):
							editor.putString("recovery", "amonra");
							editor.commit();
						break;
						}
					}
				});
	        	dialogo.show();
	        }
	        md5 = sp.getBoolean("md5", true);
	        gesturedetector = new GestureDetector(this, this);
	        directorio = sp.getString("directorio", "");
	        if(directorio.equals("")){
	        	final SharedPreferences.Editor editor = sp.edit();
	        	editor.putString("directorio", Configuracion.carpetaDescarga);
	        	editor.commit();
	        	directorio = sp.getString("directorio", "");
	        }
	        final Button buscarParches = (Button) findViewById(R.id.check_parches);
	        buscarParches.setOnClickListener(checkUpdates);
	        final Spinner romsOffline = (Spinner) findViewById(R.id.romsOffline);
	        final Button comprobarActualizaciones = (Button) findViewById(R.id.check_updates); 
	        comprobarActualizaciones.setOnClickListener(checkUpdates);
	        final WebView wb = (WebView) findViewById(R.id.update_info);
	        wb.getSettings().setDefaultTextEncodingName("UTF-8");
	        wb.setBackgroundColor(0);
	        wb.loadUrl("file:///android_asset/no_info.html");
	        if(sp.getBoolean("check_init", false) == true){
	        	readJson();
	        }
	        final File carpetaDestino = new File(directorio);
	        if(!carpetaDestino.exists()){
	        	carpetaDestino.mkdir();
	        }
	        final Button check_update_info = (Button) findViewById(R.id.chech_update_info);
	        check_update_info.setOnClickListener(new OnClickListener() {
				
				
				public void onClick(final View v) {
					readJson();
				}
			});
	        try {
				poblarRomsOffline(listado(carpetaDestino), romsOffline);
			} catch (final JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
	        final BroadcastReceiver br = new BroadcastReceiver() {
				
				
				public void onReceive(final Context context, final Intent intent) {
					if(intent.getExtras().get("estado").equals("descargando")){
						check_update_info.setText(getString(R.string.downloading));
						check_update_info.setOnClickListener(null);
					}else if(intent.getExtras().get("estado").equals("finalizado")){
						final File archivo = new File(directorio+intent.getStringExtra("archivo")+".part");
						if(archivo.exists()){
							archivo.renameTo(new File(directorio+intent.getStringExtra("archivo")));
						}
						check_update_info.setText(getString(R.string.applyUpdate));
						check_update_info.setOnClickListener(applyUpdate);
					}else if(intent.getExtras().get("estado").equals("error")){
						final File archivo = new File(directorio+intent.getStringExtra("archivo")+".part");
						archivo.delete();
						check_update_info.setText(getString(R.string.update));
						check_update_info.setOnClickListener(downloadUpdate);
					}
				}
			};
			registerReceiver(br, new IntentFilter("descargando"));
        }
    }
    
    
    public void onResume(){
    	super.onResume();
    	if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
        	Toast.makeText(myContext(), getString(R.string.error_couldNotAccessSD),Toast.LENGTH_SHORT).show();
        	try {
				finalize();
			} catch (final Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    	final SharedPreferences sp = getSharedPreferences("variables", MODE_PRIVATE);
    	directorio = sp.getString("directorio", "/sdcard/MIUIEsUpdater/");
    	md5 = sp.getBoolean("md5", true);
    	final Spinner romsOffline = (Spinner) findViewById(R.id.romsOffline);
    	try {
			poblarRomsOffline(listado(new File(directorio)), romsOffline);
		} catch (final JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public File[] listado(final File carpetaDestino){
    	final File[] archivos = carpetaDestino.listFiles(new FilenameFilter() {
			
			
			public boolean accept(final File dir, final String filename) {
				if(filename.endsWith(".zip")){
					return true;
				}
				return false;
			}
		});
    	return archivos;
    }
    
    public static Context myContext(){
    	return context;
    }
    
    
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.opciones:
            final Intent i = new Intent(this,Ajustes.class);
            startActivity(i);
            return true;
        case R.id.sobre:
        	final Intent j = new Intent(this,Sobre.class);
        	startActivity(j);
        	return true;
		default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void showNotification(final int statusBarIconID, final String text, final String detailedText) {
        // This is who should be launched if the user selects our notification.
        final Intent contentIntent = new Intent();
        contentIntent.setClass(this, Principal.class);
        // choose the ticker text
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), YOURAPP_NOTIFICATION_ID, contentIntent, 0);

        final Notification notification = new Notification(
                statusBarIconID,             // the icon for the status bar
                text,                  // the text to display in the ticker
                System.currentTimeMillis());  // the appIntent (see above)
        notification.setLatestEventInfo(Principal.this, text, detailedText, pendingIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notify.notify(
                   YOURAPP_NOTIFICATION_ID, // we use a string id because it is a unique
                                                      // number.  we use it later to cancel the
                       // notification
                   notification);
        
    }
    
    public boolean checkConex(final Context ctx){
        boolean bTieneConexion = false;
        final ConnectivityManager connec =  (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        //Con esto recogemos todas las redes que tiene el móvil (wifi, gprs...)
        final NetworkInfo[] redes = connec.getAllNetworkInfo();
       
        for(int i=0; i<2; i++){
            //Si alguna tiene conexión ponemos el boolean a true
            if (redes[i].getState() == NetworkInfo.State.CONNECTED){
                bTieneConexion = true;
            }
        }
        //Si el boolean sigue a false significa que no hay red disponible
       
        return bTieneConexion;
    }    
    
    static public void showToast(final String msg){
    	final Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
    	toast.show();
    }
    
    private final OnClickListener checkUpdates = new OnClickListener(){

		
		public void onClick(final View v) {
			readJson();
		}
	};	
	
	private final OnClickListener downloadUpdate = new OnClickListener() {
		
		
		public void onClick(final View v) {
			final Intent i = new Intent(Principal.this, DownloadService.class);
			final Bundle bundle = new Bundle();
			if(tipoUpdate.equals("completo")){
				bundle.putString("nombre", versiones.get(nUpdate).getNombre());
				bundle.putString("archivo", versiones.get(nUpdate).getArchivo());
			}else{
				bundle.putString("nombre", parches.get(nPatch).getNombre());
				bundle.putString("archivo", parches.get(nPatch).getArchivo());
			}
			final String[] mirrors = json.getMirrors();
			bundle.putStringArray("mirrors", mirrors);
			i.putExtras(bundle);
			startService(i);
		}
	};
	
	private final OnClickListener applyUpdate = new OnClickListener(){

		
		public void onClick(final View v) {
			if(tipoUpdate.equals("completo")){
				ApplyUpdate.aplicarUpdate(directorio+versiones.get(nUpdate).getArchivo(),md5);
			}else{
				ApplyUpdate.aplicarUpdate(directorio+parches.get(nPatch).getArchivo(),md5);
			}
		}
	};
	
	private void readJson(){
		final boolean conexion = checkConex(Principal.this);
		if(!conexion){
			final Toast sinConexion = Toast.makeText(Principal.this, getString(R.string.error_connectionNotAvailable), Toast.LENGTH_SHORT);
			sinConexion.show();
		} else {
			final ProgressDialog buscando = ProgressDialog.show(Principal.this, getString(R.string.searchingForUpdates), getString(R.string.pleaseWait), true);
			buscando.setCancelable(true);
			final TextView ultimaVersionTitle = (TextView) findViewById(R.id.ultimaVersionTitle);
			final TextView ultimaVersion = (TextView) findViewById(R.id.ultimaVersion);
			
			final Handler handle = new Handler(){
				
		        public void handleMessage(final Message msg) {
		            super.handleMessage(msg);
		            final Spinner romsOnline = (Spinner) findViewById(R.id.romsOnline);
		            final Spinner parchesOnline = (Spinner) findViewById(R.id.parches);
		            switch (msg.what) {
		            case 1:
		            	buscando.dismiss();				
						ultimaVersion.setText(json.getLastVersion());					
						final WebView wb = (WebView) findViewById(R.id.update_info);
						wb.getSettings().setDefaultTextEncodingName("UTF-8");
						final TextView update_title = (TextView) findViewById(R.id.version_info);
						if(versiones.size() > 0){
							cambiarWebView(versiones.get(0).getDescUrl());
							update_title.setText(versiones.get(0).getNombre());
							ultimaVersionTitle.setVisibility(0);
							ultimaVersion.setVisibility(0);
						}
						final Button check_update_info = (Button) findViewById(R.id.chech_update_info);
						
						romsOnline.setOnItemSelectedListener(new OnItemSelectedListener() {

							
							public void onItemSelected(final AdapterView<?> arg0,
									final View arg1, final int arg2, final long arg3) {
								Log.d("MIUIESUPDATER","selected rom");
								tipoUpdate = "completo";
								nUpdate = arg2;
								cambiarSliding(tipoUpdate, arg2);
							}

							
							public void onNothingSelected(final AdapterView<?> arg0) {
								// TODO Auto-generated method stub
								
							}
						});
						
						parchesOnline.setOnItemSelectedListener(new OnItemSelectedListener() {

							
							public void onItemSelected(final AdapterView<?> arg0,
									final View arg1, final int arg2, final long arg3) {
								Log.d("MIUIESUPDATER","selected patch");
								tipoUpdate = "parche";
								nPatch = arg2;
								cambiarSliding(tipoUpdate, arg2);
							}

							
							public void onNothingSelected(final AdapterView<?> arg0) {
								// TODO Auto-generated method stub
								
							}
						});
						try {
							if(versiones.size() > 0)
								poblarRomsOnline((Spinner) findViewById(R.id.romsOnline));
							else{
								romsOnline.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item));
								romsOnline.setEnabled(false);
							}
							if(parches.size() > 0)
								poblarParches(parchesOnline);
							else{
								parchesOnline.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item));
								parchesOnline.setEnabled(false);
							}
						} catch (final JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(versiones.size() > 0){
							if(new File(directorio+versiones.get(0).getArchivo()).exists()){
					        	check_update_info.setText(getString(R.string.applyUpdate));
					        	check_update_info.setOnClickListener(applyUpdate);
					        }else{
					        	check_update_info.setText(getString(R.string.update));
					        	check_update_info.setOnClickListener(downloadUpdate);
					        }
						}
						break;
		            case 2:
		            	showToast(getString(R.string.error_connectionProblemJson));
		            	buscando.dismiss();
		            	romsOnline.setEnabled(false);
		            	romsOnline.setOnItemSelectedListener(null);
		            	romsOnline.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[0]));
		            	parchesOnline.setEnabled(false);
		            	parchesOnline.setOnItemSelectedListener(null);
		            	parchesOnline.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[0]));
		            	break;
		            case 3:
		            	showToast(getString(R.string.updatesNotFound));
		            	buscando.dismiss();
		            	romsOnline.setEnabled(false);
		            	romsOnline.setOnItemSelectedListener(null);
		            	romsOnline.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[0]));
		            	parchesOnline.setEnabled(false);
		            	parchesOnline.setOnItemSelectedListener(null);
		            	parchesOnline.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[0]));
		            	break;
		            }   
		        }
			};
			final Thread thread = new Thread(new Runnable(){
					public void run() {
						json = new OnlineJSON();
						versiones = json.getVersiones();
						parches = json.getParches();
						if(versiones == null){
							handle.sendEmptyMessage(2);
						}else{
							if(!versiones.isEmpty()||!parches.isEmpty()){
								/* notify = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
								showNotification(R.drawable.icon, "¡Actualización encontrada!", "Se ha encontrado una actualización de la ROM");*/
								handle.sendEmptyMessage(1);
							}else{
								handle.sendEmptyMessage(3);
							}
						}
					}
				
			});
			thread.start();
		}
	}
	
	private void cambiarSliding(final String tipo, final int id){
		final Button check_update_info = (Button) findViewById(R.id.chech_update_info);
		final TextView update_title = (TextView) findViewById(R.id.version_info);
		final WebView wb = (WebView) findViewById(R.id.update_info);
		if(tipo.equals("parche")){
			if(parches == null)
				return;
			update_title.setText(parches.get(id).getNombre());
			Log.d("YAOSUPDATER", parches.get(id).getArchivo());
			cambiarWebView(parches.get(id).getDescUrl());
			if(new File(directorio+parches.get(id).getArchivo()).exists()){
	        	check_update_info.setText(getString(R.string.applyUpdate));
	        	check_update_info.setOnClickListener(applyUpdate);
	        }else{
	        	check_update_info.setText(getString(R.string.update));
	        	check_update_info.setOnClickListener(downloadUpdate);
	        }
		}else{
			if(versiones == null)
				return;
			update_title.setText(versiones.get(id).getNombre());
			cambiarWebView(versiones.get(id).getDescUrl());
			if(new File(directorio+versiones.get(id).getArchivo()).exists()){
	        	check_update_info.setText(getString(R.string.update));
	        	check_update_info.setOnClickListener(applyUpdate);
	        }else{
	        	check_update_info.setText(getString(R.string.update));
	        	check_update_info.setOnClickListener(downloadUpdate);
	        }
		}	
	}
	
	private void cambiarWebView(final String linea){
		final WebView wv = (WebView) findViewById(R.id.update_info);
		wv.getSettings().setDefaultTextEncodingName("UTF-8");
		if(linea.contains("url:")){
			wv.loadUrl(linea.substring(4));
		}else{
			wv.loadData(linea, "text/html", "UTF-8");
		}
	}
	
	public void poblarRomsOffline(final File[] archivos, final Spinner spinner) throws JSONException{
		final Button aplicar = (Button) findViewById(R.id.ApplyUpdate);
		if(archivos.length == 0){		
			spinner.setEnabled(false);
			spinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[0]));
			aplicar.setEnabled(false);
			aplicar.setOnClickListener(null);
		}else{
			final String[] availableROMs =  new String[archivos.length];
			for(int i = 0; i < archivos.length;i++){
				availableROMs[i] = archivos[i].getName();
			}
			final ArrayAdapter<String> ar = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableROMs);
			spinner.setAdapter(ar);
			spinner.setEnabled(true);
			aplicar.setEnabled(true);
			aplicar.setOnClickListener(new OnClickListener() {
				
				
				public void onClick(final View v) {
					final String archivo = (String) spinner.getSelectedItem();
					ApplyUpdate.aplicarUpdate(directorio+archivo,md5);
				}
			});
		}
	}
	
	public void poblarRomsOnline(final Spinner spinner) throws JSONException{
		final Button aplicar = (Button) findViewById(R.id.chech_update_info);
		if(versiones.size() == 0){		
			spinner.setEnabled(false);
			aplicar.setEnabled(false);
			aplicar.setOnClickListener(null);
		}else{
			final String[] availableROMs =  new String[versiones.size()];
			for(int i = 0; i < versiones.size();i++){
				availableROMs[i] = versiones.get(i).getNombre();
			}
			final ArrayAdapter<String> ar = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableROMs);
			spinner.setAdapter(ar);
			spinner.setEnabled(true);
			spinner.setClickable(true);
			aplicar.setEnabled(true);
			aplicar.setOnClickListener(new OnClickListener() {
				
				
				public void onClick(final View v) {
					final String archivo = (String) spinner.getSelectedItem();
					ApplyUpdate.aplicarUpdate(directorio+archivo,md5);
				}
			});
		}
	}
	
	public void poblarParches(final Spinner spinner) throws JSONException{
		final Button aplicar = (Button) findViewById(R.id.chech_update_info);
		if(parches.size() == 0){		
			spinner.setEnabled(false);
			aplicar.setEnabled(false);
			aplicar.setOnClickListener(null);
		}else{
			final String[] availableROMs =  new String[parches.size()];
			for(int i = 0; i < parches.size();i++){
				availableROMs[i] = parches.get(i).getNombre();
			}
			final ArrayAdapter<String> ar = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableROMs);
			spinner.setAdapter(ar);
			spinner.setEnabled(true);
			spinner.setClickable(true);
			aplicar.setEnabled(true);
			aplicar.setOnClickListener(new OnClickListener() {
				
				
				public void onClick(final View v) {
					final String archivo = (String) spinner.getSelectedItem();
					ApplyUpdate.aplicarUpdate(directorio+archivo,md5);
				}
			});
		}
	}
	
	
	public boolean onDown(final MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX,
			final float velocityY) {
		if((velocityY > 1500)||(velocityY < -1500)){
			return false;
		}
		final ImageView dot1 = (ImageView) findViewById(R.id.dot1);
		final ImageView dot2 = (ImageView) findViewById(R.id.dot2);
		final ImageView dot3 = (ImageView) findViewById(R.id.dot3);
		final ViewFlipper vf = (ViewFlipper) findViewById(R.id.switcher);
		if((velocityX > 400)&&(viewNum > 0)){
			vf.setInAnimation(this,R.anim.in_animation);
			vf.setOutAnimation(this,R.anim.out_animation);
			vf.showPrevious();
			if(viewNum == 1){
				setHighlighted(dot1);
				setNotHighLighted(dot2);
				setNotHighLighted(dot3);
			}else if(viewNum == 2){
				setNotHighLighted(dot1);
				setHighlighted(dot2);
				setNotHighLighted(dot3);
				final SlidingDrawer sd = (SlidingDrawer) findViewById(R.id.slidingDrawer1);
				final Animation slidOut = AnimationUtils.loadAnimation(context, R.anim.sliding_in);
				slidOut.setFillAfter(true);
				sd.startAnimation(slidOut);
				sd.scrollTo(0, 0);
			}
			if(versiones == null){
				viewNum--;
				return true;
			}
			if((viewNum == 1)&&(versiones.size() > 0)){
				cambiarSliding("completa", nUpdate);
			}
			viewNum--;
		}else if((velocityX < -400)&&(viewNum < 2)){
			vf.setInAnimation(this,R.anim.in_animation1);
			vf.setOutAnimation(this,R.anim.out_animation1); 
			vf.showNext();
			if(viewNum == 0){
				setNotHighLighted(dot1);
				setHighlighted(dot2);
				setNotHighLighted(dot3);
			}else if(viewNum == 1){
				setNotHighLighted(dot1);
				setNotHighLighted(dot2);
				setHighlighted(dot3);
				final SlidingDrawer sd = (SlidingDrawer) findViewById(R.id.slidingDrawer1);
				final Animation slidOut = AnimationUtils.loadAnimation(context, R.anim.sliding_out);
				slidOut.setFillAfter(true);
				sd.startAnimation(slidOut);
				sd.scrollTo(0, -600);
			}
			if(parches == null){
				viewNum++;
				return true;
			}
			if((viewNum == 0)&&(parches.size() > 0)){
				Log.d("MIUIESUPDATER","cambiando a parche");
				cambiarSliding("parche", nPatch);
			}
			viewNum++;
		}
		return false;
	}

	private void setHighlighted(final ImageView dot){
		dot.setImageDrawable(getResources().getDrawable(R.drawable.workspace_seekpoint_highlight));
		final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
		lp.gravity = Gravity.CENTER_VERTICAL;
		dot.setLayoutParams(lp);
		dot.setMinimumWidth(16);
		dot.setMinimumHeight(16);
		dot.setScaleType(ImageView.ScaleType.FIT_START);
	}
	
	private void setNotHighLighted(final ImageView dot){
		dot.setImageDrawable(getResources().getDrawable(R.drawable.workspace_seekpoint_normal));
		final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
		lp.gravity = Gravity.CENTER_VERTICAL;
		dot.setLayoutParams(lp);
		dot.setMinimumWidth(13);
		dot.setMinimumHeight(13);
		dot.setScaleType(ImageView.ScaleType.CENTER);
	}
	
	
	public void onLongPress(final MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
			final float distanceY) {
		return false;
	}

	
	public void onShowPress(final MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	public boolean onSingleTapUp(final MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
    public boolean onTouchEvent(final MotionEvent event) {
            return gesturedetector.onTouchEvent(event);
    }
}