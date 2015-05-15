package com.example.leonardo.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.exception.app.AppException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import cn.jpush.android.api.JPushInterface;

/**
 * Created by leonardo on 10/10/2014.
 */
public class KiiPushBroadcastReceiver extends BroadcastReceiver {

    private int YOURAPP_NOTIFICATION_ID = 1234567890;

    @Override
    public void onReceive(Context context, Intent intent) {
        String jpushMessageType = intent.getAction();
        if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(jpushMessageType)) {
            Bundle extras = intent.getExtras();
            try {
                JSONObject jsonObj = new JSONObject(extras.get("cn.jpush.android.MESSAGE").toString());
                String origin = jsonObj.get("origin").toString();
                String scope = jsonObj.get("objectScopeType").toString();
                String type = jsonObj.get("type").toString();
                String bucketID = jsonObj.get("bucketID").toString();
                String objectID = jsonObj.get("objectID").toString();
                if (jsonObj.get("origin").toString().equals("EVENT")) {  //this means push to app
                    GetCommand getCommand = new GetCommand();
                    getCommand.execute(bucketID, objectID);
                    String command = getCommand.get();
                    displayNotification(context, R.drawable.ic_launcher, "Remote Control", command);
                    Toast.makeText(context, "Notification Received!" +
                            "\nOrigin:" + origin + "\nType: " + type +
                            "\nScope:" + scope + "\nBucketID:" + bucketID +
                            "\nObjectID:" + objectID +
                            "\nCommand:" + command, Toast.LENGTH_LONG).show();
                    getCommand.cancel(true);
                } else {
                    //direct push and push to user not considered
                }
            } catch (JSONException e) {
                Log.e("JSON Error", e.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayNotification(Context context, int statusBarIconID, String title, String text) {
        long[] vibrate = {0, 100, 200, 300};
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(statusBarIconID)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSound(alarmSound)
                        .setVibrate(vibrate);

        Intent resultIntent = new Intent(context, LoginActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(LoginActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(YOURAPP_NOTIFICATION_ID, mBuilder.build());
    }

    private class GetCommand extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String...strings) {
            KiiObject object = KiiObject.createByUri(Uri.parse("kiicloud://buckets/"
                    + strings[0] + "/objects/" + strings[1]));
            JSONObject command = new JSONObject();
            try {
                object.refresh();
                command = object.getJSONObject("command");
            } catch (IOException e) {
                // Handle error
            } catch (AppException e) {
                // Handle error
            }
            return command.toString();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}