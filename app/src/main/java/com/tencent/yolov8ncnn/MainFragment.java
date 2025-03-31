package com.tencent.yolov8ncnn;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {
    private AlertDialog dialog;
    private MediaProjectionManager mediaProjectionManager;
    public static final int REQUEST_MEDIA_PROJECTION = 18;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        if (!checkOverlayDisplayPermission()) {
            requestOverlayDisplayPermission();
        }

        Button initButton = view.findViewById(R.id.init);
        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initMediaProjectionManager();
            }
        });

        return view;
    }

    private void requestOverlayDisplayPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setTitle("需要打开悬浮窗权限");
        builder.setMessage("在系统设置中为本应用打开悬浮窗权限,或允许该应用显示在其他应用上层。");
        builder.setPositiveButton("打开设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireActivity().getPackageName()));
                startActivityForResult(intent, RESULT_OK);
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    private boolean checkOverlayDisplayPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(getContext());
        }
        return true;
    }

    private void initMediaProjectionManager() {
        if (mediaProjectionManager != null) return;
        mediaProjectionManager = (MediaProjectionManager) requireActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Intent intent = new Intent(requireContext(), FloatingWindowService.class);
            intent.putExtra("code", resultCode);
            intent.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(intent);
            } else {
                requireActivity().startService(intent);
            }
            requireActivity().finish();
        }
    }
}