/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.mediatek.iwlan;

import com.android.internal.telephony.data.CellularDataService;

/**
 * WLAN data service. The modem runs ePDG itself, so IWLAN data calls go to the same RIL
 * requests as cellular ones with the access network set to IWLAN by the framework; this is
 * what MediaTek's IwlanDataService does too. A separate component gives the WLAN transport
 * its own service instance and providers.
 */
public class IwlanDataService extends CellularDataService {}
