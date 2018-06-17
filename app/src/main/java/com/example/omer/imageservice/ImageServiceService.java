package com.example.omer.imageservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ImageServiceService extends Service {

    private BroadcastReceiver reciver;
    private final IntentFilter theFilter = new IntentFilter();

    public ImageServiceService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.theFilter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        this.theFilter.addAction("android.net.wifi.STATE_CHANGE");
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startID) {
        Toast.makeText(this, "Service starting...", Toast.LENGTH_SHORT).show();
        this.reciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(wifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        //get the different network states
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                            transferImages();
                        }
                    }
                }
            }
        };

        // Registers the receiver so that your service will listen for broadcast
        this.registerReceiver(this.reciver, theFilter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service ending...", Toast.LENGTH_SHORT).show();
    }

    private void transferImages() {
        Toast.makeText(this, "sending images", Toast.LENGTH_SHORT).show();
        File Camera = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (Camera == null) {
            return;
        }
        File[] pics = Camera.listFiles();

        // progress bar
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        builder.setContentTitle("Picture Transfer").setContentText("Transfer in progress").setPriority(NotificationCompat.PRIORITY_LOW);


        int count = 0;
        if (pics != null) {
            try {
                InetAddress serverAddr = InetAddress.getByName("10.0.2.2");
                Socket socket = new Socket(serverAddr, 8000);
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    for (File pic : pics) {
                        Log.e("TCP", "YES!");
                        FileInputStream fis = new FileInputStream(pic);
                        Bitmap bm = BitmapFactory.decodeStream(fis);
                        byte[] imgbyte = getBytesFromBitmap(bm);
                        outputStream.write(imgbyte);
                        outputStream.flush();
                    }
                } catch (Exception e) {
                    Log.e("TCP", "S: Error", e);
                } finally {
                    socket.close();
                }
            } catch (Exception e) {
                Log.e("TCP", "C: Error", e);
            }
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

}
