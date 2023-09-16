package com.libremobileos.desktopmode;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.libremobileos.desktopmode.preferences.ResolutionPreference;
import com.libremobileos.desktopmode.preferences.SeekBarPreference;

import java.util.Objects;

public class PCModeConfigFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    // Constants
    public enum PrefKeys {
        AUTO_RES("pc_mode_auto_resolution", true),
        RES("pc_mode_resolution", null),
        SCALING("pc_mode_scaling", 100),
        SERVICE_BUTTON("pc_mode_service_button", null),
        RES_WIDTH("pc_mode_res_width", 1280),
        RES_HEIGHT("pc_mode_res_height", 720),
        EMULATE_TOUCH("pc_mode_emulate_touch", false),
        RELATIVE_INPUT("pc_mode_relative_input", false),
        MIRROR_INTERNAL("pc_mode_mirror_internal", false),
        AUDIO("pc_mode_audio", true),
        REMOTE_CURSOR("pc_mode_remote_cursor", true),
        CLIPBOARD("pc_mode_clipboard", true);

        final String key;
        final Object defaultValue;

        PrefKeys(String key, Object defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    private SharedPreferences mSharedPreferences;
    private Boolean mAutoResValue;
    private Integer mCustomResWidthValue;
    private Integer mCustomResHeightValue;
    private Integer mScalingValue;

    private SwitchPreference pcModeAutoRes;
    private ResolutionPreference pcModeRes;
    private SeekBarPreference pcModeScaling;
    private Preference pcModeServiceButton;

    private VNCServiceController mVncService;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pc_mode_config_preferences, rootKey);

        mSharedPreferences = requireActivity().getSharedPreferences("PCModeConfigs", MODE_PRIVATE);
        mAutoResValue = mSharedPreferences.getBoolean(PrefKeys.AUTO_RES.key, (Boolean) PrefKeys.AUTO_RES.defaultValue);
        mCustomResWidthValue = mSharedPreferences.getInt(PrefKeys.RES_WIDTH.key, (Integer) PrefKeys.RES_WIDTH.defaultValue);
        mCustomResHeightValue = mSharedPreferences.getInt(PrefKeys.RES_HEIGHT.key, (Integer) PrefKeys.RES_HEIGHT.defaultValue);
        mScalingValue = mSharedPreferences.getInt(PrefKeys.SCALING.key, (Integer) PrefKeys.SCALING.defaultValue);

        initializePreferences();

        mVncService = new VNCServiceController(requireActivity(), this::setServiceButtonTitle);
    }

    private void initializePreferences() {
        pcModeAutoRes = findPreference(PrefKeys.AUTO_RES.key);
        pcModeRes = findPreference(PrefKeys.RES.key);
        pcModeScaling = findPreference(PrefKeys.SCALING.key);
        pcModeServiceButton = findPreference(PrefKeys.SERVICE_BUTTON.key);

        pcModeAutoRes.setOnPreferenceChangeListener(this);
        pcModeRes.setOnPreferenceChangeListener(this);
        pcModeScaling.setOnPreferenceChangeListener(this);
        pcModeServiceButton.setOnPreferenceClickListener(this);

        pcModeAutoRes.setChecked(mAutoResValue);
        pcModeRes.setWidth(mCustomResWidthValue, false);
        pcModeRes.setHeight(mCustomResHeightValue, false);
        pcModeRes.setEnabled(!mAutoResValue);
        pcModeScaling.setValue(mScalingValue);
    }

    private void setServiceButtonTitle(boolean connected) {
        pcModeServiceButton.setTitle(connected ? R.string.pc_mode_service_stop : R.string.pc_mode_service_start);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mVncService.unBind();
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        Context context = getActivity();
        if (context == null) return false;

        if (PrefKeys.SERVICE_BUTTON.key.equals(pref.getKey())) {
            CharSequence title = pref.getTitle();
            if (getString(R.string.pc_mode_service_stop).contentEquals(title)) {
                VNCServiceController.stop(getActivity());
                return true;
            } else if (getString(R.string.pc_mode_service_start).contentEquals(title) ||
                       getString(R.string.pc_mode_service_restart_apply).contentEquals(title) ||
                       getString(R.string.pc_mode_service_apply).contentEquals(title)) {
                handleServiceControl(title);
                return true;
            }
        }
        return false;
    }

    private void handleServiceControl(CharSequence title) {
        if (getString(R.string.pc_mode_service_restart_apply).contentEquals(title)) {
            mVncService.unBind();
            VNCServiceController.stop(getActivity());
            applyChanges();
            VNCServiceController.start(getActivity());
            mVncService = new VNCServiceController(requireActivity(), this::setServiceButtonTitle);
        } else if (getString(R.string.pc_mode_service_apply).contentEquals(title)) {
            applyChanges();
            VNCServiceController.start(getActivity());
            setServiceButtonTitle(true);
        } else {
            VNCServiceController.start(getActivity());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();

        if (key.equals(PrefKeys.AUTO_RES.key)) {
            handleAutoResChange((Boolean) newValue);
            return true;
        } else if (key.equals(PrefKeys.RES.key)) {
            handleResolutionChange();
            return true;
        } else if (key.equals(PrefKeys.SCALING.key)) {
            handleScalingChange((Integer) newValue);
            return true;
        }
        return false;
    }

    private void handleAutoResChange(Boolean newValue) {
        if (!Objects.equals(newValue, mAutoResValue)) {
            pcModeRes.setEnabled(!newValue);
            mAutoResValue = newValue;
            handleChange(true);
        }
    }

    private void handleResolutionChange() {
        boolean changed = pcModeRes.getWidth() != mCustomResWidthValue || pcModeRes.getHeight() != mCustomResHeightValue;
        if (changed) {
            mCustomResWidthValue = pcModeRes.getWidth();
            mCustomResHeightValue = pcModeRes.getHeight();
            handleChange(false);
        }
    }

    private void handleScalingChange(Integer newValue) {
        if (!Objects.equals(newValue, mScalingValue)) {
            mScalingValue = newValue;
            handleChange(false);
        }
    }

    private void handleChange(boolean needRestart) {
        if (mVncService == null || !mVncService.isRunning()) {
            applyChanges();
            setServiceButtonTitle(false);
        } else {
            pcModeServiceButton.setTitle(needRestart ? R.string.pc_mode_service_restart_apply : R.string.pc_mode_service_apply);
        }
    }

    private void applyChanges() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PrefKeys.AUTO_RES.key, mAutoResValue)
              .putInt(PrefKeys.RES_WIDTH.key, mCustomResWidthValue)
              .putInt(PrefKeys.RES_HEIGHT.key, mCustomResHeightValue)
              .putInt(PrefKeys.SCALING.key, mScalingValue)
              .apply();
    }
}
