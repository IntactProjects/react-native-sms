package com.tkporter.sendsms;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.net.Uri;
import android.provider.Telephony;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

public class SendSMSModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext reactContext;
    private Callback callback = null;
    private static final int REQUEST_CODE = 5235;

    public SendSMSModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "SendSMS";
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            sendCallback(false, true, false);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    public void sendCallback(Boolean completed, Boolean cancelled, Boolean error) {
        if (callback != null) {
            callback.invoke(completed, cancelled, error);
            callback = null;
        }
    }

    @ReactMethod
    public void send(ReadableMap options, final Callback callback) {
        try {
            this.callback = callback;
            new SendSMSObserver(reactContext, this, options).start();

            String body = options.hasKey("body") ? options.getString("body") : "";
            ReadableArray recipients = options.hasKey("recipients") ? options.getArray("recipients") : null;

            Intent sendIntent;
            String recipientString = "";

            if (recipients != null) {
                String separator = ",";

                //Samsung for some reason uses commas and not semicolons as a delimiter
                if(android.os.Build.MANUFACTURER.equalsIgnoreCase("Samsung")){
                    separator = ",";
                }

                for (int i = 0; i < recipients.size(); i++) {
                    recipientString += recipients.getString(i);
                    recipientString += separator;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                sendIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + recipientString));
            } else {
                sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.setType("vnd.android-dir/mms-sms");
                sendIntent.putExtra("address", recipientString);
            }

            sendIntent.putExtra("sms_body", body);
            sendIntent.putExtra(sendIntent.EXTRA_TEXT, body);
            sendIntent.putExtra("exit_on_sent", true);

            reactContext.startActivityForResult(sendIntent, REQUEST_CODE, sendIntent.getExtras());
        } catch (Exception e) {
            sendCallback(false, false, true);
            throw e;
        }
    }

}
