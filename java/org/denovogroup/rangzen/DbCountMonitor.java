package org.denovogroup.rangzen;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for notifying listeners when changes occur.
 */
public class DbCountMonitor {

    /** Handler to the Android main thread */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** List of db-listeners */
    private final List<DbListener> mDbListeners = new ArrayList<>();

    /**
     * Add a CursorListener to be notified when the DB is changed, this call must be
     * done on the main thread.
     *
     * @param dbListener
     */
    public void addListener(final DbListener dbListener) {
        if (!isOnMainThread()) {
            throw new RuntimeException("listeners must be added on the UI thread");
        }

        mDbListeners.add(dbListener);
    }

    /**
     * Remove a CursorListener, this call must be done on the main thread.
     *
     * @param cursorListener
     */
    public void removeListener(final DbListener cursorListener) {
        if (!isOnMainThread()) {
            throw new RuntimeException("listeners must be removed on the UI thread");
        }

        mDbListeners.remove(cursorListener);
    }

    /**
     * Helper method, notifies all listeners on the Android main thread when a change occurs.
     * This method can be called from any thread.
     */
    public void notifyAllListeners() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mDbListeners.isEmpty()) {
                    for (DbListener cursorListener : mDbListeners) {
                        cursorListener.dbUpdated();
                    }
                }
            }
        });
    }

    /**
     * A DB listener
     */
    public interface DbListener {

        /**
         * The Db this listener is listening to has been updated.
         * This call comes on the main thread, no long running operations should be performed on it.
         */
        void dbUpdated();
    }

    /**
     * Helper method to determine if we are on the Android main thread.
     *
     * @return true if we are on the Android main thread.
     */
    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

}
