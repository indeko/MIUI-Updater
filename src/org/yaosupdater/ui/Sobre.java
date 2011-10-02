package org.yaosupdater.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;

import de.sUpdater.R;

public class Sobre extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sobre);
        WebView wv = (WebView) findViewById(R.id.webView1);
        wv.getSettings().setDefaultTextEncodingName("UTF-8");
        wv.setBackgroundColor(0);
        wv.loadUrl("file:///android_asset/about.html");
	}
}
