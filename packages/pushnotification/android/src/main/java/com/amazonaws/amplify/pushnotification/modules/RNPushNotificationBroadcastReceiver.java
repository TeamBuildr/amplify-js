/*
 * Copyright 2017-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.amplify.pushnotification.modules;

import java.util.HashMap;
import java.util.Map;
import android.content.BroadcastReceiver;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;

import com.amazonaws.amplify.pushnotification.modules.RNPushNotificationJsDelivery;
/**
 * The Amazon Pinpoint push notification receiver.
 */
public class RNPushNotificationBroadcastReceiver extends BroadcastReceiver {

    private final static String LOG_TAG = "RNPushNotificationBroadcastReceiver";

    private Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openApp(Context context, Bundle notification) {
        Class intentClass = getMainActivityClass(context);
        String deeplink = notification.getString("pinpoint.deeplink");
        Boolean hasDeeplink = deeplink != null;

        // if pinpoint data has a deeplink, we use an ACTION_VIEW intent (https://developer.android.com/reference/android/content/Intent#ACTION_VIEW)
        // using this type of Intent is what native android Linking.getInitialUrl expects (https://github.com/facebook/react-native/blob/v0.64.3/ReactAndroid/src/main/java/com/facebook/react/modules/intent/IntentModule.java#L59)
        // this allows us to handle deep linking on cold push
        Intent launchIntent = hasDeeplink ? new Intent(Intent.ACTION_VIEW, Uri.parse(deeplink)) : new Intent(context, intentClass);        if (launchIntent == null) {
            Log.e(LOG_TAG, "Couldn't get app launch intent for campaign notification.");
            return;
        }

        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        launchIntent.setPackage(null);
        context.startActivity(launchIntent);    
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "broadcaster received");

        // send the message to device emitter
        // Construct and load our normal React JS code bundle
        ReactInstanceManager mReactInstanceManager = ((ReactApplication) context.getApplicationContext()).getReactNativeHost().getReactInstanceManager();
        ReactContext reactContext = mReactInstanceManager.getCurrentReactContext();
        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery((ReactApplicationContext) reactContext);
        jsDelivery.emitNotificationOpened(intent.getBundleExtra("notification"));

        openApp(context, intent.getBundleExtra("notification"));
    }
}