package com.android.settings;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog.Builder;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreferenceCompat;
import com.android.settings.DevelopmentPasswordManager;

public class ChangeDevelopmentPasswordPreference extends CustomDialogPreferenceCompat {
    private static final String TAG = "ChangeDevelopmentPasswordPreference";

    private DevelopmentPasswordManager mPasswordManager;

    public ChangeDevelopmentPasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
                                          DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);

        builder.setTitle(R.string.title_change_development_password);
        mPasswordManager = DevelopmentPasswordManager.getInstance();
        final View dialogView = View.inflate(getContext(), R.layout.change_password_dialog, null);

        // Find views within dialog layout
        TextView txt_status = dialogView.findViewById(R.id.text_status_confirm);
        EditText editTextOldPassword = dialogView.findViewById(R.id.edit_text_old_password);
        EditText editTextNewPassword = dialogView.findViewById(R.id.edit_text_new_password);
        EditText editTextConfirmPassword = dialogView.findViewById(R.id.edit_text_confirm_password);

        editTextOldPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String oldPassword = editTextOldPassword.getText().toString();
                String newPassword = editTextNewPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();

                if (mPasswordManager.calculateHmacSHA256(oldPassword, "meta").equals(mPasswordManager.loadPassword())) {
                    if(newPassword.length() == 0 && confirmPassword.length() == 0) {
                        txt_status.setText(R.string.txt_status_old_password_correct);
                        txt_status.setTextColor(Color.GREEN);
                    }
                    else {
                        if (newPassword.equals(confirmPassword)) {
                            txt_status.setText(R.string.txt_status_confirm_match);
                            txt_status.setTextColor(Color.GREEN);
                        } else {
                            txt_status.setText(R.string.txt_status_confirm_not_match);
                            txt_status.setTextColor(Color.RED);
                        }
                    }
                } else {
                    txt_status.setText(R.string.txt_status_old_password_incorrect);
                    txt_status.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        editTextNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String oldPassword = editTextOldPassword.getText().toString();
                String newPassword = editTextNewPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();

                if (mPasswordManager.calculateHmacSHA256(oldPassword, "meta").equals(mPasswordManager.loadPassword())) {
                    if(newPassword.length() == 0 && confirmPassword.length() == 0) {
                        txt_status.setText(R.string.txt_status_old_password_correct);
                        txt_status.setTextColor(Color.GREEN);
                    }
                    else {
                        if (newPassword.equals(confirmPassword)) {
                            txt_status.setText(R.string.txt_status_confirm_match);
                            txt_status.setTextColor(Color.GREEN);
                        } else {
                            txt_status.setText(R.string.txt_status_confirm_not_match);
                            txt_status.setTextColor(Color.RED);
                        }
                    }
                } else {
                    txt_status.setText(R.string.txt_status_old_password_incorrect);
                    txt_status.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        editTextConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String oldPassword = editTextOldPassword.getText().toString();
                String newPassword = editTextNewPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();

                if (mPasswordManager.calculateHmacSHA256(oldPassword, "meta").equals(mPasswordManager.loadPassword())) {
                    if(newPassword.length() == 0 && confirmPassword.length() == 0) {
                        txt_status.setText(R.string.txt_status_old_password_correct);
                        txt_status.setTextColor(Color.GREEN);
                    }
                    else {
                        if (newPassword.equals(confirmPassword)) {
                            txt_status.setText(R.string.txt_status_confirm_match);
                            txt_status.setTextColor(Color.GREEN);
                        } else {
                            txt_status.setText(R.string.txt_status_confirm_not_match);
                            txt_status.setTextColor(Color.RED);
                        }
                    }
                } else {
                    txt_status.setText(R.string.txt_status_old_password_incorrect);
                    txt_status.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Set up buttons
        builder.setPositiveButton(R.string.positive_button_change_password, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle password change logic here
                String oldPassword = editTextOldPassword.getText().toString();
                String newPassword = editTextNewPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();
                if(confirmPassword.length() > 0 && confirmPassword.equals(newPassword) && mPasswordManager.calculateHmacSHA256(oldPassword, "meta").equals(mPasswordManager.loadPassword())) {
                    mPasswordManager.savePassword(newPassword);
                    Toast.makeText(getContext(), R.string.txt_change_password_success, Toast.LENGTH_SHORT).show();
                    Log.d("Password", "changed password: " + oldPassword + " - " + newPassword);
                } else {
                    Toast.makeText(getContext(), R.string.txt_change_password_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.negative_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setView(dialogView);
    }
}