/*
 * Copyright (C) 2014 ZXing authors
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

package org.denovogroup.rangzen;

import android.hardware.Camera;
import android.util.Log;

/**
 * @author Sean Owen This class establishes camera behavior for the camera
 *         activity/QR reading. It has a private constructor because it contains
 *         static methods that can be called from any class.
 * @author jesus Have modified this class from Zxing to focus the camera to get
 *         better results for scanning.
 */
final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    static final int ZOOM = 2;

    private CameraConfigurationManager() {
    }

    static void configure(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(1280, 720);
        // parameters.setPreviewSize(1920, 1080);
        configureAdvanced(parameters);
        camera.setParameters(parameters);
        logAllParameters(parameters);
    }

    private static void configureAdvanced(Camera.Parameters parameters) {
        CameraConfigurationUtils.setBestPreviewFPS(parameters);
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
        CameraConfigurationUtils.setVideoStabilization(parameters);
        CameraConfigurationUtils.setMetering(parameters);
        CameraConfigurationUtils.setZoom(parameters, ZOOM);
        CameraConfigurationUtils.setFocus(parameters, true, false, false);
    }

    private static void logAllParameters(Camera.Parameters parameters) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            for (String line : CameraConfigurationUtils
                    .collectStats(parameters).split("\n")) {
                Log.i(TAG, line);
            }
        }
    }

}