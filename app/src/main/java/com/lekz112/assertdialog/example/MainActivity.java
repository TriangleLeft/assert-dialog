package com.lekz112.assertdialog.example;

import com.lekz112.assertdialog.AssertDialog;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonNormal = (Button) findViewById(R.id.button_normal);
        buttonNormal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AssertDialog.assertTrue("Condition is not true", false);
                AssertDialog.assertTrue(false);
                AssertDialog.assertEquals("2 != 3", 2, 3);
                AssertDialog.assertEquals("Expected", "Actual");
                AssertDialog.fail("Click assert");
            }
        });
        Button buttonAsync = (Button) findViewById(R.id.button_async);
        buttonAsync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        AssertDialog.fail("Fail in async");
                        return null;
                    }
                }.execute();
            }
        });
        Button buttonHandler = (Button) findViewById(R.id.button_handler);
        buttonHandler.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AssertDialog.fail("Handler fail");
                    }
                }, 1000);
            }
        });
    }
}
