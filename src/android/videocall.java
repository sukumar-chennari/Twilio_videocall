package cordova.plugin.videocall.videocall;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

import src.cordova.plugin.videocall.Analytics;
import src.cordova.plugin.videocall.PermissionUtil.PermissionUtil;
import src.cordova.plugin.videocall.RoomActivity.RoomActivity;

/**
 * This class echoes a string called from JavaScript.
 */
public class videocall extends CordovaPlugin {

    private String roomName, token, id;
    public static String identity;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            this.roomName = args.getString(0);
            this.token = args.getString(1);
            this.identity = args.getString(2);
            this.id = args.getString(3);
            this.executeActivity();
//            coolMethod();
            return true;
        } else if (action.equals("retriveData")) {
            String dataReceived = args.getString(0);
            return true;
        }
        return false;
    }

    private void executeActivity() {
//        getNetworkStatus();

        if (isPermissionsGranted()) {
            Intent intent = new Intent(this.cordova.getActivity(), RoomActivity.class);
            intent.putExtra("room", roomName);
            intent.putExtra("identity", identity);
            intent.putExtra("token", token);
            intent.putExtra("id", "3");
            Log.e("logging->", identity);
            this.cordova.getActivity().startActivity(intent);
        }

    }

    private void showNotification(String message){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(cordova.getContext(), "CHANNEL_ID");

        mBuilder.setContentTitle("test");
        mBuilder.setContentText("message");
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) cordova.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "bella_notification";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel chat bot bella",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }
        notificationManager.notify(0, mBuilder.build());
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean getNetworkStatus() {

        ConnectivityManager cm = (ConnectivityManager) cordova.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        int downloadSpeed = nc.getLinkDownstreamBandwidthKbps();
        int uploadSpeed = nc.getLinkUpstreamBandwidthKbps();
        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifiManager = (WifiManager) cordova.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int linkSpeed = wifiManager.getConnectionInfo().getLinkSpeed();
            Toast.makeText(cordova.getContext(), "linkspeed wifi in MBPS " + linkSpeed, Toast.LENGTH_LONG).show();
            if (linkSpeed < 3) {
                Map<String, String> event = new HashMap<>();
                event.put("message", "Low bandwidth for the user " + roomName + "@" + identity);
                Analytics.trackEvent("Bandwidth", event);
            }
        } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            Toast.makeText(cordova.getContext(), "linkspeed mobile in KBPS " + downloadSpeed, Toast.LENGTH_LONG).show();
            if (downloadSpeed < 3000) {
                Map<String, String> event = new HashMap<>();
                event.put("message", "Low bandwidth for the user " + roomName + "@" + identity);
                Analytics.trackEvent("Bandwidth", event);
            }

        }
        return true;
    }

    private void coolMethod() {
//        if (isPermissionsGranted()) {
        Intent intent = new Intent(this.cordova.getActivity(), RoomActivity.class);
        intent.putExtra("room", roomName);
        intent.putExtra("identity", identity);
        intent.putExtra("token", token);
        intent.putExtra("id", "3");
        this.cordova.getActivity().startActivity(intent);
//        }

    }

    private boolean isPermissionsGranted() {
        boolean flag = false;
        final PermissionUtil permissionUtil = new PermissionUtil(this.cordova.getContext());
        boolean isCameraEnabled = permissionUtil.isPermissionGranted(Manifest.permission.CAMERA);
        boolean isMicEnabled = permissionUtil.isPermissionGranted(Manifest.permission.RECORD_AUDIO);
        if (isCameraEnabled && isMicEnabled)
            flag = true;
        else
            requestPermissions();
        return flag;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cordova.requestPermissions(this, 111, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO});
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (requestCode == 111) {
            boolean isCameraEnabled = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean isMicEnabled = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (isCameraEnabled && isMicEnabled) {
                Intent intent = new Intent(this.cordova.getActivity(), RoomActivity.class);
                intent.putExtra("room", roomName);
                intent.putExtra("identity", identity);
                intent.putExtra("token", token);
                intent.putExtra("id", "3");
                this.cordova.getActivity().startActivity(intent);
            } else {
                requestPermissions();
            }
        }
    }

}
