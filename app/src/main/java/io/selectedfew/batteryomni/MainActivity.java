package io.selectedfew.batteryomni;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("batteryomni");
    }

    private TextView batteryStatsTextView;
    private TextView wakelockTextView;
    private BatteryReceiver batteryReceiver;
    private static final String CHANNEL_ID = "batteryomni_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batteryStatsTextView = findViewById(R.id.batteryStatsTextView);
        wakelockTextView = findViewById(R.id.wakelockTextView);

        batteryStatsTextView.setPadding(
                batteryStatsTextView.getPaddingLeft(),
                batteryStatsTextView.getPaddingTop() + 200,
                batteryStatsTextView.getPaddingRight(),
                batteryStatsTextView.getPaddingBottom()
        );

        Button genReportButton = findViewById(R.id.genReportButton);
        genReportButton.setOnClickListener(v -> generateFullReport());

        Button ramFlushButton = findViewById(R.id.ramFlushButton);
        ramFlushButton.setOnClickListener(v -> flushRAM());

        Button cpuBoostButton = findViewById(R.id.cpuBoostButton);
        cpuBoostButton.setOnClickListener(v -> {
            // Placeholder for CPU Boost logic
        });

        batteryReceiver = new BatteryReceiver();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        createNotificationChannel();
        checkRootAccess();
        readWakelocks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }

    public native String stringFromJNI(int level, String status, int voltage, int temperature);

    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            String technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

            float percent = (level / (float) scale) * 100;

            String statusText;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    statusText = "Charging";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    statusText = "Discharging";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    statusText = "Full";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    statusText = "Not Charging";
                    break;
                default:
                    statusText = "Unknown";
            }

            String healthText;
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    healthText = "Good";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    healthText = "Overheat";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    healthText = "Dead";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    healthText = "Over Voltage";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    healthText = "Failure";
                    break;
                default:
                    healthText = "Unknown";
            }

            String plugType;
            switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_USB:
                    plugType = "USB";
                    break;
                case BatteryManager.BATTERY_PLUGGED_AC:
                    plugType = "AC";
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    plugType = "Wireless";
                    break;
                default:
                    plugType = "Not Plugged";
            }

            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

            StringBuilder info = new StringBuilder();
            info.append(stringFromJNI((int) percent, statusText, voltage, temp)).append("\n");
            info.append("Health: ").append(healthText).append("\n");
            info.append("Plug Type: ").append(plugType).append("\n");
            info.append("Technology: ").append(technology != null ? technology : "Unknown").append("\n");
            info.append("Charge Counter: ").append(chargeCounter >= 0 ? chargeCounter + " µAh" : "Unavailable");

            batteryStatsTextView.setText(info.toString());
        }
    }

    private void checkRootAccess() {
        try {
            Process su = Runtime.getRuntime().exec("su");
            BufferedReader in = new BufferedReader(new InputStreamReader(su.getInputStream()));
            su.getOutputStream().write("echo RootCheck\n".getBytes());
            su.getOutputStream().flush();
            su.getOutputStream().close();
            su.waitFor();
            batteryStatsTextView.append("\nRoot: OK 🔓");
        } catch (Exception e) {
            batteryStatsTextView.append("\nRoot: ❌ NOT available");
        }
    }

    private void readWakelocks() {
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                su.getOutputStream().write("dumpsys power | grep Wake\n".getBytes());
                su.getOutputStream().flush();
                su.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(su.getInputStream()));
                StringBuilder wakelockData = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    wakelockData.append(line).append("\n");
                }
                su.waitFor();

                runOnUiThread(() -> wakelockTextView.setText(wakelockData.toString()));
            } catch (Exception e) {
                runOnUiThread(() -> wakelockTextView.setText("Wakelocks: ❌ Error reading"));
            }
        }).start();
    }

    private void generateFullReport() {
        String batteryInfo = batteryStatsTextView.getText().toString();
        String wakelockInfo = wakelockTextView.getText().toString();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String fullReport = "Timestamp: " + timestamp + "\n\n"
                + "Battery Info:\n" + batteryInfo + "\n\n"
                + "Wakelock Info:\n" + wakelockInfo + "\n";

        try {
            File path = getExternalFilesDir(null);
            if (path != null) {
                File file = new File(path, "battery_report.txt");
                FileWriter writer = new FileWriter(file, false);
                writer.write(fullReport);
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void flushRAM() {
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                su.getOutputStream().write("sh -c 'sync; echo 3 > /proc/sys/vm/drop_caches'\n".getBytes());
                su.getOutputStream().flush();
                su.getOutputStream().close();
                su.waitFor();

                runOnUiThread(() ->
                        showNotification("WTF did you flush", "Dropped pagecache, dentries, and inodes ⚙️")
                );
            } catch (Exception e) {
                runOnUiThread(() ->
                        showNotification("Flush Failed", "Unable to flush RAM: " + e.getMessage())
                );
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Omni Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
