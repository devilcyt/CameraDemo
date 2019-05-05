package com.example.camera.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

public class DialogUtil {

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String ARG_MESSAGE = "message";

    public static class ConfirmDialog extends DialogFragment {

        public static DialogUtil.ConfirmDialog newInstance(String message){
            DialogUtil.ConfirmDialog dialog = new DialogUtil.ConfirmDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            final Fragment fragment = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("申请相机权限")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fragment.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = fragment.getActivity();
                            if(activity != null){
                                activity.finish();
                            }
                        }
                    })
                    .create();
        }
    }

    public static class ErrorDialog extends DialogFragment{


        public static DialogUtil.ErrorDialog newInstance(String message){
            DialogUtil.ErrorDialog dialog = new DialogUtil.ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    }).create();
        }
    }
}
