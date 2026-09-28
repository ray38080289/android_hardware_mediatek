/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.mediatek.wfo;

import com.mediatek.wfo.IWifiOffloadListener;

/**
 * Leading part of MediaTek's IWifiOffloadService. Only the first methods are declared;
 * their order defines transaction codes 1-3 and must match the MediaTek interface.
 */
interface IWifiOffloadService {
    void registerForHandoverEvent(IWifiOffloadListener listener);
    void unregisterForHandoverEvent(IWifiOffloadListener listener);
    int getRatType(int simIdx);
}
