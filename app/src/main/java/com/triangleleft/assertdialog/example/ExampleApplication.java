package com.triangleleft.assertdialog.example;

import com.triangleleft.assertdialog.AssertDialog;

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
