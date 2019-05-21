package com.ofilm.utils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.ofilm.camera.R;

public class ConfirmDialogUtil extends DialogFragment {

    private static final String MESSAGE = "message";
    private static final String PERMISSIONS = "permissions";
    private static final String REQUEST_CODE = "request_code";
    private static final String NOT_GRANTD_MESSAGE = "not_granted_message";

    /*
    *
    *  message: 权限提示语资源id
    *  permissions: 权限名称
    *  requestCode: 请求码,用于权限申请
    *  notGrantedMessage: 认证失败提示语资源id
    *
    * */
    public static ConfirmDialogUtil newInstance(int message, String[] permissions,
                                                int requestCode, int notGrantedMessage){

        ConfirmDialogUtil dialog = new ConfirmDialogUtil();
        Bundle args = new Bundle();
        args.putInt(MESSAGE, message);
        args.putStringArray(PERMISSIONS, permissions);
        args.putInt(REQUEST_CODE, requestCode);
        args.putInt(NOT_GRANTD_MESSAGE, notGrantedMessage);

        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Bundle args = getArguments();

        return new AlertDialog.Builder(getActivity())
                .setMessage(args.getInt(MESSAGE))
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] permissions = args.getStringArray(PERMISSIONS);
                                if(permissions == null){
                                    throw new IllegalArgumentException();
                                }

                                ActivityCompat.requestPermissions(getActivity(), permissions, args.getInt(REQUEST_CODE));
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), R.string.no_permission, Toast.LENGTH_SHORT).show();

                            }
                        })
                .create();
    }
}
