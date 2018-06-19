package com.example.omer.imageservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.inputmethod.InputContentInfo;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(wifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        //get the different network states
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                            transferImages(context);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void transferImages(Context context) {
        Toast.makeText(this, "sending images", Toast.LENGTH_SHORT).show();
        File Camera = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (Camera == null) {
            return;
        }
        final File[] pics = Camera.listFiles();

        // progress bar
        final int NI = 1;
        NotificationChannel NC = new NotificationChannel("default", "default", NotificationManager.IMPORTANCE_DEFAULT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default")
        .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("BackUp The Pics").setPriority(NotificationCompat.PRIORITY_DEFAULT);
        final NotificationManager notificationManager = (NotificationManager) getSystemService(context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(NC);
        builder.setProgress(100, 0,false);

        builder.setContentTitle("Picture Transfer").setContentText("Transfer in progress").setPriority(NotificationCompat.PRIORITY_LOW);
        notificationManager.notify(10, builder.build());

        int count = 0;
        if (pics != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InetAddress serverAddr = InetAddress.getByName("10.0.2.2");
                        try {
                            int bar = 0;
                            for (File pic : pics) {
                                Socket socket = new Socket(serverAddr, 8000);
                                OutputStream outputStream = socket.getOutputStream();
                                InputStream inputStream = socket.getInputStream();
                                Log.e("TCP", "YES!");
                                FileInputStream fis = new FileInputStream(pic);
                                Bitmap bm = BitmapFactory.decodeStream(fis);
                                byte[] imgbyte = getBytesFromBitmap(bm);
                                byte[] b = new byte[1];
                                outputStream.write(pic.getName().getBytes());
                                if (inputStream.read(b,0,1) == 1) {
                                    outputStream.write(imgbyte);
                                }
                                outputStream.flush();
                                bar = bar + 100 / pics.length;
                                builder.setProgress(100, bar, false);
                                notificationManager.notify(10, builder.build());
                                socket.close();
                            }
                            builder.setProgress(0,0, false);
                            builder.setContentText("Finish Transfer.");
                            notificationManager.notify(10, builder.build());
                        } catch (Exception e) {
                            Log.e("TCP", "S: Error", e);
                        }
                    } catch (Exception e) {
                        Log.e("TCP", "C: Error", e);
                    }
                }
            }).start();
        }
    }


    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

    public static byte[] extractBytes(File file) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        FileInputStream fis = new FileInputStream(file);
        try {
            int i;
            while ((i = fis.read(buffer)) != -1) {
                stream.write(buffer, 0, i);
            }

        } catch (IOException ex) {
        }
        return stream.toByteArray();

    }

}
