/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.mediatek.iwlan;

import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.data.ApnSetting;
import android.telephony.data.QualifiedNetworksService;

import java.util.List;

/**
 * Tells the framework which access network IMS should use. The MediaTek modem decides
 * (stock MtkQualifiedNetworksService relays a modem indication); its decision is visible
 * as the WFC state, so IMS is qualified on IWLAN while Wi-Fi calling is registered and on
 * cellular otherwise.
 */
public class IwlanQualifiedNetworksService extends QualifiedNetworksService {
    private static final List<Integer> CELLULAR = List.of(AccessNetworkType.EUTRAN,
            AccessNetworkType.NGRAN, AccessNetworkType.UTRAN, AccessNetworkType.GERAN);
    private static final List<Integer> WIFI = List.of(AccessNetworkType.IWLAN);

    @Override
    public NetworkAvailabilityProvider onCreateNetworkAvailabilityProvider(int slotIndex) {
        return new Provider(slotIndex);
    }

    private class Provider extends NetworkAvailabilityProvider implements WfcStateTracker.Listener {
        private final WfcStateTracker mTracker;

        Provider(int slotIndex) {
            super(slotIndex);
            mTracker = WfcStateTracker.get(IwlanQualifiedNetworksService.this);
            mTracker.addListener(this);
            update(mTracker.getState(slotIndex));
        }

        @Override
        public void onWfcStateChanged(int slotIndex, int state) {
            if (slotIndex == getSlotIndex()) {
                update(state);
            }
        }

        private void update(int state) {
            updateQualifiedNetworkTypes(ApnSetting.TYPE_IMS,
                    state == WfcStateTracker.WFC_STATE_ON ? WIFI : CELLULAR);
        }

        @Override
        public void close() {
            mTracker.removeListener(this);
        }
    }
}
