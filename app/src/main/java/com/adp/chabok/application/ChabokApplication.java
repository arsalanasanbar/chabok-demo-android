package com.adp.chabok.application;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.adp.chabok.R;
import com.adp.chabok.activity.MainActivity;
import com.adp.chabok.activity.WallActivity;
import com.adp.chabok.common.Constants;
import com.adp.chabok.common.Utils;
import com.adp.chabok.data.ChabokDAO;
import com.adp.chabok.data.ChabokDAOImpl;
import com.adpdigital.push.AdpPushClient;
import com.adpdigital.push.ChabokNotification;
import com.adpdigital.push.DeliveryMessage;
import com.adpdigital.push.NotificationHandler;
import com.adpdigital.push.PushMessage;
import com.adpdigital.push.location.LocationManager;
import com.adpdigital.push.location.OnLocationUpdateListener;
import com.google.android.gms.common.ConnectionResult;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static com.adp.chabok.common.Constants.CAPTAIN_NAME;
import static com.adp.chabok.common.Constants.CHANNEL_NAME;
import static com.adp.chabok.common.Constants.STATUS_DIGGING;

public class ChabokApplication extends Application implements OnLocationUpdateListener {

    private static final String TAG = "ChabokApplication";
    private final static int SUMMARY_NOTIFICATION_LIMIT = 1;
    private static final String NOTIFICATION_GROUP_KEY = "group-key";
    private static ChabokApplication instance;
    private AdpPushClient adpPush = null;
    private ArrayList<String> lines = new ArrayList<>();
    private boolean buildNotification = true;

    private LocationManager locationManger;
    private Location mCurrentLocation;
    private String eventName = "";

