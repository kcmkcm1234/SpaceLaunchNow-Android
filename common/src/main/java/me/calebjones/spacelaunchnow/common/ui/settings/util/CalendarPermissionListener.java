package me.calebjones.spacelaunchnow.common.ui.settings.util;


import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import androidx.preference.PreferenceFragmentCompat;
import me.calebjones.spacelaunchnow.common.ui.settings.SettingsFragment;
import me.calebjones.spacelaunchnow.common.ui.settings.fragments.GeneralFragment;

public class CalendarPermissionListener implements MultiplePermissionsListener {

    private final SettingsFragment activity;

    public CalendarPermissionListener(SettingsFragment activity) {
        this.activity = activity;
    }

    @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
        for (PermissionGrantedResponse response : report.getGrantedPermissionResponses()) {
            activity.showPermissionGranted(response.getPermissionName());
        }

        for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
            activity.showPermissionDenied(response.getPermissionName(), response.isPermanentlyDenied());
        }
    }

    @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                             PermissionToken token) {
        activity.showPermissionRationale(token);
    }
}
