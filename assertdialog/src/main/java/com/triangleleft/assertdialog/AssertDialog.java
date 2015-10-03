package com.triangleleft.assertdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
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
    private static Field sMsgTargetField;
    private static boolean sQuitModal;
    private static Context sAppContext;
    private static AssertMode sMode;
    private static Method sMsgRecycleUnchecked;

    /**
     * Init assert dialog.
     *
     * @param mode    work mode
     * @param context context to create dialog from.
     */
    public static void init(AssertMode mode, Context context) {
        sAppContext = context;
        sMode = mode;
    }

    /**
     * Assert that to objects are equal.
     */
    public static void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Assert that object are equal with custom assertion message.
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
     */
    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    /**
     * Display assert dialog with message.
     */
    public static void fail(final String message) {
        if (message == null) {
            Log.wtf(TAG, sAppContext.getString(R.string.assert_fail), new Throwable());
        } else {
            Log.wtf(TAG, message, new Throwable());
        }

        switch (sMode) {
            case LOG:
                return;
            case THROW:
                throw new AssertionError(message);
            case DIALOG:
            default:
                break;
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
                builder.setTitle(R.string.assert_fail);
                builder.setMessage(message);
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
            sMsgQueueNextMethod = clsMsgQueue.getDeclaredMethod("next");
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }

        sMsgQueueNextMethod.setAccessible(true);

        try {
            sMsgTargetField = clsMessage.getDeclaredField("target");
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        }

        sMsgTargetField.setAccessible(true);

        // Starting from lollipop we have to use unchecked version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            try {
                sMsgRecycleUnchecked = clsMessage.getDeclaredMethod("recycleUnchecked");
            } catch (SecurityException | NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            }

            sMsgRecycleUnchecked.setAccessible(true);
        }

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
                msg = (Message) sMsgQueueNextMethod.invoke(queue);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            if (msg != null) {
                Handler target = null;
                try {
                    target = (Handler) sMsgTargetField.get(msg);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                if (target == null) {
                    // No target is a magic identifier for the quit message.
                    sQuitModal = true;
                    break;
                }

                target.dispatchMessage(msg);
                // Starting from lollipop we have to use unchecked version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        sMsgRecycleUnchecked.invoke(msg);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } else {
                    msg.recycle();
                }
            }
        }
    }

    /**
     * Work mode. Defines what to do in case that assertion fails.
     */
    public enum AssertMode {
        /**
         * Log assert to Log.wtf
         */
        LOG,
        /**
         * Show dialog, blocking current thread.
         */
        DIALOG,
        /**
         * Throw AssertionException
         */
        THROW
    }
}
