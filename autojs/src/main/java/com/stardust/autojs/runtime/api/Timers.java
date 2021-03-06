package com.stardust.autojs.runtime.api;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.stardust.autojs.runtime.ScriptBridges;

/**
 * Created by Stardust on 2017/7/21.
 */

public class Timers {

    private SparseArray<Runnable> mHandlerCallbacks = new SparseArray<>();
    private int mCallbackMaxId = 0;
    private ScriptBridges mBridges;
    private ThreadLocal<Handler> mHandler = new ThreadLocal<>();
    private long mFutureCallbackUptimeMillis = 0;

    public Timers(ScriptBridges bridges) {
        mBridges = bridges;
    }

    private void ensureHandler() {
        if (mHandler.get() == null) {
            mHandler.set(new Handler());
        }
    }

    public int setTimeout(final Object callback, final long delay, final Object... args) {
        ensureHandler();
        mCallbackMaxId++;
        final int id = mCallbackMaxId;
        Runnable r = () -> {
            mBridges.callFunction(callback, null, args);
            mHandlerCallbacks.remove(id);
        };
        mHandlerCallbacks.put(id, r);
        postDelayed(r, delay);
        return id;
    }

    public boolean clearTimeout(int id) {
        return clearCallback(id);
    }

    public int setInterval(final Object listener, final long interval, final Object... args) {
        ensureHandler();
        mCallbackMaxId++;
        final int id = mCallbackMaxId;
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mHandlerCallbacks.get(id) == null)
                    return;
                mBridges.callFunction(listener, null, args);
                postDelayed(this, interval);
            }
        };
        mHandlerCallbacks.put(id, r);
        postDelayed(r, interval);
        return id;
    }

    private void postDelayed(Runnable r, long interval) {
        long uptime = SystemClock.uptimeMillis() + interval;
        mHandler.get().postAtTime(r, uptime);
        mFutureCallbackUptimeMillis = Math.max(mFutureCallbackUptimeMillis, uptime);
    }

    public boolean clearInterval(int id) {
        return clearCallback(id);
    }

    public int setImmediate(final Object listener, final Object... args) {
        ensureHandler();
        mCallbackMaxId++;
        final int id = mCallbackMaxId;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mBridges.callFunction(listener, null, args);
                mHandlerCallbacks.remove(id);
            }
        };
        mHandlerCallbacks.put(id, r);
        postDelayed(r, 0);
        return id;
    }

    public boolean clearImmediate(int id) {
        return clearCallback(id);
    }

    private boolean clearCallback(int id) {
        Runnable callback = mHandlerCallbacks.get(id);
        if (callback != null) {
            mHandler.get().removeCallbacks(callback);
            mHandlerCallbacks.remove(id);
            return true;
        }
        return false;
    }


    public boolean hasPendingCallback() {
        return mFutureCallbackUptimeMillis > SystemClock.uptimeMillis();
    }
}
