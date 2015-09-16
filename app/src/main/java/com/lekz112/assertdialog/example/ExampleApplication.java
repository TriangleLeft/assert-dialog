package com.lekz112.assertdialog.example;

import com.lekz112.assertdialog.AssertDialog;

import android.app.Application;

/**
 * Application class, used to init AssertDialog.
 */
public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AssertDialog.init(true, getApplicationContext());
    }
}
