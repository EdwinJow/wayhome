package info.truthindata.weatherandtraffic.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;

import info.truthindata.weatherandtraffic.R;

/**
 * Created by Ed on 7/2/2017.
 */

public class NotificationHelper {
    private FragmentActivity currentActivity;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private Context mContext;
    private final int notificationID = 1001;

    public NotificationHelper(FragmentActivity activity, Context context){
        currentActivity = activity;
        mBuilder = new NotificationCompat.Builder(currentActivity);
        mContext = context;

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void sendNotification(@NonNull String title, @NonNull String text) {
        mBuilder.setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentTitle(title)
                .setContentText(text);

        mNotificationManager.notify(notificationID, mBuilder.build());
    }
}