    private Handler handler = new Handler();
    private boolean handlerHasTask = false;
    private NotificationCompat.Builder compactBuilder;

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public static ChabokApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getPushClient(MainActivity.class);
        instance = this;
        adpPush.addListener(this);

    }

    public void clearMessages() {
        lines.clear();
    }

    public synchronized AdpPushClient getPushClient(Class activityClass) {
        try {
            if (adpPush == null) {
                adpPush = AdpPushClient.init(
                        getApplicationContext(),
                        activityClass,
                        Constants.APP_ID,
                        Constants.API_KEY,
                        Constants.USER_NAME,
                        Constants.PASSWORD
                );

                adpPush.setDevelopment(Constants.DEV_MODE);
                adpPush.enableDeliveryTopic();
                adpPush.addListener(this);


                String clientNo = adpPush.getUserId();
                if (!"".equals(clientNo)) {
                    adpPush.register(clientNo, new String[]{CHANNEL_NAME, Constants.CAPTAIN_CHANNEL_NAME});
                }
            }

            NotificationHandler nh = new NotificationHandler() {

                @Override
                public Class getActivityClass(ChabokNotification chabokNotification) {
                    if (chabokNotification.getMessage() != null && chabokNotification.getMessage().getChannel() != null) {
                        String topic = chabokNotification.getMessage().getChannel();
                        return topic != null && topic.contains(Constants.CAPTAIN_NAME) ? MainActivity.class : WallActivity.class;
                    } else return WallActivity.class;

                }

                @Override
                public boolean buildNotification(ChabokNotification chabokNotification, final NotificationCompat.Builder builder) {

                    PushMessage pushMessage = chabokNotification.getMessage();

                    if (pushMessage != null) {

                        buildNotification = !messageIsForCurrentActivity(pushMessage) && decideAndBuildNotifications(pushMessage, builder);

                    } else {
                        lines.add(chabokNotification.getText());

                    }

                    compactBuilder = builder;
                    final int delay = 2000;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!handlerHasTask && lines.size() > 0 && buildNotification && compactBuilder != null) {
                                Log.i("handlerPostDelayed ", "<<<<<<<<<<<<<<<<<<<");
                                handlerHasTask = true;
                                compactNotifications();
                            }
                        }

                    }, delay);

                    return false;
                }
            };
            adpPush.addNotificationHandler(nh);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return adpPush;
    }

    private boolean messageIsForCurrentActivity(PushMessage pushMessage) {

        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> tasks;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tasks = am.getAppTasks();
            ComponentName cn = tasks.size() > 0 ? tasks.get(0).getTaskInfo().topActivity : null;

            if (cn != null) {
                return (pushMessage.getChannel().contains(CAPTAIN_NAME)
                        && cn.getClassName().equals(MainActivity.class.getName())
                        && MainActivity.INBOX_FRAGMENT.equals(MainActivity.currentFragmentTag))
                        || (pushMessage.getChannel().contains(CHANNEL_NAME) && cn.getClassName().equals(WallActivity.class.getName()));
            }
        }

        return false;
    }

    private boolean decideAndBuildNotifications(PushMessage pushMessage, NotificationCompat.Builder builder) {

        if (pushMessage.getData() != null && pushMessage.getSenderId() != null) {

            if (pushMessage.getSenderId().trim().equals(adpPush.getUserId())) {   // it's users own message
                return false;

            } else if (!pushMessage.getSenderId().trim().equals("")) {  // it's from users and have proper sender name

                builder.setTicker(pushMessage.getData().optString(Constants.KEY_NAME) + ": " + pushMessage.getBody());
                builder.setContentText(pushMessage.getData().optString(Constants.KEY_NAME) + ": " + pushMessage.getBody());

            }

        } else {   //it's from server

            builder.setTicker(getResources().getString(R.string.app_name) + ": " + pushMessage.getBody());
            builder.setContentText(getResources().getString(R.string.app_name) + ": " + pushMessage.getBody());

        }

        lines.add(pushMessage.getBody());
        return true;
    }


    private void compactNotifications() {

        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
        compactBuilder.setSmallIcon(getNotificationIcon());

        int messagesCount = lines.size();
        Log.i("messagesCount ****** ", String.valueOf(messagesCount) + " ******");

        if (messagesCount > SUMMARY_NOTIFICATION_LIMIT) {

            mNotificationManager.cancelAll();

            compactBuilder.setGroup(NOTIFICATION_GROUP_KEY);
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                    .setSummaryText(messagesCount + " new messages")
                    .setBigContentTitle(getString(R.string.app_name));
            int count = 0;
            for (int i = lines.size() - 1; i >= 0 && count < 5; i--) {
                inboxStyle.addLine(lines.get(i));
                count++;
            }
            compactBuilder.setContentTitle(getString(R.string.app_name))
                    .setContentText(messagesCount + " new messages")
                    .setStyle(inboxStyle)
                    .setNumber(messagesCount)
                    .setGroupSummary(true)
                    .setGroupAlertBehavior(GROUP_ALERT_SUMMARY);

            Notification notification = compactBuilder.build();
            mNotificationManager.notify(messagesCount, notification);

        } else {

            Notification notification = compactBuilder.build();
            mNotificationManager.notify(messagesCount, notification);
        }

        ring();
        clearMessages();
        handlerHasTask = false;
    }


    private void ring() {

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unused")
    public void onEvent(DeliveryMessage message) {

        ChabokDAO dao = ChabokDAOImpl.getInstance(this);
        dao.updateCounter(message.getDeliveredMessageId());

        Intent intent = new Intent(Constants.SEND_BROADCAST);
        intent.putExtra(Constants.DELIVERED_MESSAGE_SERVER_ID, message.getDeliveredMessageId());
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(ChabokApplication.getContext());
        broadcaster.sendBroadcast(intent);

    }

    private int getNotificationIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.drawable.ic_silhouette : R.drawable.ic_launcher;
    }

    public synchronized AdpPushClient getPushClient() {
        if (adpPush == null) {
            throw new IllegalStateException("Adp Push Client not initialized");
        }
        return adpPush;
    }


    @Override
    public void onTerminate() {
        // dismiss push client on app termination
        adpPush.dismiss();
        super.onTerminate();
    }

    @Override
    public void onLocationUpdated(Location location) {
        mCurrentLocation = location;
        Log.d(TAG, "onLocationUpdated: lat: " + (location != null ? location.getLatitude() : "null"));
        updateUserStatus(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (mCurrentLocation == null) {
            mCurrentLocation = locationManger.getLastLocation();
            updateUserStatus(mCurrentLocation);
        }
    }

    @Override
    public void onSuspended() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onGeofencesRegisteredSuccessful() {

    }

    private void updateUserStatus(Location location) {
        if (location != null) {
            if (STATUS_DIGGING.equalsIgnoreCase(eventName)) {
                Utils.setUserStatus(STATUS_DIGGING, location);
                eventName = "";
            }
        }
    }

    public void initializeLocationManager() {

        locationManger = LocationManager.init(getContext());
        locationManger.addListener(this);
    }

    public LocationManager getLocationManger() {
        return locationManger;
    }

    public Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    public void setmCurrentLocation(Location mCurrentLocation) {
        this.mCurrentLocation = mCurrentLocation;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
}
