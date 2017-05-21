//=============================================================================
// Copyright  2016 YI Technologies, Inc. All Rights Reserved.
//
// This software is the confidential and proprietary information of YI
// Technologies, Inc. ("Confidential Information").  You shall not
// disclose such Confidential Information and shall use it only in
// accordance with the terms of the license agreement you entered into
// with YI.
//
// YI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
// SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// PURPOSE, OR NON-INFRINGEMENT. YI SHALL NOT BE LIABLE FOR ANY DAMAGES
// SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
// THIS SOFTWARE OR ITS DERIVATIVES.
//=============================================================================

package com.xiaoyi.yi360demo;

import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.*;
import android.support.v7.app.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.Switch;

import java.net.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import com.xiaoyi.action.*;

enum ControlView {
    cameraSelection,
    cameraGrid,
    settings
};

enum CameraMode {
    video,
    photo
}

public class GridActivity extends AppCompatActivity {
    private boolean mStartScanTimer;
    private GridView gridView;
    private AlertDialog mExitDialog;
    private Menu mMenu;
    public ArrayList<Camera> pendingCameras;
    public ArrayList<Camera> cameras;
    public GridAdapter gridAdapter;
    private CameraMode cameraMode = CameraMode.video;
    private boolean isRecording = false;

    // Photo settings
    private ColorMode photoColorMode = ColorMode.YIColor;
    private ExposureValue photoExposureValue = ExposureValue.ev_0;
    private ISO photoIso = ISO.iso_Auto;
    private MeteringMode photoMeteringMode = MeteringMode.Center;
    private PhotoResolution photoResolution = PhotoResolution.p_12MP_4000x3000_4x3_w;
    private Sharpness photoSharpness = Sharpness.Medium;
    private ShutterTime photoShutterTime = ShutterTime.st_Auto;
    private WhiteBalance photoWhiteBalance = WhiteBalance.Auto;

    // Video settings
    private ColorMode videoColorMode = ColorMode.YIColor;
    private ExposureValue videoExposureValue = ExposureValue.ev_0;
    private ISO videoIso = ISO.iso_Auto;
    private FieldOfView videoFieldOfView = FieldOfView.Medium;
    private MeteringMode videoMeteringMode = MeteringMode.Center;
    private VideoResolution videoResolution = VideoResolution.v_3840x2160_30p_16x9_super;
    private Sharpness videoSharpness = Sharpness.Medium;
    private Quality videoQuality = Quality.High;
    private VideoStandard videoStandard = VideoStandard.PAL;
    private WhiteBalance videoWhiteBalance = WhiteBalance.Auto;
    private VideoRotateMode videoRotate = VideoRotateMode.Auto;
    private ToggleState videoMuteState = ToggleState.Off;
    private int videoDelayStart = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize YiCameraPlatform
        try {
            Platform.initialize(new Logger() {
                @Override
                public void verbose(String message) {
                    Log.v("YiCameraPlatform", message);
                }

                @Override
                public void info(String message) {
                    Log.i("YiCameraPlatform", message);
                }

                @Override
                public void warning(String message) {
                    Log.w("YiCameraPlatform", message);
                }

                @Override
                public void error(String message) {
                    Log.e("YiCameraPlatform", message);
                }
            });
        } catch (Exception ex) {
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        pendingCameras = new ArrayList<>();
        cameras = new ArrayList<>();
        gridAdapter = new GridAdapter(this, cameras);

        setContentView(R.layout.grid_activity);
        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(gridAdapter);

        updateActionButtonText();
        initSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.grid_activity_actionbar, menu);

