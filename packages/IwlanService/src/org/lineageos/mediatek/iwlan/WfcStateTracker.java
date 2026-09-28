/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.mediatek.iwlan;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.wfo.IMwiService;
import com.mediatek.wfo.IWifiOffloadListener;
import com.mediatek.wfo.IWifiOffloadService;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wi-Fi calling (WFC) state per SIM slot, as reported by MediaTek's "mwis" service.
 *
 * On MediaTek the modem sets up ePDG and IMS over Wi-Fi by itself; "mwis" (hosted by
 * com.mediatek.ims) exposes the result. The IWLAN network and qualified networks services
 * turn it into the transport information the telephony framework needs.
 */
final class WfcStateTracker {
    private static final String TAG = "IwlanWfcState";
    private static final String MWI_SERVICE = "mwis";
    private static final long RETRY_DELAY_MS = 3000;

    /** Registered for Wi-Fi calling (IMS over IWLAN). */
    static final int WFC_STATE_ON = 1;
    /** Wi-Fi calling registration in progress. */
    static final int WFC_STATE_SEARCHING = 2;
    static final int WFC_STATE_UNKNOWN = 0xff;

    interface Listener {
        void onWfcStateChanged(int slotIndex, int state);
    }

    private static WfcStateTracker sInstance;

    private final Handler mHandler;
    private final int[] mStates;
    private final CopyOnWriteArrayList<Listener> mListeners = new CopyOnWriteArrayList<>();

    private IMwiService mMwiService;
    private IWifiOffloadService mOffloadService;

    private final IBinder.DeathRecipient mDeathRecipient = this::onMwisDied;

    private final IWifiOffloadListener.Stub mOffloadListener = new IWifiOffloadListener.Stub() {
        @Override
        public void onWfcStateChanged(int simIdx, int state) {
            mHandler.post(() -> update(simIdx, state));
        }

        @Override
        public void onHandover(int simIdx, int stage, int ratType) {}

        @Override
        public void onRoveOut(int simIdx, boolean roveOut, int rssi) {}

        @Override
        public void onRequestImsSwitch(int simIdx, boolean isImsOn) {}

        @Override
        public void onWifiPdnOOSStateChanged(int simIdx, int oosState) {}

        @Override
        public void onAllowWifiOff() {}
    };

    static synchronized WfcStateTracker get(Context context) {
        if (sInstance == null) {
            sInstance = new WfcStateTracker(context.getApplicationContext());
        }
        return sInstance;
    }

    private WfcStateTracker(Context context) {
        int slots = context.getSystemService(TelephonyManager.class).getSupportedModemCount();
        mStates = new int[Math.max(slots, 1)];
        Arrays.fill(mStates, WFC_STATE_UNKNOWN);

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
        mHandler.post(this::connect);
    }

    int getState(int slotIndex) {
        return slotIndex >= 0 && slotIndex < mStates.length ? mStates[slotIndex] : WFC_STATE_UNKNOWN;
    }

    void addListener(Listener listener) {
        mListeners.add(listener);
    }

    void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void connect() {
        IBinder binder = ServiceManager.getService(MWI_SERVICE);
        if (binder == null) {
            mHandler.postDelayed(this::connect, RETRY_DELAY_MS);
            return;
        }
        try {
            binder.linkToDeath(mDeathRecipient, 0);
            mMwiService = IMwiService.Stub.asInterface(binder);
            mOffloadService = mMwiService.getWfcHandlerInterface();
            if (mOffloadService != null) {
                mOffloadService.registerForHandoverEvent(mOffloadListener);
            }
            for (int slot = 0; slot < mStates.length; slot++) {
                update(slot, mMwiService.getWfcState(slot));
            }
            Log.i(TAG, "Connected to mwis");
        } catch (RemoteException e) {
            Log.e(TAG, "mwis call failed, retrying", e);
            mMwiService = null;
            mOffloadService = null;
            mHandler.postDelayed(this::connect, RETRY_DELAY_MS);
        }
    }

    private void onMwisDied() {
        mHandler.post(() -> {
            Log.w(TAG, "mwis died, reconnecting");
            mMwiService = null;
            mOffloadService = null;
            for (int slot = 0; slot < mStates.length; slot++) {
                update(slot, WFC_STATE_UNKNOWN);
            }
            connect();
        });
    }

    private void update(int slotIndex, int state) {
        if (slotIndex < 0 || slotIndex >= mStates.length || mStates[slotIndex] == state) {
            return;
        }
        Log.i(TAG, "slot " + slotIndex + " WFC state " + mStates[slotIndex] + " -> " + state);
        mStates[slotIndex] = state;
        for (Listener listener : mListeners) {
            listener.onWfcStateChanged(slotIndex, state);
        }
    }
}
