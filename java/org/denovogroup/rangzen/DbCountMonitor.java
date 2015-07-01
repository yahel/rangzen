package org.denovogroup.rangzen;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class DbCountMonitor {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private volatile int mListenerCount = 0;

    private final List<CursorListener> mCursorListeners = new ArrayList<>(2);

    public synchronized boolean willNotifyListeners() {
        boolean retVal = mListenerCount == 0;
        mListenerCount++;

        return retVal;
    }

    public synchronized boolean notifyListeners() {
        mListenerCount--;
        if (mListenerCount <= 0) {
            mListenerCount = 0;
            notifyAllListeners();
            return true;
        }

        return false;
    }

    public void addListener(final CursorListener cursorListener) {
        if (!isOnMainThread()) {
            throw new RuntimeException("listeners must be added on the UI thread");
        }

        mCursorListeners.add(cursorListener);
    }

    public void removeListener(final CursorListener cursorListener) {
        if (!isOnMainThread()) {
            throw new RuntimeException("listeners must be removed on the UI thread");
        }

        mCursorListeners.remove(cursorListener);
    }


    private void notifyAllListeners() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mCursorListeners.isEmpty()) {
                    for (CursorListener cursorListener : mCursorListeners) {
                        cursorListener.reloadCursor();
                    }
                }
            }
        });
    }

    public interface CursorListener {
        void reloadCursor();
    }

    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

}