        mMenu = menu;
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.photoSettings).getVisibility() == View.VISIBLE ||
            findViewById(R.id.videoSettings).getVisibility() == View.VISIBLE) {
            updateSettings();
            showControlView(ControlView.cameraGrid);
        } else if (mExitDialog != null && mExitDialog.isShowing()) {
            mExitDialog.dismiss();
        } else {
            exit(null);
        }
    }

    public void select6Cameras(View view) {
        selectCameras(6);
    }

    public void select7Cameras(View view) {
        selectCameras(7);
    }

    public void select10Cameras(View view) {
        selectCameras(10);
    }

    public void select24Cameras(View view) {
        selectCameras(24);
    }

    private Camera getCameraByIp(String ip, ArrayList<Camera> cameras) {
        for (Camera camera: cameras) {
            if (camera.getIp().equals(ip)) {
                return camera;
            }
        }
        return null;
    }

    private void startRecording() {
        setRecording(true);

        // Start recording after 10 seconds
        Date date = new Date();
        date.setTime(date.getTime() + videoDelayStart * 1000);
        for (Camera camera: cameras) {
            if (!camera.getIp().isEmpty()) {
                camera.startRecording(
                        date,
                        videoColorMode,
                        videoExposureValue,
                        videoIso,
                        videoFieldOfView,
                        videoMeteringMode,
                        videoResolution,
                        videoQuality,
                        videoSharpness,
                        videoWhiteBalance,
                        videoStandard,
                        videoMuteState);
            }
        }
    }

    private void stopRecording() {
        setRecording(false);

        for (Camera camera: cameras) {
            if (!camera.getIp().isEmpty()) {
                camera.stopRecording();
            }
        }
    }

    private void capturePhoto() {
        for (Camera camera: cameras) {
            if (!camera.getIp().isEmpty()) {
                camera.capturePhoto(
                        photoColorMode,
                        photoExposureValue,
                        photoIso,
                        photoMeteringMode,
                        photoResolution,
                        photoSharpness,
                        photoShutterTime,
                        photoWhiteBalance);
            }
        }
    }

    public void showSettings(MenuItem item) {
        showControlView(ControlView.settings);
    }

    public void switchMode(MenuItem item) {
        stopRecording();
        cameraMode = cameraMode == CameraMode.video ? CameraMode.photo : CameraMode.video;
        updateActionButtonText();
    }

    public void exit(MenuItem item) {
        if (mExitDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setMessage("Do you want to Exit?");
            builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mStartScanTimer = false;
                    stopRecording();
                    for (Camera camera : cameras) {
                        camera.disconnect();
                    }
                    finish();
                }
            });
            builder.setNegativeButton("Run at background", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    moveTaskToBack(true);
                }
            });
            mExitDialog = builder.create();
        }
        mExitDialog.show();
    }

    private boolean isWifiHotSpotEnabled() {
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            Method isWifiApEnabledMethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean)isWifiApEnabledMethod.invoke(wifiManager);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void selectCameras(int cameraCount) {
        // check WifiHotSpot is enabled or not, if wifi hotspot is not enabled, show error
        if (!isWifiHotSpotEnabled()) {
            showMessage("WifiHotspot is not enabled. Please enable it.");
            return;
        }

        for (int i = 0; i < mMenu.size(); i++) {
            mMenu.getItem(i).setVisible(true);
        }

        showControlView(ControlView.cameraGrid);

        cameras.clear();
        for (int i = 0; i < cameraCount; ++i) {
            cameras.add(new Camera(this, null, null));
        }
        gridAdapter.notifyDataSetChanged();

        // start to scan camera
        mStartScanTimer = true;
        doScanCamera();
    }

    // Scan cameras and maintain the camera lists every 10 seconds.
    // This function will do following things:
    //      1. Get the list who is connected to the hotspot.
    //      2. Close the camera connection whose ip is not in the list.
    //      3. For new ip in the the list, find a free camera connection to connect.
    //      4. Send heartbeat for all camera connections.
    private void doScanCamera() {
        if (!mStartScanTimer) {
            return;
        }

        Log.i("YiCamera", "scan camera");

        // check whether the cameras list is full
        boolean hasFreeSlot = false;
        for (int i = 0; i < cameras.size(); ++i) {
            if (cameras.get(i).getIp().isEmpty()) {
                hasFreeSlot = true;
                break;
            }
        }

        if (hasFreeSlot) {
            final GridActivity obj = this;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<String> clientIPs = new ArrayList<>();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] splitted = line.split(" +");
                            if (splitted != null && splitted.length >= 4) {
                                String ip = splitted[0];
                                String mac = splitted[3];
                                if (mac.matches("..:..:..:..:..:..")) {
                                    if (InetAddress.getByName(ip).isReachable(1000)) {
                                        clientIPs.add(ip);
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!mStartScanTimer) {
                                return;
                            }

                            for (String ip : clientIPs) {
                                if (getCameraByIp(ip, pendingCameras) == null && getCameraByIp(ip, cameras) == null) {
                                    Log.i("YiCamera", "Find new ip: " + ip);
                                    String hostname = "";
                                    try {
                                        hostname = InetAddress.getByName(ip).getHostName();
                                    } catch (Exception ex) {
                                    }
                                    Camera camera = new Camera(obj, ip, hostname);
                                    pendingCameras.add(camera);
                                    camera.connect();
                                }
                            }
                        }
                    });
                }
            });
        } else {
            Log.i("YI360", "No free slot");
        }

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        doScanCamera();
                    }
                });
            }
        }, 10 * 1000);
    }

    private void showMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Message");
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();
    }

    private void showControlView(ControlView view) {
        findViewById(R.id.cameraSelectionView).setVisibility(view == ControlView.cameraSelection ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.cameraGridView).setVisibility(view == ControlView.cameraGrid ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.photoSettings).setVisibility(view == ControlView.settings && cameraMode == CameraMode.photo ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.videoSettings).setVisibility(view == ControlView.settings && cameraMode == CameraMode.video ? View.VISIBLE : View.INVISIBLE);
    }

    public void actionButtonPressed(View view) {
        if (cameraMode == CameraMode.video) {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        } else {
            capturePhoto();
        }
    }

    private void setRecording(boolean recording) {
        isRecording = recording;
        updateActionButtonText();
    }

    private void updateActionButtonText() {
        Button actionButton = (Button) findViewById(R.id.actionButton);
        actionButton.setText(cameraMode == CameraMode.video ? (isRecording ? "Stop Recording" : "Start Recording") : "Capture Photograph");
    }

    private void updateSettings() {
        photoColorMode = ColorMode.values()[((Spinner) findViewById(R.id.photoColorMode)).getSelectedItemPosition() + 1];
        photoExposureValue = ExposureValue.values()[((Spinner) findViewById(R.id.photoExposureValue)).getSelectedItemPosition() + 1];
        photoIso = ISO.values()[((Spinner) findViewById(R.id.photoIso)).getSelectedItemPosition() + 1];
        photoMeteringMode = MeteringMode.values()[((Spinner) findViewById(R.id.photoMeteringMode)).getSelectedItemPosition() + 1];
        photoResolution = PhotoResolution.values()[((Spinner) findViewById(R.id.photoResolution)).getSelectedItemPosition() + 1];
        photoSharpness = Sharpness.values()[((Spinner) findViewById(R.id.photoSharpness)).getSelectedItemPosition() + 1];
        photoShutterTime = ShutterTime.values()[((Spinner) findViewById(R.id.photoShutterTime)).getSelectedItemPosition() + 1];
        photoWhiteBalance = WhiteBalance.values()[((Spinner) findViewById(R.id.photoWhiteBalance)).getSelectedItemPosition() + 1];

        videoColorMode = ColorMode.values()[((Spinner) findViewById(R.id.videoColorMode)).getSelectedItemPosition() + 1];
        videoExposureValue = ExposureValue.values()[((Spinner) findViewById(R.id.videoExposureValue)).getSelectedItemPosition() + 1];
        videoIso = ISO.values()[((Spinner) findViewById(R.id.videoIso)).getSelectedItemPosition() + 1];
        videoFieldOfView = FieldOfView.values()[((Spinner) findViewById(R.id.videoFieldOfView)).getSelectedItemPosition() + 1];
        videoMeteringMode = MeteringMode.values()[((Spinner) findViewById(R.id.videoMeteringMode)).getSelectedItemPosition() + 1];
        videoResolution = VideoResolution.values()[((Spinner) findViewById(R.id.videoResolution)).getSelectedItemPosition() + 1];
        videoSharpness = Sharpness.values()[((Spinner) findViewById(R.id.videoSharpness)).getSelectedItemPosition() + 1];
        videoQuality = Quality.values()[((Spinner) findViewById(R.id.videoQuality)).getSelectedItemPosition() + 1];
        videoStandard = VideoStandard.values()[((Spinner) findViewById(R.id.videoStandard)).getSelectedItemPosition() + 1];
        videoWhiteBalance = WhiteBalance.values()[((Spinner) findViewById(R.id.videoWhiteBalance)).getSelectedItemPosition() + 1];
        videoRotate = VideoRotateMode.values()[((Spinner) findViewById(R.id.videoRotate)).getSelectedItemPosition() + 1];
        videoMuteState = ((Switch) findViewById(R.id.videoMuteState)).isChecked() ? ToggleState.On : ToggleState.Off;

        int position = ((Spinner) findViewById(R.id.videoStartDelay)).getSelectedItemPosition();
        switch(position) {
            case 0: videoDelayStart = 0; break;
            case 1: videoDelayStart = 1; break;
            case 2: videoDelayStart = 5; break;
            case 3: videoDelayStart = 10; break;
            case 4: videoDelayStart = 15; break;
            case 5: videoDelayStart = 30; break;
        }
    }

    private void initSettings() {

        ((Spinner) findViewById(R.id.photoColorMode)).setSelection(photoColorMode.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoExposureValue)).setSelection(photoExposureValue.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoIso)).setSelection(photoIso.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoMeteringMode)).setSelection(photoMeteringMode.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoResolution)).setSelection(photoResolution.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoSharpness)).setSelection(photoSharpness.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoShutterTime)).setSelection(photoShutterTime.ordinal() - 1);
        ((Spinner) findViewById(R.id.photoWhiteBalance)).setSelection(photoWhiteBalance.ordinal() - 1);

        ((Spinner) findViewById(R.id.videoColorMode)).setSelection(videoColorMode.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoExposureValue)).setSelection(videoExposureValue.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoIso)).setSelection(videoIso.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoFieldOfView)).setSelection(videoFieldOfView.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoMeteringMode)).setSelection(videoMeteringMode.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoResolution)).setSelection(videoResolution.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoSharpness)).setSelection(videoSharpness.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoQuality)).setSelection(videoQuality.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoStandard)).setSelection(videoStandard.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoWhiteBalance)).setSelection(videoWhiteBalance.ordinal() - 1);
        ((Spinner) findViewById(R.id.videoRotate)).setSelection(videoRotate.ordinal() - 1);
        ((Switch) findViewById(R.id.videoMuteState)).setChecked(videoMuteState == ToggleState.On);

        int position = 10;
        switch(videoDelayStart) {
            case 0: position = 0; break;
            case 1: position = 1; break;
            case 5: position = 2; break;
            case 10: position = 3; break;
            case 15: position = 4; break;
            case 30: position = 5; break;
        }
        ((Spinner) findViewById(R.id.videoStartDelay)).setSelection(position);

    }
}