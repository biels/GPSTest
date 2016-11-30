package biel.com.gpstest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.util.Log;

/**
 * Created by Biel on 30/11/2016.
 */

public class ProximityIntentReceiver extends BroadcastReceiver {
    private static final String TAG = "ProximityIntentReceiver";
    private static final int NOTIFICATION_ID = 800;
    {
        Log.i("ProximityIntentReceiver", "Instantiated");
    }
    static {
        Log.i("ProximityIntentReceiver", "Loaded");
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String key = LocationManager.KEY_PROXIMITY_ENTERING;
        Boolean isEntering = intent.getBooleanExtra(key, false);
        if(isEntering){
            Log.i(TAG, "Entering");
        }else {
            Log.i(TAG, "Exiting");
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, null, 0);
        Notification notification = createNotification();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    private Notification createNotification() {
        Notification notification = new Notification();

        notification.icon = R.drawable.ic_stat_name;
        notification.when = System.currentTimeMillis();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;

        notification.ledARGB = Color.YELLOW;
        notification.ledOnMS = 300;
        notification.ledOffMS = 1500;

        return notification;
    }
}
