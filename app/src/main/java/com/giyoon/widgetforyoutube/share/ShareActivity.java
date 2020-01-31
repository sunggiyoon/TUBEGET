package com.giyoon.widgetforyoutube.share;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.giyoon.widgetforyoutube.R;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
    }

    public void onCancelButonClicked(){
        finish();
    }
}
