package io.selectedfew.batteryomni;

import androidx.appcompat.app.AppCompatActivity;
import android.content.*;
import android.os.*;
import java.io.*;

import io.selectedfew.batteryomni.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("batteryomni");
    }

    private ActivityMainBinding binding;
    private BatteryReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        batteryReceiver = new BatteryReceiver();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }

    public native String stringFromJNI(int level, String status, int voltage, int temperature);

    private String getWakelockStatus() {
        StringBuilder output = new StringBuilder();
        try {
            java.lang.Process process = Runtime.getRuntime().exec("dumpsys power");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("wake") || line.toLowerCase().contains("held")) {
                    output.append(line.trim()).append("\n");
                }
            }

            reader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            output.append("Wakelock check failed.\n");
        }

        return output.toString().isEmpty() ? "No wakelocks detected." : output.toString();
    }

    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);

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

            String info = stringFromJNI((int) percent, statusText, voltage, temp);
            String wakelocks = getWakelockStatus();

            binding.sampleText.setText(info + "\n\nWakelocks:\n" + wakelocks);
        }
    }
}
