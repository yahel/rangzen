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
 */
public final class CameraFragment extends Fragment implements
		SurfaceHolder.Callback {

	private static final String TAG = CameraFragment.class.getSimpleName();
	//private static final String SCAN_ACTION = "com.google.zxing.client.android.SCAN";

	private boolean hasSurface;
	private boolean returnResult;
	private SurfaceHolder holderWithCallback;
	private Camera camera;
	private DecodeRunnable decodeRunnable;
	private Result result;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// returnResult should be true if activity was started using
		// startActivityForResult() with SCAN_ACTION intent
//		Intent intent = getIntent();
//		returnResult = intent != null && SCAN_ACTION.equals(intent.getAction());
		//returnResult = true;
		
      SharedPreferences settings = getActivity().getSharedPreferences(
      QRCodeViewPager.INTENT, 0);
      settings.getBoolean("returnResult", true);

		Window window = getActivity().getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//setContentView(R.layout.qr_read);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.qr_read, container, false);
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) getActivity().findViewById(R.id.preview_view);
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

//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (result != null) {
//			switch (keyCode) {
//			case KeyEvent.KEYCODE_DPAD_CENTER:
//				handleResult(result);
//				return true;
//			case KeyEvent.KEYCODE_BACK:
//				reset();
//				return true;
//			}
//		}
//		return super.onKeyDown(keyCode, event);
//	}

	private void initCamera(SurfaceHolder holder) {
		if (camera != null) {
			throw new IllegalStateException("Camera not null on initialization");
		}
		camera = Camera.open();
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

	void setResult(Result result) {
		if (returnResult) {
			Intent scanResult = new Intent(
					"com.google.zxing.client.android.SCAN");
			scanResult.putExtra("SCAN_RESULT", result.getText());
			getActivity().setResult(Activity.RESULT_OK, scanResult);
			getActivity().finish();
		} else {
			TextView statusView = (TextView) getActivity().findViewById(R.id.status_view);
			String text = result.getText();
			statusView.setText(text);
			statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
					Math.max(14, 56 - text.length() / 4));
			statusView.setVisibility(View.VISIBLE);
			this.result = result;
		}
	}

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

	public synchronized void reset() {
		TextView statusView = (TextView) getActivity().findViewById(R.id.status_view);
		statusView.setVisibility(View.GONE);
		result = null;
		decodeRunnable.startScanning();
	}
	
	public Result getResult() {
		return result;
	}

}