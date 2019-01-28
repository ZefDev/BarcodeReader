package com.example.z;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;

public class WebPage extends Activity {
    private WebView webView;
    private Button imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_page);
        webView = (WebView) findViewById(R.id.webView);
        // включаем поддержку JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        // указываем страницу загрузки
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl(getIntent().getStringExtra("link"));
        imageButton = (Button) findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}