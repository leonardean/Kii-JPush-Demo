#JPush-the old way vs. the good way
##the old way:
Integrated with Kii Cloud, Jpush notification takes serveral steps to implement:

###1. Jpush initializaion 
~~~java
JPushInterface.init(this);
~~~
####Wrap suggestion:
The step of jpush initialization could be wrapped with Kii SDK initialization, so that the developer does not need to explicitly call it.

###2. User login
~~~java
String username = "leonardean";
String password = "123123";
KiiUser.logIn(new KiiUserCallBack() {
    public void onLoginCompleted(int token, KiiUser user, Exception e) {
        if (e != null) {
            Toast.makeText(LoginActivity.this, "Error login: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(LoginActivity.this, "Login successful as: " + user.getUsername(), Toast.LENGTH_LONG).show();
    }
}, username, password);
~~~
####Wrap suggestion:
Letting callback function to be the first parameter does not seem elegant.

###3. Install user phone

~~~java
String regId = JPushPreference.getRegistrationId(LoginActivity.this.getApplicationContext());
// If the id is saved in the preference, it skip the registration
if (regId.isEmpty()) {
    JPushInterface.resumePush(LoginActivity.this.getApplicationContext());
    final String pushRegId = JPushInterface.getUdid(LoginActivity.this.getApplicationContext());
    JPushInterface.setAlias(LoginActivity.this.getApplicationContext(), pushRegId, null);
    // install user phone
    KiiUser.pushInstallation(PushBackend.JPUSH, DEVELOPMENT).install(pushRegId, new KiiPushCallBack() {
        public void onInstallCompleted(int taskId, Exception e) {
            if (e != null) {
                Toast.makeText(LoginActivity.this, "Error install: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            // if all succeeded, locally save registration ID to preference.
            JPushPreference.setRegistrationId(LoginActivity.this.getApplicationContext(), pushRegId);
            Toast.makeText(LoginActivity.this, "Device binding done", Toast.LENGTH_SHORT).show();
        }
    });
} else {
    Toast.makeText(LoginActivity.this, "Device binding done before\n So... nothing has just been done", Toast.LENGTH_SHORT).show();
}
~~~
####Wrap suggestion:
This could be wrapped so that it is automatically done after user login.

###4. Subscribe to bucket
~~~java
final KiiBucket bucket = Kii.bucket(REMOTECTRLBUCKET);
final KiiUser user = KiiUser.getCurrentUser();
user.pushSubscription().subscribeBucket(bucket, new KiiPushCallBack() {
    @Override
    public void onSubscribeBucketCompleted(int taskId, KiiBucket target, Exception exception) {
        if (exception != null) {
            Toast.makeText(LoginActivity.this, "error in bucket subscription: "+exception, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(LoginActivity.this, "bucket subscription done", Toast.LENGTH_SHORT).show();
    }
});
~~~
####Wrap suggestion
This could be done after user setting an ownership with a device since normally this is a one time operation. Still, the method should be kept in a more elegant way (hide the bucket info) so that user may choose to mute(unsubscribe) or relisten(subscribe again) a device. Also, bucket unsubscription could be wrapped so that it's auto executed when ownership canceled.

###5. Create object to trigger push
~~~java
KiiObject object = Kii.bucket(REMOTECTRLBUCKET).object();
JSONObject jsonObject = new JSONObject();
try {
    jsonObject.put("color", "#666666");
    jsonObject.put("brightness", 50);
    jsonObject.put("temperature", 30);
    jsonObject.put("status", "ON");
} catch (JSONException e) {
    e.printStackTrace();
}
object.set("command", jsonObject);
object.save(new KiiObjectCallBack() {
    @Override
    public void onSaveCompleted(int token, KiiObject object, Exception exception) {
        if (exception != null) {
            Toast.makeText(LoginActivity.this, "Error in storing object: " + exception.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        Log.i("Object", "object store done");
        Log.d("Object", object.toUri().toString());
    }
});
~~~
####Wrap suggestion
Ideally, we should hide the concept of bucket, object from developers so that all they need to do the sending control conmmands. After wrapping, the code structure may look very similar, but still worth it because it changes how developers feel about our API (more tangible).

###6. Receive push to do logics
~~~java
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
~~~
####Wrap suggestion
The whole BroadcastReceiver needs to be wrapped. Developers are only interested in doing logics after receiving the notification and its containing info. They probably dont want to pay much attention about how to get the data from object and how the notification displays. Notification display settings could be just open, but it's better we have a wrapped version of it with more a simple(default) setting options and ways.(sound, vibrate, icons, text ...)
##the good way:
###1. Jpush initialization
~~~java
KiiIOT.initialize(this, "33483c48", "2af45304ed70f5deb4c206f4e8ef0a98", "https://api-cn3.kii.com/api");
~~~
This function call initialize KiiSDK and JPush at a time

###2. User login and phone install
~~~java
String username = "leonardean";
String password = "123123";
KiiIOTUser.logIn(username, password, new KiiIOTUserCallBack() {
    public void onLoginCompleted(int token, KiiUser user, Exception e, Exception pushInstallException ) {
        if (e != null) {
            Toast.makeText(LoginActivity.this, "Error login: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (pushInstallException != null) {
            Toast.makeText(LoginActivity.this, "Error install push: " + pushInstallException.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(LoginActivity.this, "Login successful as: " + user.getUsername(), Toast.LENGTH_LONG).show();
    }
});
~~~
When login, the function also installs jpush for the user phone and returns push install exception if there is.
###3. Bucket Subscription
~~~java
KiiThing.loadWithVendorThingID("rBnvSPOXBDF9r29GJeGS", new KiiCallback<KiiThing>() {
  @Override
  public void onComplete(KiiThing result, Exception e) {
    if (e != null) {
      // Error handling
      return;
    }
    result.registerOwner(KiiIOTUser.getCurrentUser(), new KiiCallback<KiiThingOwner>() {
      @Override
      public void onComplete(KiiThingOwner result, Exception e, Exception deviceListenException) {
        if (e != null) {
          // Error handling
          return;
        }
        if (deviceListenException != null) {
      		// Error handling
      		return;
    	}
      }
    });
  }
});
~~~
The bucket used for subscription to get backward push from device should (ideally) be a user scope bucket, and its name should be hidden from developer. All developers need to do is setup an ownership and the ACL change (letting thing to create object in that user scope bucket) and bucket subscription is done for them. Vice versa, when a user cancels an ownership, the opposite operation needs to be done.

Apart from the above push, there might need other kinds of pushes, therefore, other preset buckets. We can say the above is "control command feedback push", and there needs other pushes for "gernal report" (how/when to use it, up to developer. e.g. when a user set a schedule task, the thing let the user know when it starts working) and "status checker" if we fail to make this functionality in MQTT(user manually check the status of a device). User might want to mute a device, so that he won't get periodical "report push" from a device. The user can still check them though cuz they are objects in bucket anyway.

~~~java
final KiiIOTUser user = KiiIOTUser.getCurrentUser();
KiiThing.loadWithVendorThingID("rBnvSPOXBDF9r29GJeGS", new KiiCallback<KiiThing>() {
  @Override
  public void onComplete(KiiThing result, Exception e) {
    if (e != null) {
      // Error handling
      return;
    }
    user.muteThing(result, new KiiCallback<KiiThingMute>() {
      @Override
      public void onComplete(KiiThingMute result, Exception e) {
        if (e != null) {
          // Error handling
          return;
        }
      }
    });
  }
});
~~~
Vice versa, there needs to be a `listenThing()` function.

###4. Control Command send
~~~java
KiiThing.loadWithVendorThingID("rBnvSPOXBDF9r29GJeGS", new KiiCallback<KiiThing>() {
  @Override
  public void onComplete(KiiThing result, Exception e) {
    if (e != null) {
      // Error handling
      return;
    }
    KiiCommand command = result.command();
    command.setColor("#666666");
    command.setBrightness(50);
    command.setTemperature(30);
    command.setStatus("ON");
    
    command.send(new KiiCommandCallBack(){
    	@Override
    	public void onCommandSendCompleted(int token, KiiCommand, command, Exception e){
    		if (e != null){
    			//exception handle
    		}
    	}
    });
  }
});
~~~
As mentioned before, the concept of bucket and object creation needs to be hidden to provide a more intuitive way: get a thing, create a command for that thing, set some params, send the command. (Ideally, the feedback should include how the thing react in response to the command, we need to either find a way of doing it or use push back)
###5. Push receive
~~~java
public class HelloWorldBroadcastReceiver extends KiiPushBroadcastReceiver {    

    @Override
    public void onReceive(Context context, Intent intent, KiiThingStatus status) {
        //get response status and do business logics
        KiiPushBroadcastReceiver.displayDefaultNotification(context, icon, title, text); //we can have something like this
    }  
}
~~~
Developers are only interested in getting the content of the push from thing, so we wrap everthing else for them including getting push info, determine push type (push to app, push to user, direct push... we only consider push to app, which is triggered by bucket object creation), getting object, wrapping the object content as KiiThingStatus. We shall also provide them a default way to display notification.

If we use push to get the response from a thing after sending a control command, we also need to match a commandID to make sure the push is in response for which command 
