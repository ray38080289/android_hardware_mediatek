/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.mediatek.iwlan;

import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.NetworkService;
import android.telephony.NetworkServiceCallback;
import android.telephony.TelephonyManager;

import java.util.List;

/**
 * WLAN network service: reports the IWLAN PS registration the modem already has when
 * Wi-Fi calling is registered. Without it the framework sees no voice/data route while
 * the cellular radio is off and rejects Wi-Fi calls before they reach IMS.
 * Same mapping as MediaTek's IWlanNetworkService.
 */
public class IwlanNetworkService extends NetworkService {

    @Override
    public NetworkServiceProvider onCreateNetworkServiceProvider(int slotIndex) {
        return new Provider(slotIndex);
    }

    private class Provider extends NetworkServiceProvider implements WfcStateTracker.Listener {
        private final WfcStateTracker mTracker;

        Provider(int slotIndex) {
            super(slotIndex);
            mTracker = WfcStateTracker.get(IwlanNetworkService.this);
            mTracker.addListener(this);
        }

        @Override
        public void requestNetworkRegistrationInfo(int domain, NetworkServiceCallback callback) {
            if (domain != NetworkRegistrationInfo.DOMAIN_PS) {
                callback.onRequestNetworkRegistrationInfoComplete(
                        NetworkServiceCallback.RESULT_ERROR_UNSUPPORTED, null);
                return;
            }
            callback.onRequestNetworkRegistrationInfoComplete(NetworkServiceCallback.RESULT_SUCCESS,
                    registrationInfo(mTracker.getState(getSlotIndex())));
        }

        @Override
        public void onWfcStateChanged(int slotIndex, int state) {
            if (slotIndex == getSlotIndex()) {
                notifyNetworkRegistrationInfoChanged();
            }
        }

        @Override
        public void close() {
            mTracker.removeListener(this);
        }
    }

    private static NetworkRegistrationInfo registrationInfo(int wfcState) {
        int regState;
        int technology = TelephonyManager.NETWORK_TYPE_IWLAN;
        switch (wfcState) {
            case WfcStateTracker.WFC_STATE_ON:
                regState = NetworkRegistrationInfo.REGISTRATION_STATE_HOME;
                break;
            case WfcStateTracker.WFC_STATE_SEARCHING:
                regState = NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_SEARCHING;
                break;
            default:
                regState = NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
                technology = TelephonyManager.NETWORK_TYPE_UNKNOWN;
                break;
        }
        return new NetworkRegistrationInfo.Builder()
                .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                .setRegistrationState(regState)
                .setAccessNetworkTechnology(technology)
                .setAvailableServices(regState == NetworkRegistrationInfo.REGISTRATION_STATE_HOME
                        ? List.of(NetworkRegistrationInfo.SERVICE_TYPE_DATA)
                        : List.of())
                .build();
    }
}
