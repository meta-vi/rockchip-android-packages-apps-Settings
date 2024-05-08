package com.android.settings;

import android.os.Build;
import android.content.SharedPreferences;
import android.content.Context;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class DevelopmentPasswordManager {
    private Context mContext;
    private static DevelopmentPasswordManager single_instance = null;

    public static synchronized DevelopmentPasswordManager getInstance()
    {
        if (single_instance == null)
            single_instance = new DevelopmentPasswordManager();

        return single_instance;
    }

    private DevelopmentPasswordManager() {}
    public void setContext(Context context) {
        mContext = context;
    }

    public void savePassword(String password) {
        try {
            if(mContext != null) {
                SharedPreferences sharedPreferences = mContext.getSharedPreferences("developer_options_password", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("developer_options_password_key", calculateHmacSHA256(password, "meta"));
                editor.apply();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String loadPassword() {
        String savedText = null;
        if(mContext != null) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("developer_options_password", Context.MODE_PRIVATE);
            savedText = sharedPreferences.getString("developer_options_password_key", calculateHmacSHA256("meta@2024", "meta"));
        }
        return savedText;
    }

    // Function to calculate HMAC with SHA-256 algorithm
    public String calculateHmacSHA256(String data, String key) {
        try {
            // Create a new SecretKeySpec with the given key and algorithm
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");

            // Get an instance of Mac with HmacSHA256 algorithm
            Mac mac = Mac.getInstance("HmacSHA256");

            // Initialize the Mac instance with the SecretKeySpec
            mac.init(secretKeySpec);

            // Calculate the HMAC of the input data
            byte[] hmacBytes = mac.doFinal(data.getBytes());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(hmacBytes);
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
}


