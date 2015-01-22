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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.TextParsedResult;
import com.google.zxing.client.result.URIParsedResult;

import java.io.IOException;

/**
 * @author Sean Owen
 * @author jesus Have modified this class so that it is no longer an activity,
 *         it is now a fragment. This class is called to activate the camera and
 *         to begin decoding the input from the camera. This class also
 *         populates a surfaceView with the camera input so that it is shown on
 *         screen.
 */
public final class CameraFragment extends Fragment implements
        SurfaceHolder.Callback {

    private static final String TAG = CameraFragment.class.getSimpleName();
    // private static final String SCAN_ACTION =
    // "com.google.zxing.client.android.SCAN";

    private boolean hasSurface;
    private boolean returnResult;
    private SurfaceHolder holderWithCallback;
    private Camera camera;
    private DecodeRunnable decodeRunnable;
    private Result result;

    /**
     * Used to use the intent action in order to return the correct result.
     * Currently set so that only one intent option is avaiable and it was saved
     * in the QRCODEVIEWPAGER actvitiy.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // SharedPreferences settings = getActivity().getSharedPreferences(
        // QRCodeViewPager.INTENT, 0);
        // settings.getBoolean("returnResult", true);
        StorageBase mStore = new StorageBase(getActivity(),
                StorageBase.ENCRYPTION_DEFAULT);
        if (mStore.getInt("CameraIntent", 0) == 1) {
            returnResult = true;
        }

        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // setContentView(R.layout.qr_read);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qr_read, container, false);
    }

    /**
     * SurfaceHolder is resumed. This is the camera view that is shown on
     * screen.
     */
    @Override
    public synchronized void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) getActivity().findViewById(
                R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder?");
        }
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            holderWithCallback = surfaceHolder;
        }
    }

    /**
     * While this fragment is no longer visible it should not be decoding or
     * using the camera object at all.
     */
    @Override
    public synchronized void onPause() {
        result = null;
        if (decodeRunnable != null) {
            decodeRunnable.stop();
            decodeRunnable = null;
        }
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (holderWithCallback != null) {
            holderWithCallback.removeCallback(this);
            holderWithCallback = null;
        }
        super.onPause();
    }

    @Override
    public synchronized void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface created");
        holderWithCallback = null;
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public synchronized void surfaceChanged(SurfaceHolder holder, int format,
            int width, int height) {
        // do nothing
    }

    @Override
    public synchronized void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Surface destroyed");
        holderWithCallback = null;
        hasSurface = false;
    }

    /**
     * Check each cameraid between 0 and cameraCount-1, looking for rear-facing
     * cameras. As soon as one is found, return it. If no rear-facing cameras are
     * found, returns null.
     *
     * @param cameraCount The number of cameras
     */
    private Camera getFirstRearFacingCamera() {
      int cameraCount = Camera.getNumberOfCameras();
      CameraInfo info = new CameraInfo();
      for (int i=0; i < cameraCount; i++) {
        Camera.getCameraInfo(i, info);
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
          return Camera.open(i);
        }
      }
      return null;
    }

    /**
     * Check each cameraid between 0 and cameraCount-1, looking for front-facing
     * cameras. As soon as one is found, return it. If no front-facing cameras are
     * found, returns null.
     *
     * @param cameraCount The number of cameras
     */
    private Camera getFirstFrontFacingCamera() {
      int cameraCount = Camera.getNumberOfCameras();
      CameraInfo info = new CameraInfo();
      for (int i=0; i < cameraCount; i++) {
        Camera.getCameraInfo(i, info);
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
          return Camera.open(i);
        }
      }
      return null;
    }

    /**
     * For some reason the camera begins tilted 90 degrees so I have it set to
     * counteract that. This also begins running a decoder on the camera stream.
     * 
     * @param holder
     *            Surface to be used for preview of the camera.
     */
    private void initCamera(SurfaceHolder holder) {
        if (camera != null) {
          throw new IllegalStateException("Camera not null on initialization");
        }

        Camera camera = getFirstRearFacingCamera();
        if (camera == null) {
          camera = getFirstFrontFacingCamera();
        }
        // If camera is still null, there aren't any cameras at all. 
        if (camera == null) {
          throw new IllegalStateException("Camera is null");
        }

        CameraConfigurationManager.configure(camera);

        try {
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Cannot start preview", e);
        }
        
        decodeRunnable = new DecodeRunnable(this, camera);
        new Thread(decodeRunnable).start();
        reset();
    }

    /**
     * Sets the result as the result of the activity. So that when the parent
     * activity, QRVIEWPAGER, closes, it will have returned the value of the
     * read qr code.
     * 
     * @param result
     *            The result of reading a QR code.
     */
    void setResult(Result result) {
        if (result.getText().substring(0, 10).equals("Rangzen://")) {
            FriendStore friendStore = new FriendStore(getActivity(), StorageBase.ENCRYPTION_DEFAULT);
            
            // TODO(lerner): Actually convert from the friend code string to 
            // the friend id properly. Simply grabbing the bytes of the string
            // isn't the right thing.
            String friendCode = result.getText().substring(10);
            friendStore.addFriendBytes(friendCode.getBytes()); // WRONG.

            TextView statusView = (TextView) getActivity().findViewById(
                    R.id.status_view);
            String text = "Friend Added Successfully";
            statusView.setText(text);
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    Math.max(14, 56 - text.length() / 4));
            statusView.setVisibility(View.VISIBLE);
            this.result = result;
        }
    }

    /**
     * Not used as of right now. This would go to the website that the QR code
     * encoded or do an action that the qr code suggested.
     */
    public void handleResult(Result result) {
        ParsedResult parsed = ResultParser.parseResult(result);
        Intent intent;
        if (parsed.getType() == ParsedResultType.URI) {
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(((URIParsedResult) parsed).getURI()));
        } else {
            intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra("query", ((TextParsedResult) parsed).getText());
        }
        startActivity(intent);
    }

    /**
     * On back button if there is a currently set result then it will clear it
     * and begin scanning again.
     */
    public synchronized void reset() {
        TextView statusView = (TextView) getActivity().findViewById(
                R.id.status_view);
        statusView.setVisibility(View.GONE);
        result = null;
        decodeRunnable.startScanning();
    }

    public Result getResult() {
        return result;
    }

}
