package com.example.leonardo.test;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiObjectCallBack;
import com.kii.cloud.storage.callback.KiiPushCallBack;
import com.kii.cloud.storage.callback.KiiUserCallBack;
import com.kii.cloud.storage.KiiPushInstallation.PushBackend;

import org.json.JSONException;
import org.json.JSONObject;

import cn.jpush.android.api.JPushInterface;

public class LoginActivity extends Activity {

    private Button signUp;
    private Button signIn;
    private Button pushInstall;
    private Button bucketSub;
    private Button bucketUnsub;
    private Button bucketTrigger;

    private String REMOTECTRLBUCKET = "remoteControl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //initialize Kii SDK
        Kii.initialize("33483c48", "2af45304ed70f5deb4c206f4e8ef0a98", "https://api-cn3.kii.com/api");
        //cancel any pending notification
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        //initialize jpush interface
        JPushInterface.init(this);

        // If the id is saved in the preference, it skip the registration and just install push.
        String regId = JPushPreference.getRegistrationId(this.getApplicationContext());
        if (regId.isEmpty()) {
            registerJPush();
            Toast.makeText(LoginActivity.this, "Register JPush done", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(LoginActivity.this, "Register JPush done before", Toast.LENGTH_SHORT).show();
        }

        signUp = (Button) findViewById(R.id.btnSignUp);
        signIn = (Button) findViewById(R.id.btnSignIn);
        pushInstall = (Button) findViewById(R.id.btnPushInstall);
        bucketSub = (Button) findViewById(R.id.btnTopicSub);
        bucketUnsub = (Button) findViewById(R.id.btnTopicUnsub);
        bucketTrigger = (Button) findViewById(R.id.btnBucketTrigger);

        signUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                KiiUser.Builder builder = KiiUser.builderWithName("leonardean");
                builder.withEmail("user_123456@example.com");
                builder.withPhone("+819012345678");
                KiiUser user = builder.build();

                user.register(new KiiUserCallBack() {
                    @Override
                    public void onRegisterCompleted(int token, KiiUser user, Exception exception) {
                        if (exception != null) {
                            Toast.makeText(LoginActivity.this, exception.toString(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(LoginActivity.this, "User Register Done as: " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    }
                }, "123123");
            }
        });

        signIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = "leonardean";
                String password = "123123";
                KiiUser.logIn(new KiiUserCallBack() {
                    public void onLoginCompleted(int token, KiiUser user, Exception e) {
                        if (e != null) {
                            Toast.makeText(LoginActivity.this, "Error login: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(LoginActivity.this, "Logged in as " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    }
                }, username, password);
            }
        });

        pushInstall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean development = false;
                final String regId = JPushInterface.getUdid(LoginActivity.this.getApplicationContext());
                KiiUser.pushInstallation(PushBackend.JPUSH, development).install(regId, new KiiPushCallBack() {
                    public void onInstallCompleted(int taskId, Exception e) {
                        if (e != null) {
                            Toast.makeText(LoginActivity.this, "Error install: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(LoginActivity.this, "JPush installation done", Toast.LENGTH_SHORT).show();
                        JPushPreference.setRegistrationId(LoginActivity.this.getApplicationContext(), regId);
                    }
                });
            }
        });

        bucketSub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        bucketUnsub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final KiiBucket bucket = Kii.bucket(REMOTECTRLBUCKET);
                final KiiUser user = KiiUser.getCurrentUser();
                user.pushSubscription().unsubscribeBucket(bucket, new KiiPushCallBack() {
                    @Override
                    public void onUnSubscribeBucketCompleted(int taskId, KiiBucket target, Exception exception) {
                        if (exception != null) {
                            Toast.makeText(LoginActivity.this, "error in bucket unsubscription: "+exception, Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(LoginActivity.this, "bucket unsubscription done", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        bucketTrigger.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        JPushInterface.onResume(this);
    }

    @Override
    protected void onPause() {
        JPushInterface.onPause(this);
        super.onPause();
    }

    private void registerJPush() {
        JPushInterface.resumePush(this.getApplicationContext());
        String regId = JPushInterface.getUdid(this.getApplicationContext());
        JPushInterface.setAlias(this.getApplicationContext(), regId, null);
        JPushPreference.setRegistrationId(this.getApplicationContext(), regId);
    }
}