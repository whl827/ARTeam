package com.google.ar.core.examples.java.helloar;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;

//<!--android:theme="@style/Theme.AppCompat.Light.NoActionBar">-->

public class InfoActivity extends AppCompatActivity{

    private TextView infoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        infoText = findViewById(R.id.infoID);

        String TAG = "MSG";
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(null);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#33000000")));

        Intent intent = getIntent();
        String message = intent.getStringExtra(HelloArActivity.EXTRA_MESSAGE);
        infoText.setText(message);

       // Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
