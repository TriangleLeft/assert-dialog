package com.triangleleft.assertdialog;

import org.junit.ComparisonFailure;

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
 * Shows alert dialog, blocking thread execution when assertion fails.
 * Based on JUnit Assert class.
 * As it's not possible to override static methods, I had to simple reuse source code.
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

    private AssertDialog() {
        // Static use only
    }

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
     * Asserts that a condition is true. If it isn't it throws an
     * {@link AssertionError} with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param condition condition to be checked
     */
    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    /**
     * Asserts that a condition is true. If it isn't it throws an
     * {@link AssertionError} without a message.
     *
     * @param condition condition to be checked
     */
    public static void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws an
     * {@link AssertionError} with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param condition condition to be checked
     */
    public static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws an
     * {@link AssertionError} without a message.
     *
     * @param condition condition to be checked
     */
    public static void assertFalse(boolean condition) {
        assertFalse(null, condition);
    }

    /**
     * Asserts that two objects are equal. If they are not, an
     * {@link AssertionError} is thrown with the given message. If
     * <code>expected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param expected expected value
     * @param actual actual value
     */
    public static void assertEquals(String message, Object expected,
                                    Object actual) {
        if (equalsRegardingNull(expected, actual)) {
            return;
        }
        if (expected instanceof String && actual instanceof String) {
            String cleanMessage = message == null ? "" : message;
            ComparisonFailure comparisonFailure = new ComparisonFailure(cleanMessage, (String) expected,
                    (String) actual);
            fail(comparisonFailure.getMessage());
        } else {
            failNotEquals(message, expected, actual);
        }
    }

    private static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        }

        return isEquals(expected, actual);
    }

    private static boolean isEquals(Object expected, Object actual) {
        return expected.equals(actual);
    }

    /**
     * Asserts that two objects are equal. If they are not, an
     * {@link AssertionError} without a message is thrown. If
     * <code>expected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     */
    public static void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two objects are <b>not</b> equals. If they are, an
     * {@link AssertionError} is thrown with the given message. If
     * <code>unexpected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param unexpected unexpected value to check
     * @param actual the value to check against <code>unexpected</code>
     */
    public static void assertNotEquals(String message, Object unexpected,
                                       Object actual) {
        if (equalsRegardingNull(unexpected, actual)) {
            failEquals(message, actual);
        }
    }

    /**
     * Asserts that two objects are <b>not</b> equals. If they are, an
     * {@link AssertionError} without a message is thrown. If
     * <code>unexpected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param unexpected unexpected value to check
     * @param actual the value to check against <code>unexpected</code>
     */
    public static void assertNotEquals(Object unexpected, Object actual) {
        assertNotEquals(null, unexpected, actual);
    }

    private static void failEquals(String message, Object actual) {
        String formatted = "Values should be different. ";
        if (message != null) {
            formatted = message + ". ";
        }

        formatted += "Actual: " + actual;
        fail(formatted);
    }

    /**
     * Asserts that two longs are <b>not</b> equals. If they are, an
     * {@link AssertionError} is thrown with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param unexpected unexpected value to check
     * @param actual the value to check against <code>unexpected</code>
     */
    public static void assertNotEquals(String message, long unexpected, long actual) {
        if (unexpected == actual) {
            failEquals(message, Long.valueOf(actual));
        }
    }

    /**
     * Asserts that two longs are <b>not</b> equals. If they are, an
     * {@link AssertionError} without a message is thrown.
     *
     * @param unexpected unexpected value to check
     * @param actual the value to check against <code>unexpected</code>
     */
    public static void assertNotEquals(long unexpected, long actual) {
        assertNotEquals(null, unexpected, actual);
    }

    /**
     * Asserts that two doubles are <b>not</b> equal to within a positive delta.
     * If they are, an {@link AssertionError} is thrown with the given
     * message. If the unexpected value is infinity then the delta value is
     * ignored. NaNs are considered equal:
     * <code>assertNotEquals(Double.NaN, Double.NaN, *)</code> fails
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param unexpected unexpected value
     * @param actual the value to check against <code>unexpected</code>
     * @param delta the maximum delta between <code>unexpected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertNotEquals(String message, double unexpected,
                                       double actual, double delta) {
        if (!doubleIsDifferent(unexpected, actual, delta)) {
            failEquals(message, Double.valueOf(actual));
        }
    }

    /**
     * Asserts that two doubles are <b>not</b> equal to within a positive delta.
     * If they are, an {@link AssertionError} is thrown. If the unexpected
     * value is infinity then the delta value is ignored.NaNs are considered
     * equal: <code>assertNotEquals(Double.NaN, Double.NaN, *)</code> fails
     *
     * @param unexpected unexpected value
     * @param actual the value to check against <code>unexpected</code>
     * @param delta the maximum delta between <code>unexpected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertNotEquals(double unexpected, double actual, double delta) {
        assertNotEquals(null, unexpected, actual, delta);
    }

    /**
     * Asserts that two floats are <b>not</b> equal to within a positive delta.
     * If they are, an {@link AssertionError} is thrown. If the unexpected
     * value is infinity then the delta value is ignored.NaNs are considered
     * equal: <code>assertNotEquals(Float.NaN, Float.NaN, *)</code> fails
     *
     * @param unexpected unexpected value
     * @param actual the value to check against <code>unexpected</code>
     * @param delta the maximum delta between <code>unexpected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertNotEquals(float unexpected, float actual, float delta) {
        assertNotEquals(null, unexpected, actual, delta);
    }

    /**
     * Asserts that two doubles are equal to within a positive delta.
     * If they are not, an {@link AssertionError} is thrown with the given
     * message. If the expected value is infinity then the delta value is
     * ignored. NaNs are considered equal:
     * <code>assertEquals(Double.NaN, Double.NaN, *)</code> passes
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param delta the maximum delta between <code>expected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertEquals(String message, double expected,
                                    double actual, double delta) {
        if (doubleIsDifferent(expected, actual, delta)) {
            failNotEquals(message, Double.valueOf(expected), Double.valueOf(actual));
        }
    }

    /**
     * Asserts that two floats are equal to within a positive delta.
     * If they are not, an {@link AssertionError} is thrown with the given
     * message. If the expected value is infinity then the delta value is
     * ignored. NaNs are considered equal:
     * <code>assertEquals(Float.NaN, Float.NaN, *)</code> passes
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param delta the maximum delta between <code>expected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertEquals(String message, float expected,
                                    float actual, float delta) {
        if (floatIsDifferent(expected, actual, delta)) {
            failNotEquals(message, Float.valueOf(expected), Float.valueOf(actual));
        }
    }

    /**
     * Asserts that two floats are <b>not</b> equal to within a positive delta.
     * If they are, an {@link AssertionError} is thrown with the given
     * message. If the unexpected value is infinity then the delta value is
     * ignored. NaNs are considered equal:
     * <code>assertNotEquals(Float.NaN, Float.NaN, *)</code> fails
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param unexpected unexpected value
     * @param actual the value to check against <code>unexpected</code>
     * @param delta the maximum delta between <code>unexpected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertNotEquals(String message, float unexpected,
                                       float actual, float delta) {
        if (!floatIsDifferent(unexpected, actual, delta)) {
            failEquals(message, actual);
        }
    }

    private static boolean doubleIsDifferent(double d1, double d2, double delta) {
        if (Double.compare(d1, d2) == 0) {
            return false;
        }
        if ((Math.abs(d1 - d2) <= delta)) {
            return false;
        }

        return true;
    }

    private static boolean floatIsDifferent(float f1, float f2, float delta) {
        if (Float.compare(f1, f2) == 0) {
            return false;
        }
        if ((Math.abs(f1 - f2) <= delta)) {
            return false;
        }

        return true;
    }

    /**
     * Asserts that two longs are equal. If they are not, an
     * {@link AssertionError} is thrown.
     *
     * @param expected expected long value.
     * @param actual actual long value
     */
    public static void assertEquals(long expected, long actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two longs are equal. If they are not, an
     * {@link AssertionError} is thrown with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param expected long expected value.
     * @param actual long actual value
     */
    public static void assertEquals(String message, long expected, long actual) {
        if (expected != actual) {
            failNotEquals(message, Long.valueOf(expected), Long.valueOf(actual));
        }
    }

    /**
     * Asserts that two doubles are equal to within a positive delta.
     * If they are not, an {@link AssertionError} is thrown. If the expected
     * value is infinity then the delta value is ignored.NaNs are considered
     * equal: <code>assertEquals(Double.NaN, Double.NaN, *)</code> passes
     *
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param delta the maximum delta between <code>expected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(null, expected, actual, delta);
    }

    /**
     * Asserts that two floats are equal to within a positive delta.
     * If they are not, an {@link AssertionError} is thrown. If the expected
     * value is infinity then the delta value is ignored. NaNs are considered
     * equal: <code>assertEquals(Float.NaN, Float.NaN, *)</code> passes
     *
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param delta the maximum delta between <code>expected</code> and
     * <code>actual</code> for which both numbers are still
     * considered equal.
     */
    public static void assertEquals(float expected, float actual, float delta) {
        assertEquals(null, expected, actual, delta);
    }

    /**
     * Asserts that an object isn't null. If it is an {@link AssertionError} is
     * thrown with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param object Object to check or <code>null</code>
     */
    public static void assertNotNull(String message, Object object) {
        assertTrue(message, object != null);
    }

    /**
     * Asserts that an object isn't null. If it is an {@link AssertionError} is
     * thrown.
     *
     * @param object Object to check or <code>null</code>
     */
    public static void assertNotNull(Object object) {
        assertNotNull(null, object);
    }

    /**
     * Asserts that an object is null. If it is not, an {@link AssertionError}
     * is thrown with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param object Object to check or <code>null</code>
     */
    public static void assertNull(String message, Object object) {
        if (object == null) {
            return;
        }
        failNotNull(message, object);
    }

    /**
     * Asserts that an object is null. If it isn't an {@link AssertionError} is
     * thrown.
     *
     * @param object Object to check or <code>null</code>
     */
    public static void assertNull(Object object) {
        assertNull(null, object);
    }

    private static void failNotNull(String message, Object actual) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }
        fail(formatted + "expected null, but was:<" + actual + ">");
    }

    /**
     * Asserts that two objects refer to the same object. If they are not, an
     * {@link AssertionError} is thrown with the given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param expected the expected object
     * @param actual the object to compare to <code>expected</code>
     */
    public static void assertSame(String message, Object expected, Object actual) {
        if (expected == actual) {
            return;
        }
        failNotSame(message, expected, actual);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not the
     * same, an {@link AssertionError} without a message is thrown.
     *
     * @param expected the expected object
     * @param actual the object to compare to <code>expected</code>
     */
    public static void assertSame(Object expected, Object actual) {
        assertSame(null, expected, actual);
    }

    /**
     * Asserts that two objects do not refer to the same object. If they do
     * refer to the same object, an {@link AssertionError} is thrown with the
     * given message.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param unexpected the object you don't expect
     * @param actual the object to compare to <code>unexpected</code>
     */
    public static void assertNotSame(String message, Object unexpected,
                                     Object actual) {
        if (unexpected == actual) {
            failSame(message);
        }
    }

    /**
     * Asserts that two objects do not refer to the same object. If they do
     * refer to the same object, an {@link AssertionError} without a message is
     * thrown.
     *
     * @param unexpected the object you don't expect
     * @param actual the object to compare to <code>unexpected</code>
     */
    public static void assertNotSame(Object unexpected, Object actual) {
        assertNotSame(null, unexpected, actual);
    }

    private static void failSame(String message) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }
        fail(formatted + "expected not same");
    }

    private static void failNotSame(String message, Object expected,
                                    Object actual) {
        String formatted = "";
        if (message != null) {
            formatted = message + " ";
        }
        fail(formatted + "expected same:<" + expected + "> was not:<" + actual
                + ">");
    }

    private static void failNotEquals(String message, Object expected,
                                      Object actual) {
        fail(format(message, expected, actual));
    }

    static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null && !"".equals(message)) {
            formatted = message + " ";
        }
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (expectedString.equals(actualString)) {
            return formatted + "expected: "
                    + formatClassAndValue(expected, expectedString)
                    + " but was: " + formatClassAndValue(actual, actualString);
        } else {
            return formatted + "expected:<" + expectedString + "> but was:<"
                    + actualString + ">";
        }
    }

    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }

    /**
     * This interface facilitates the use of expectThrows from Java 8. It allows method references
     * to void methods (that declare checked exceptions) to be passed directly into expectThrows
     * without wrapping. It is not meant to be implemented directly.
     *
     * @since 4.13
     */
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     * Asserts that {@code runnable} throws an exception of type {@code expectedThrowable} when
     * executed. If it does, the exception object is returned. If it does not throw an exception, an
     * {@link AssertionError} is thrown. If it throws the wrong type of exception, an {@code
     * AssertionError} is thrown describing the mismatch; the exception that was actually thrown can
     * be obtained by calling {@link AssertionError#getCause}.
     *
     * @param expectedThrowable the expected type of the exception
     * @param runnable       a function that is expected to throw an exception when executed
     * @since 4.13
     */
    public static void assertThrows(Class<? extends Throwable> expectedThrowable, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable actualThrown) {
            if (expectedThrowable.isInstance(actualThrown)) {
                return;
            } else {
                String mismatchMessage = format("unexpected exception type thrown;",
                        expectedThrowable.getSimpleName(), actualThrown.getClass().getSimpleName());

                fail(mismatchMessage, actualThrown);
                return;
            }
        }
        String message = String.format("expected %s to be thrown, but nothing was thrown",
                expectedThrowable.getSimpleName());
        fail(message);
    }


    /**
     * Fails a test with no message.
     */
    public static void fail() {
        fail(null, null);
    }

    /**
     * Display assert dialog with message.
     * @param message message to display
     */
    public static void fail(String message) {
        fail(message, null);
    }

    /**
     * Display assert dialog with message taken from throwable.
     * @param throwable throwable to display
     */
    public static void fail(Throwable throwable) {
        String message = null;
        if (throwable != null) {
            if (throwable.getMessage() != null) {
                message = throwable.getMessage();
            } else {
                message = throwable.getClass().getSimpleName();
            }
        }
        fail(message, throwable);
    }

    /**
     * Display assert dialog with message.
     * @param message message to display
     * @param throwable throwable to log
     */
    public static void fail(final String message, Throwable throwable) {
        if (sAppContext == null || sMode == null) {
            throw new IllegalStateException("You have to call init() first");
        }

        if (throwable == null) {
            throwable = new Throwable();
        }

        /**
         * If no message was passed, use default one.
         */
        if (message == null) {
            Log.wtf(TAG, sAppContext.getString(R.string.assert_fail), throwable);
        } else {
            Log.wtf(TAG, message, throwable);
        }

        switch (sMode) {
            case LOG:
                // Log only, we've already done it
                return;
            case THROW:
                // Throw error only
                throw new AssertionError(message);
            case DIALOG:
                // Do nothing, we would show dialog message below
                break;
            default:
                throw new IllegalStateException("Unknown mode " + sMode);
        }

        if (!prepareModal()) {
            throw new IllegalStateException("Failed to show dialog");
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
