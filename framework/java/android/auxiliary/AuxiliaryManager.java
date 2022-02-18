/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.auxiliary;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.auxiliary.aidl.IAuxiliaryManagerService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import java.util.List;

/**
 * Auxiliary Manager.
 *
 * @hide
 */
public class AuxiliaryManager {
    private boolean mBound = false;
    private static final String TAG = "AuxiliaryManager";

    private IAuxiliaryManagerService mService;
    private final Context mContext;

    public AuxiliaryManager(Context context) {
        mContext = context;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IAuxiliaryManagerService.Stub.asInterface(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    /**
     * Gets auxiliary version.
     *
     * @hide
     */
    public String getVersion() {
        try {
            if (!mBound) {
                Intent intent = new Intent("com.android.auxiliary.service.AuxiliaryService");
                ComponentName serviceComponent = resolveAuxiliaryService(
                        intent, mContext.getPackageManager());
                intent.setComponent(serviceComponent);
                boolean r = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                if (r) {
                    return "bindService is successful";
                }
                return "bindService is failed";
            } else {
                return mService.getVersion();
            }
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Find the ComponentName of Auxiliary service, given its intent and package manager.
     *
     * @return ComponentName of Auxiliary service. Null if the service is not found.
     */
    private @Nullable ComponentName resolveAuxiliaryService(@NonNull Intent intent,
            @NonNull PackageManager pm) {
        List<ResolveInfo> services = pm.queryIntentServices(intent,
                PackageManager.ResolveInfoFlags.of(0));
        if (services == null || services.isEmpty()) {
            Slog.e(TAG, "Failed to find auxiliary service");
            return null;
        }

        for (int i = 0; i < services.size(); i++) {
            ResolveInfo ri = services.get(i);
            ComponentName resolved = new ComponentName(
                    ri.serviceInfo.packageName, ri.serviceInfo.name);
            // There should only be one matching service inside the given package.
            // If there's more than one, return the first one found.
            return resolved;
        }
        Slog.e(TAG, "Didn't find any matching auxiliary service.");
        return null;
    }
}
