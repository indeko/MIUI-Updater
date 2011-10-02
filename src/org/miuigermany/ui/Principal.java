package org.miuigermany.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.json.JSONException;
import org.miuigermany.R;
import org.miuigermany.online.OnlineJSON;
import org.miuigermany.preferences.Ajustes;
import org.miuigermany.preferences.Configuracion;
import org.miuigermany.services.DownloadService;
import org.miuigermany.utils.ApplyUpdate;
import org.miuigermany.utils.Parche;
import org.miuigermany.utils.Update;

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
	
	private int YOURAPP_NOTIFICATION_ID = 9224;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
        	Toast not_sd = Toast.makeText(getBaseContext(), getString(R.string.error_couldNotAccessSD),Toast.LENGTH_SHORT);
        	not_sd.show();
        	finish();
        }else{
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.main);
	        viewNum = 0;
	        ImageView dot1 = (ImageView) findViewById(R.id.dot1);
	        setHighlighted(dot1);
	        Principal.context = Principal.this;
	        final SharedPreferences sp = getSharedPreferences("variables", MODE_PRIVATE);
	        String recovery = sp.getString("recovery","ninguno");
	        if(recovery.equals("ninguno")){
	        	CharSequence[] items = context.getResources().getStringArray(R.array.recoveries);
	        	final Editor editor = sp.edit();
	        	editor.putString("recovery", "cwm");
				editor.commit();
	        	AlertDialog.Builder dialogo = new AlertDialog.Builder(context);
	        	dialogo.setCancelable(false);
	        	dialogo.setTitle(getText(R.string.recoveryInUse));
	        	dialogo.setPositiveButton(getText(R.string.confirm), null);
	        	dialogo.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
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
	        	SharedPreferences.Editor editor = sp.edit();
	        	editor.putString("directorio", Configuracion.carpetaDescarga);
	        	editor.commit();
	        	directorio = sp.getString("directorio", "");
	        }
	        Button buscarParches = (Button) findViewById(R.id.check_parches);
	        buscarParches.setOnClickListener(checkUpdates);
	        final Spinner romsOffline = (Spinner) findViewById(R.id.romsOffline);
	        Button comprobarActualizaciones = (Button) findViewById(R.id.check_updates); 
	        comprobarActualizaciones.setOnClickListener(checkUpdates);
	        WebView wb = (WebView) findViewById(R.id.update_info);
	        wb.getSettings().setDefaultTextEncodingName("UTF-8");
	        wb.setBackgroundColor(0);
	        wb.loadUrl("file:///android_asset/no_info.html");
	        if(sp.getBoolean("check_init", false) == true){
	        	readJson();
	        }
	        File carpetaDestino = new File(directorio);
	        if(!carpetaDestino.exists()){
	        	carpetaDestino.mkdir();
	        }
	        final Button check_update_info = (Button) findViewById(R.id.chech_update_info);
	        check_update_info.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					readJson();
				}
			});
	        try {
				poblarRomsOffline(listado(carpetaDestino), romsOffline);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
	        BroadcastReceiver br = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent intent) {
					if(intent.getExtras().get("estado").equals("descargando")){
						check_update_info.setText(getString(R.string.downloading));
						check_update_info.setOnClickListener(null);
					}else if(intent.getExtras().get("estado").equals("finalizado")){
						File archivo = new File(directorio+intent.getStringExtra("archivo")+".part");
						if(archivo.exists()){
							archivo.renameTo(new File(directorio+intent.getStringExtra("archivo")));
						}
						check_update_info.setText(getString(R.string.applyUpdate));
						check_update_info.setOnClickListener(applyUpdate);
					}else if(intent.getExtras().get("estado").equals("error")){
						File archivo = new File(directorio+intent.getStringExtra("archivo")+".part");
						archivo.delete();
						check_update_info.setText(getString(R.string.update));
						check_update_info.setOnClickListener(downloadUpdate);
					}
				}
			};
			registerReceiver(br, new IntentFilter("descargando"));
        }
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
        	Toast.makeText(myContext(), getString(R.string.error_couldNotAccessSD),Toast.LENGTH_SHORT).show();
        	try {
				finalize();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    	SharedPreferences sp = getSharedPreferences("variables", MODE_PRIVATE);
    	directorio = sp.getString("directorio", "/sdcard/MIUI-Updater/");
    	md5 = sp.getBoolean("md5", true);
    	Spinner romsOffline = (Spinner) findViewById(R.id.romsOffline);
    	try {
			poblarRomsOffline(listado(new File(directorio)), romsOffline);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public File[] listado(File carpetaDestino){
    	File[] archivos = carpetaDestino.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.opciones:
            Intent i = new Intent(this,Ajustes.class);
            startActivity(i);
            return true;
        case R.id.sobre:
        	Intent j = new Intent(this,Sobre.class);
        	startActivity(j);
        	return true;
		default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void showNotification(int statusBarIconID, String text, String detailedText) {
        // This is who should be launched if the user selects our notification.
        Intent contentIntent = new Intent();
        contentIntent.setClass(this, Principal.class);
        // choose the ticker text
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), YOURAPP_NOTIFICATION_ID, contentIntent, 0);

        Notification notification = new Notification(
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
    
    public boolean checkConex(Context ctx){
        boolean bTieneConexion = false;
        ConnectivityManager connec =  (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        //Con esto recogemos todas las redes que tiene el móvil (wifi, gprs...)
        NetworkInfo[] redes = connec.getAllNetworkInfo();
       
        for(int i=0; i<2; i++){
            //Si alguna tiene conexión ponemos el boolean a true
            if (redes[i].getState() == NetworkInfo.State.CONNECTED){
                bTieneConexion = true;
            }
        }
        //Si el boolean sigue a false significa que no hay red disponible
       
        return bTieneConexion;
    }    
    
    static public void showToast(String msg){
    	Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
    	toast.show();
    }
    
    private OnClickListener checkUpdates = new OnClickListener(){

		@Override
		public void onClick(View v) {
			readJson();
		}
	};	
	
	private OnClickListener downloadUpdate = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent i = new Intent(Principal.this, DownloadService.class);
			Bundle bundle = new Bundle();
			if(tipoUpdate.equals("completo")){
				bundle.putString("nombre", versiones.get(nUpdate).getNombre());
				bundle.putString("archivo", versiones.get(nUpdate).getArchivo());
			}else{
				bundle.putString("nombre", parches.get(nPatch).getNombre());
				bundle.putString("archivo", parches.get(nPatch).getArchivo());
			}
			String[] mirrors = json.getMirrors();
			bundle.putStringArray("mirrors", mirrors);
			i.putExtras(bundle);
			startService(i);
		}
	};
	
	private OnClickListener applyUpdate = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(tipoUpdate.equals("completo")){
				ApplyUpdate.aplicarUpdate(directorio+versiones.get(nUpdate).getArchivo(),md5);
			}else{
				ApplyUpdate.aplicarUpdate(directorio+parches.get(nPatch).getArchivo(),md5);
			}
		}
	};
	
	private void readJson(){
		boolean conexion = checkConex(Principal.this);
		if(!conexion){
			Toast sinConexion = Toast.makeText(Principal.this, getString(R.string.error_connectionNotAvailable), Toast.LENGTH_SHORT);
			sinConexion.show();
		} else {
			final ProgressDialog buscando = ProgressDialog.show(Principal.this, getString(R.string.searchingForUpdates), getString(R.string.pleaseWait), true);
			buscando.setCancelable(true);
			final TextView ultimaVersionTitle = (TextView) findViewById(R.id.ultimaVersionTitle);
			final TextView ultimaVersion = (TextView) findViewById(R.id.ultimaVersion);
			
			final Handler handle = new Handler(){
				@Override
		        public void handleMessage(Message msg) {
		            super.handleMessage(msg);
		            Spinner romsOnline = (Spinner) findViewById(R.id.romsOnline);
		            Spinner parchesOnline = (Spinner) findViewById(R.id.parches);
		            switch (msg.what) {
		            case 1:
		            	buscando.dismiss();				
						ultimaVersion.setText(json.getLastVersion());					
						WebView wb = (WebView) findViewById(R.id.update_info);
						wb.getSettings().setDefaultTextEncodingName("UTF-8");
						TextView update_title = (TextView) findViewById(R.id.version_info);
						if(versiones.size() > 0){
							cambiarWebView(versiones.get(0).getDescUrl());
							update_title.setText(versiones.get(0).getNombre());
							ultimaVersionTitle.setVisibility(0);
							ultimaVersion.setVisibility(0);
						}
						Button check_update_info = (Button) findViewById(R.id.chech_update_info);
						
						romsOnline.setOnItemSelectedListener(new OnItemSelectedListener() {

							@Override
							public void onItemSelected(AdapterView<?> arg0,
									View arg1, int arg2, long arg3) {
								Log.d("MIUIESUPDATER","selected rom");
								tipoUpdate = "completo";
								nUpdate = arg2;
								cambiarSliding(tipoUpdate, arg2);
							}

							@Override
							public void onNothingSelected(AdapterView<?> arg0) {
								// TODO Auto-generated method stub
								
							}
						});
						
						parchesOnline.setOnItemSelectedListener(new OnItemSelectedListener() {

							@Override
							public void onItemSelected(AdapterView<?> arg0,
									View arg1, int arg2, long arg3) {
								Log.d("MIUIUPDATER","selected patch");
								tipoUpdate = "parche";
								nPatch = arg2;
								cambiarSliding(tipoUpdate, arg2);
							}

							@Override
							public void onNothingSelected(AdapterView<?> arg0) {
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
						} catch (JSONException e) {
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
			Thread thread = new Thread(new Runnable(){
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
	
	private void cambiarSliding(String tipo, int id){
		Button check_update_info = (Button) findViewById(R.id.chech_update_info);
		TextView update_title = (TextView) findViewById(R.id.version_info);
		WebView wb = (WebView) findViewById(R.id.update_info);
		if(tipo.equals("parche")){
			if(parches == null)
				return;
			update_title.setText(parches.get(id).getNombre());
			Log.d("MIUIUPDATER", parches.get(id).getArchivo());
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
	
	private void cambiarWebView(String linea){
		WebView wv = (WebView) findViewById(R.id.update_info);
		wv.getSettings().setDefaultTextEncodingName("UTF-8");
		if(linea.contains("url:")){
			wv.loadUrl(linea.substring(4));
		}else{
			wv.loadData(linea, "text/html", "UTF-8");
		}
	}
	
	public void poblarRomsOffline(File[] archivos, final Spinner spinner) throws JSONException{
		Button aplicar = (Button) findViewById(R.id.ApplyUpdate);
		if(archivos.length == 0){		
			spinner.setEnabled(false);
			spinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[0]));
			aplicar.setEnabled(false);
			aplicar.setOnClickListener(null);
		}else{
			String[] availableROMs =  new String[archivos.length];
			for(int i = 0; i < archivos.length;i++){
				availableROMs[i] = archivos[i].getName();
			}
			ArrayAdapter<String> ar = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableROMs);
			spinner.setAdapter(ar);
			spinner.setEnabled(true);
			aplicar.setEnabled(true);
			aplicar.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String archivo = (String) spinner.getSelectedItem();
					ApplyUpdate.aplicarUpdate(directorio+archivo,md5);
				}
			});
		}
	}
	
	public void poblarRomsOnline(final Spinner spinner) throws JSONException{
		Button aplicar = (Button) findViewById(R.id.chech_update_info);
		if(versiones.size() == 0){		
			spinner.setEnabled(false);
			aplicar.setEnabled(false);
			aplicar.setOnClickListener(null);
		}else{
			String[] availableROMs =  new String[versiones.size()];
			for(int i = 0; i < versiones.size();i++){
				availableROMs[i] = versiones.get(i).getNombre();
			}
			ArrayAdapter<String> ar = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableROMs);
			spinner.setAdapter(ar);
			spinner.setEnabled(true);
			spinner.setClickable(true);
			aplicar.setEnabled(true);
			aplicar.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String archivo = (String) spinner.getSelectedItem();
					ApplyUpdate.aplicarUpdate(directorio+archivo,md5);
				}
			});
		}
	}
	
	public void poblarParches(final Spinner spinner) throws JSONException{
		Button aplicar = (Button) findViewById(R.id.chech_update_info);
		if(parches.size() == 0){		
			spinner.setEnabled(false);
			aplicar.setEnabled(false);
			aplicar.setOnClickListener(null);
		}else{
			String[] availableROMs =  new String[parches.size()];
			for(int i = 0; i < parches.size();i++){
				availableROMs[i] = parches.get(i).getNombre();
			}
			ArrayAdapter<String> ar = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, availableROMs);
			spinner.setAdapter(ar);
			spinner.setEnabled(true);
			spinner.setClickable(true);
			aplicar.setEnabled(true);
			aplicar.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String archivo = (String) spinner.getSelectedItem();
					ApplyUpdate.aplicarUpdate(directorio+archivo,md5);
				}
			});
		}
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if((velocityY > 1500)||(velocityY < -1500)){
			return false;
		}
		ImageView dot1 = (ImageView) findViewById(R.id.dot1);
		ImageView dot2 = (ImageView) findViewById(R.id.dot2);
		ImageView dot3 = (ImageView) findViewById(R.id.dot3);
		ViewFlipper vf = (ViewFlipper) findViewById(R.id.switcher);
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
				SlidingDrawer sd = (SlidingDrawer) findViewById(R.id.slidingDrawer1);
				Animation slidOut = AnimationUtils.loadAnimation(context, R.anim.sliding_in);
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
				SlidingDrawer sd = (SlidingDrawer) findViewById(R.id.slidingDrawer1);
				Animation slidOut = AnimationUtils.loadAnimation(context, R.anim.sliding_out);
				slidOut.setFillAfter(true);
				sd.startAnimation(slidOut);
				sd.scrollTo(0, -600);
			}
			if(parches == null){
				viewNum++;
				return true;
			}
			if((viewNum == 0)&&(parches.size() > 0)){
				Log.d("MIUIUPDATER","changing patch");
				cambiarSliding("parche", nPatch);
			}
			viewNum++;
		}
		return false;
	}

	private void setHighlighted(ImageView dot){
		dot.setImageDrawable(getResources().getDrawable(R.drawable.workspace_seekpoint_highlight));
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
		lp.gravity = Gravity.CENTER_VERTICAL;
		dot.setLayoutParams(lp);
		dot.setMinimumWidth(16);
		dot.setMinimumHeight(16);
		dot.setScaleType(ImageView.ScaleType.FIT_START);
	}
	
	private void setNotHighLighted(ImageView dot){
		dot.setImageDrawable(getResources().getDrawable(R.drawable.workspace_seekpoint_normal));
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
		lp.gravity = Gravity.CENTER_VERTICAL;
		dot.setLayoutParams(lp);
		dot.setMinimumWidth(13);
		dot.setMinimumHeight(13);
		dot.setScaleType(ImageView.ScaleType.CENTER);
	}
	
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
            return gesturedetector.onTouchEvent(event);
    }
}