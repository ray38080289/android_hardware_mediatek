/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.mediatek.wfo;

/**
 * Callback registered with MediaTek's IWifiOffloadService. Method order defines the
 * transaction codes and must match the MediaTek interface (all calls are one-way).
 */
oneway interface IWifiOffloadListener {
    void onHandover(int simIdx, int stage, int ratType);
    void onRoveOut(int simIdx, boolean roveOut, int rssi);
    void onRequestImsSwitch(int simIdx, boolean isImsOn);
    void onWifiPdnOOSStateChanged(int simIdx, int oosState);
    void onAllowWifiOff();
    void onWfcStateChanged(int simIdx, int state);
}
