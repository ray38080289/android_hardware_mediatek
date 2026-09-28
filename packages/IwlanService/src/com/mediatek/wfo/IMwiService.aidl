/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.mediatek.wfo;

import com.mediatek.wfo.IWifiOffloadService;

/**
 * Client side of the "mwis" service hosted by MediaTek's ImsService (com.mediatek.ims).
 * Method order defines the transaction codes and must match the MediaTek interface.
 */
interface IMwiService {
    IWifiOffloadService getWfcHandlerInterface();
    int getWfcState(int phoneId);
}
