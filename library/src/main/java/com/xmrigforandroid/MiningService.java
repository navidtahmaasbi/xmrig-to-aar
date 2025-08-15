package com.xmrigforandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.xmrigforandroid.data.serialization.XMRigFork;
import com.xmrigforandroid.utils.ProcessExitDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiningService extends Service {

    private static final String LOG_TAG = "miningservice";
    private static final String NOTIFICATION_CHANNEL_ID = "com.xmrigforandroid.service";
    private static final String NOTIFICATION_CHANNEL_NAME = "XMRig Service";
    private static final int NOTIFICATION_ID = 1001;
    private Notification.Builder notificationbuilder;
    private Process process;
    private OutputReaderThread outputHandler;
    private boolean managedByTraffmonetizer = false; // Flag to track management state

    private final String ansiRegex = "\\e\\[[\\d;]*[^\\d;]";
    private final Pattern ansiRegexPattern = Pattern.compile(ansiRegex);

    // Callbacks
    private final Runnable onMinerStart;
    private final Runnable onMinerStop;
    private final Consumer<String> onStdout;

    public MiningService() {
        this.onMinerStart = () -> Log.d(LOG_TAG, "Miner started (default constructor)");
        this.onMinerStop = () -> Log.d(LOG_TAG, "Miner stopped (default constructor)");
        this.onStdout = (str) -> Log.d(LOG_TAG, "Stdout: " + str);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // No getIntent() here; rely on onStartCommand for intent
        if (!managedByTraffmonetizer) {
            // Fallback: Create a minimal valid notification if not managed
            createNotificationChannel();
            Notification notification = buildNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
                Log.d(LOG_TAG, "Service created and started as foreground at " + System.currentTimeMillis());
            } else {
                Log.w(LOG_TAG, "Failed to create notification, stopping service at " + System.currentTimeMillis());
                stopSelf();
            }
        } else {
            Log.d(LOG_TAG, "Managed by Traffmonetizer, skipping startForeground at " + System.currentTimeMillis());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            managedByTraffmonetizer = intent.getBooleanExtra("managed_by_traffmonetizer", false);
            Log.d(LOG_TAG, "Service started with managedByTraffmonetizer: " + managedByTraffmonetizer + " at " + System.currentTimeMillis());
            // If already created and managed, no action needed here unless state changes
        }
        return START_STICKY; // Restart if killed
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            } else {
                Log.e(LOG_TAG, "NotificationManager is null at " + System.currentTimeMillis());
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MiningService.class); // Placeholder intent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Mining Service")
                    .setContentText("Mining in progress...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Fallback for API 16-25
            return new Notification.Builder(this)
                    .setContentTitle("Mining Service")
                    .setContentText("Mining in progress...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        }
        return null; // Pre-Jelly Bean (API < 16) is not supported for foreground services
    }

    public class MiningServiceBinder extends Binder {
        public MiningService getService() {
            return MiningService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final IMiningService.Stub binder = new IMiningService.Stub() {
        @Override
        public void startMiner(String configPath, String xmrigFork) {
            startMining(configPath, xmrigFork);
        }

        @Override
        public void stopMiner() {
            stopMining();
        }
    };

    @Override
    public void onDestroy() {
        stopMining();
        super.onDestroy();
    }

    public void stopMining() {
        if (outputHandler != null) {
            outputHandler.interrupt();
            outputHandler = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
            Log.i(LOG_TAG, "stopped");
        }
    }

    public void startMining(String configPath, String xmrigFork) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "XMRigForAndroid::MinerWakeLock");
        wakeLock.acquire();

        Log.i(LOG_TAG, "starting...");
        if (process != null) {
            process.destroy();
        }

        String xmrigBin = xmrigFork.equals(XMRigFork.MONEROOCEAN.toString()) ? "libxmrig-mo.so" : "libxmrig.so";
        Log.d(LOG_TAG, "libxmrig: " + getApplicationInfo().nativeLibraryDir + "/" + xmrigBin);

        try {
            String[] args = {
                    "./" + getApplicationInfo().nativeLibraryDir + "/" + xmrigBin,
                    "-c", configPath,
                    "--http-host=127.0.0.1",
                    "--http-port=50080",
                    "--http-access-token=XMRigForAndroid",
                    "--http-no-restricted"
            };
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);

            process = pb.start();
            Log.d(LOG_TAG, "Process started successfully"); // Replace PID log

            ProcessExitDetector processExitDetector = new ProcessExitDetector(process);
            processExitDetector.addProcessListener(process -> onMinerStop.run());
            processExitDetector.start();

            outputHandler = new OutputReaderThread(process.getInputStream(), process.getErrorStream());
            outputHandler.start();

            onMinerStart.run();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception starting process: ", e);
            process = null;
            wakeLock.release();
        }
    }

    private class OutputReaderThread extends Thread {
        private InputStream inputStream;
        private InputStream errorStream;
        private BufferedReader reader;
        private BufferedReader errorReader;

        OutputReaderThread(InputStream inputStream, InputStream errorStream) {
            this.inputStream = inputStream;
            this.errorStream = errorStream;
        }

        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                errorReader = new BufferedReader(new InputStreamReader(errorStream));
                String line;

                // Read stdout
                while ((line = reader.readLine()) != null) {
                    onStdout.accept(line);
                    Log.d(LOG_TAG, "Stdout: " + line);
                    if (currentThread().isInterrupted()) return;
                }

                // Read stderr
                while ((line = errorReader.readLine()) != null) {
                    Log.e(LOG_TAG, "Stderr: " + line);
                    if (currentThread().isInterrupted()) return;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "OutputReaderThread error: ", e);
                onMinerStop.run();
            }
        }
    }

    // Interface for Java compatibility with Kotlin lambdas
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }
}