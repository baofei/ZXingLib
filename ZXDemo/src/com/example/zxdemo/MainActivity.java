
package com.example.zxdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.bf.zx.qrcodelib.AmbientLightManager;
import com.bf.zx.qrcodelib.BeepManager;
import com.zxing.qrcodelib.camera.CameraManager;
import com.zxing.qrcodelib.decode.Decode;
import com.zxing.qrcodelib.decode.OnDecodeCallBack;
import com.zxing.qrcodelib.result.QrcodeResult;
import com.zxing.qrcodelib.view.LinefinderView;
import com.zxing.qrcodelib.view.ViewfinderResultPointCallback;

import java.io.IOException;

public class MainActivity extends ActionBarActivity implements SurfaceHolder.Callback,
        OnDecodeCallBack {
    private BeepManager beepManager;

    private AmbientLightManager ambientLightManager;

    private CameraManager cameraManager;

    private LinefinderView viewfinderView;

    private SurfaceView surfaceView;

    private Decode decode;

    private boolean hasSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beepManager = new BeepManager(this, R.raw.beep, false, true);
        ambientLightManager = new AmbientLightManager(this);
        viewfinderView = (LinefinderView) findViewById(R.id.viewfinder_view);
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {

        cameraManager = new CameraManager(this.getApplication());
        viewfinderView.setCameraManager(cameraManager);
        decode = new Decode(cameraManager, new ViewfinderResultPointCallback(viewfinderView));
        decode.setDecodeCallBack(this);

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
        ambientLightManager.start(cameraManager);
        super.onResume();
    }

    @Override
    protected void onPause() {
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        decode.stop();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        hasSurface = false;
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        decode.stop();
        super.onDestroy();
    }

    @Override
    public boolean onDecodeCallBack(boolean isSuc, QrcodeResult result) {
        if (isSuc) {
            beepManager.playBeepSoundAndVibrate();
            Toast.makeText(this, result.result.getText(), Toast.LENGTH_SHORT).show();
            surfaceView.postDelayed(new Runnable() {

                @Override
                public void run() {
                    cameraManager.requestPreviewFrame();

                }
            }, 5000);
            return false;//return true结束扫描
        } else {
            cameraManager.requestPreviewFrame();
            return false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e("surfaceCreated", "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        decode.stop();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        viewfinderView.setText("将取景框对准二维码，即可自动扫描!", -1,
                24 * surfaceHolder.getSurfaceFrame().right / 720);
        if (cameraManager.isOpen()) {
            Log.w("initcamera", "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder, 0.5f);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            cameraManager.startPreview(decode);
            cameraManager.requestPreviewFrame();
        } catch (IOException ioe) {
            Log.w("initcamera", ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w("initcamera", "Unexpected error initializing camera", e);
        }

    }
}
