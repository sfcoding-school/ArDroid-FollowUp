package castellini.jacopo.ardroidfollowup;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    private CameraBridgeViewBase mOpenCvCameraView;
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private String currState = "null";
    private Mat mRgba;
    private FeatureScanner featureScanner;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        openAccessory(accessory);
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory))
                    closeAccessory();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        closeAccessory();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);

        if (mInputStream != null && mOutputStream != null) {
            return;
        }
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory))
                openAccessory(accessory);
            else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        featureScanner = new FeatureScanner(this.getApplicationContext());

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraSurface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
        }
    }

    private void closeAccessory() {
        try {
            if (mFileDescriptor != null)
                mFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        String newState = featureScanner.doTheWork(mRgba);

        if (!newState.equals(currState)) {
            currState = newState;
            sendCommand(getCommand(newState));
        }

        return mRgba;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean sendCommand(String command) {
        byte[] buffer = command.getBytes();
        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mInputStream != null) {
            int ret = 0;
            byte[] response = new byte[2];
            while (ret <= 0) {
                try {
                    ret = mInputStream.read(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return response[0] == '4' && response[1] == '2';
        }
        return false;
    }

    public String getCommand(String command) {
        String commandToExecute = "null";
        switch (command) {
            case "stop":
                commandToExecute = "0V;";
                break;
            case "left":
                commandToExecute = "0V;2A3;2B2;";
                break;
            case "right":
                commandToExecute = "0V;2A2;2B3;";
                break;
            case "forward":
                commandToExecute = "0V;1V0;2V3;";
                break;
            case "backward":
                commandToExecute = "0V;1V1;2V3;";
                break;
        }
        return commandToExecute;
    }
}