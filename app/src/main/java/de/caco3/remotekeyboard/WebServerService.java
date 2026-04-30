package de.caco3.remotekeyboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

/**
 * Foreground service that hosts the HTTPS web keyboard server.
 * <p>
 * Running the server in its own {@link Service} — rather than inside
 * {@link RemoteKeyboardService} — means the server stays up even when
 * Remote Keyboard is not the currently-selected input method. That way the
 * browser can always reach the phone and surface a helpful explanation
 * ("wrong keyboard selected") instead of a raw "connection failed" error.
 */
public class WebServerService extends Service {

    public static final String TAG = "WebServerService";
    private static final String CHANNEL_ID = "remotekeyboard_server";
    private static final int NOTIFICATION_ID = 42;

    private WebKeyboardServer webServer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForegroundWithNotification();

        webServer = new WebKeyboardServer();
        try {
            webServer.start(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start web server: " + e.getMessage(), e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* Keep the service alive — the system may restart it if it is killed. */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private Notification buildNotification() {
        String title = getString(R.string.notification_title);
        String content = getString(R.string.notification_waiting, getWifiIp());

        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_stat_service)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
    }

    @SuppressWarnings("deprecation")
    private String getWifiIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(WIFI_SERVICE);
        if (wifiManager == null) return "?";
        WifiInfo info = wifiManager.getConnectionInfo();
        int addr = info.getIpAddress();
        if (addr == 0) return "?";
        return (addr & 0xFF) + "." + ((addr >> 8) & 0xFF) + "."
                + ((addr >> 16) & 0xFF) + "." + ((addr >> 24) & 0xFF);
    }
}
