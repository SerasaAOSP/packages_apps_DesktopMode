package com.libremobileos.desktopmode;

import static android.content.Context.MODE_PRIVATE;
import static com.libremobileos.desktopmode.PCModeAdvancedConfigFragment.*;
import static com.libremobileos.desktopmode.PCModeConfigFragment.*;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;

import com.libremobileos.vncflinger.IVncFlinger;

@SuppressLint("WrongConstant")
public class VNCServiceController extends BroadcastReceiver {

    private static final String VNC_FLINGER_PACKAGE = "com.libremobileos.vncflinger";
    private static final String VNC_FLINGER_CLASS = "com.libremobileos.vncflinger.VncFlinger";
    private static final String ACTION_START = "com.libremobileos.desktopmode.START";
    private static final String SHARED_PREF_NAME = "PCModeConfigs";

    private Context mContext;
    private IVncFlinger mService;
    private VNCServiceConnection mServiceConnection;
    private VNCServiceListener mListener;

    public VNCServiceController() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_START.equals(intent.getAction())) {
            start(context);
        }
    }

    public interface VNCServiceListener {
        void onServiceEvent(boolean connected);
    }

    public VNCServiceController(Context context, VNCServiceListener listener) {
        mContext = context;
        mListener = listener;
        mServiceConnection = new VNCServiceConnection();
        bindServiceToContext(context);
    }

    public void unBind() {
        mContext.unbindService(mServiceConnection);
    }

    private void bindServiceToContext(Context context) {
        Intent intent = createIntentForVncFlinger();
        context.bindService(intent, mServiceConnection, 0);
    }

    private static Intent createIntentForVncFlinger() {
        Intent intent = new Intent();
        intent.setClassName(VNC_FLINGER_PACKAGE, VNC_FLINGER_CLASS);
        return intent;
    }

    class VNCServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = IVncFlinger.Stub.asInterface(binder);
            if (mListener != null) {
                mListener.onServiceEvent(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            bindServiceToContext(mContext);
            if (mListener != null) {
                mListener.onServiceEvent(false);
            }
        }
    }

    public static void start(Context context) {
        Intent intent = createIntentForVncFlinger();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);

        // General
        boolean autoResize = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.AUTO_RES.key, 
                                                          (Boolean) PCModeConfigFragment.PrefKeys.AUTO_RES.defaultValue);
        int width = sharedPreferences.getInt(PCModeConfigFragment.PrefKeys.RES_WIDTH.key, 
                                             (Integer) PCModeConfigFragment.PrefKeys.RES_WIDTH.defaultValue);
        int height = sharedPreferences.getInt(PCModeConfigFragment.PrefKeys.RES_HEIGHT.key, 
                                              (Integer) PCModeConfigFragment.PrefKeys.RES_HEIGHT.defaultValue);
        int scale = sharedPreferences.getInt(PCModeConfigFragment.PrefKeys.SCALING.key, 
                                             (Integer) PCModeConfigFragment.PrefKeys.SCALING.defaultValue);

        // Advanced
        boolean emulateTouchValue = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.EMULATE_TOUCH.key, 
                                                                (Boolean) PCModeConfigFragment.PrefKeys.EMULATE_TOUCH.defaultValue);
        boolean relativeInputValue = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.RELATIVE_INPUT.key, 
                                                                 (Boolean) PCModeConfigFragment.PrefKeys.RELATIVE_INPUT.defaultValue);
        boolean mirrorInternalValue = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.MIRROR_INTERNAL.key, 
                                                                  (Boolean) PCModeConfigFragment.PrefKeys.MIRROR_INTERNAL.defaultValue);
        boolean audioValue = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.AUDIO.key, 
                                                          (Boolean) PCModeConfigFragment.PrefKeys.AUDIO.defaultValue);
        boolean remoteCursorValue = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.REMOTE_CURSOR.key, 
                                                                (Boolean) PCModeConfigFragment.PrefKeys.REMOTE_CURSOR.defaultValue);
        boolean clipboard = sharedPreferences.getBoolean(PCModeConfigFragment.PrefKeys.CLIPBOARD.key, 
                                                         (Boolean) PCModeConfigFragment.PrefKeys.CLIPBOARD.defaultValue);
        int dpi = 160 * scale / 100;
        if (!autoResize) {
            intent.putExtra("width", width);
            intent.putExtra("height", height);
        }
        intent.putExtra("dpi", dpi);
        intent.putExtra("allowResize", autoResize);
        intent.putExtra("emulateTouch", emulateTouchValue);
        intent.putExtra("useRelativeInput", relativeInputValue);
        intent.putExtra("mirrorInternal", mirrorInternalValue);
        intent.putExtra("hasAudio", audioValue);
        intent.putExtra("remoteCursor", remoteCursorValue);
        intent.putExtra("clipboard", clipboard);

        intent.putExtra("intentEnable", true);
        intent.putExtra("intentPkg", "com.libremobileos.desktopmode");
        intent.putExtra("intentComponent", "com.libremobileos.desktopmode.PCModeConfigActivity");

        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = createIntentForVncFlinger();
        context.stopService(intent);
    }

    public boolean isRunning() {
        if (mService != null) {
            try {
                return mService.isRunning();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
