package com.lekz112.assertdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * Behaves same as JUnit Assert class.
 * Shows alert dialog, blocking thread execution when assertion fails.
 * Based on http://stackoverflow.com/questions/6120567/android-how-to-get-a-modal-dialog-or-similar-modal-behavior/6198192#6198192
 */
public class AssertDialog {

    private static final String TAG = AssertDialog.class.getSimpleName();
    private static Method sMsgQueueNextMethod;
    private static Field sMsgTargetFiled;
    private static boolean sQuitModal;
    private static Context sAppContext;
    private static boolean sDebug;

    /**
     * Init assert dialog.
     * If debug is enabled, shows dialog during assertion fails, otherwise silently logs it.
     *
     * @param debug   debug mode
     * @param context context to create dialog from.
     */
    public static void init(boolean debug, Context context) {
        sAppContext = context;
        sDebug = debug;
    }

    /**
     * Assert that to objects are equal.
     * @param expected
     * @param actual
     */
    public static void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Assert that object are equal with custom assertion message.
     * @param message
     * @param expected
     * @param actual
     */
    public static void assertEquals(String message, Object expected, Object actual) {
        boolean condition;
        if (expected == null) {
            condition = actual == null;
        } else {
            condition = expected.equals(actual);
        }
        assertTrue(message, condition);
    }

    /**
     * Assert that condition is true.
     */
    public static void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    /**
     * Assert that condition is true with custom assert message.
     * @param message
     * @param condition
     */
    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    /**
     * Display assert dialog with message.
     */
    public static void fail(String message) {
        final String logMessage;
        if (message == null) {
            logMessage = sAppContext.getString(R.string.assert_fail);
        } else {
            logMessage = message;
        }

        Log.wtf(TAG, logMessage, new Throwable());

        // Don't show any dialogs in release version
        if (!sDebug) {
            return;
        }

        if (!prepareModal()) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                // build alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(sAppContext);
                builder.setMessage(logMessage);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.button_continue,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.wtf(TAG, sAppContext.getString(R.string.selected_option,
                                        sAppContext.getString(R.string.button_continue)));
                                sQuitModal = true;
                                dialog.dismiss();
                                latch.countDown();
                            }
                        });
                builder.setNegativeButton(R.string.button_stop, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.wtf(TAG, sAppContext.getString(R.string.selected_option,
                                sAppContext.getString(R.string.button_stop)));
                        // Stop whole application
                        System.exit(1);
                        // We probably don't need this, but still.
                        latch.countDown();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();
            }
        };

        // We can show dialogs only on main thread
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            // Ui thread, just run runnable
            runnable.run();
            // and "Block" ui thread
            doModal();
        } else {
            // We need to execute it on main thread
            new Handler(Looper.getMainLooper()).post(runnable);
            // Now halt execution until dialog button is pressed
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean prepareModal() {
        Class<?> clsMsgQueue;
        Class<?> clsMessage;

        try {
            clsMsgQueue = Class.forName("android.os.MessageQueue");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        try {
            clsMessage = Class.forName("android.os.Message");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        try {
            sMsgQueueNextMethod = clsMsgQueue.getDeclaredMethod("next", new Class[]{});
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }

        sMsgQueueNextMethod.setAccessible(true);

        try {
            sMsgTargetFiled = clsMessage.getDeclaredField("target");
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        }

        sMsgTargetFiled.setAccessible(true);
        return true;
    }

    private static void doModal() {
        sQuitModal = false;

        // get message queue associated with main UI thread
        MessageQueue queue = Looper.myQueue();
        while (!sQuitModal) {
            // call queue.next(), might block
            Message msg = null;
            try {
                msg = (Message) sMsgQueueNextMethod.invoke(queue, new Object[]{});
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            if (msg != null) {
                Handler target = null;
                try {
                    target = (Handler) sMsgTargetFiled.get(msg);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                if (target == null) {
                    // No target is a magic identifier for the quit message.
                    sQuitModal = true;
                    break;
                }

                target.dispatchMessage(msg);
                msg.recycle();
            }
        }
    }
}
