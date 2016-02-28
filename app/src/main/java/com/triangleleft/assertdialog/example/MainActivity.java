package com.triangleleft.assertdialog.example;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.concurrent.TimeoutException;

import static com.triangleleft.assertdialog.AssertDialog.assertEquals;
import static com.triangleleft.assertdialog.AssertDialog.assertTrue;
import static com.triangleleft.assertdialog.AssertDialog.fail;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonNormal = (Button) findViewById(R.id.button_normal);
        buttonNormal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                assertTrue("Condition is not true", false);
                assertTrue(false);
                assertEquals("2 != 3", 2, 3);
                assertEquals("test", "toast");
                assertEquals(2.0332, 2.0333, 0.0000001);
                fail("Click assert");
            }
        });
        Button buttonAsync = (Button) findViewById(R.id.button_async);
        buttonAsync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        fail("Fail in async");
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
                        fail("Handler fail");
                    }
                }, 1000);
            }
        });
        Button tryCatch = (Button) findViewById(R.id.button_function);
        tryCatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    testMethodThowsCheckedException();
                } catch (TimeoutException e) {
                    fail(e);
                }
            }
        });
    }

    private void testMethodThowsCheckedException() throws TimeoutException {
        throw new TimeoutException("Timeout!");
    }
}
