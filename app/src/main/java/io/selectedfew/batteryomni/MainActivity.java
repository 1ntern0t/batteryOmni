package io.selectedfew.batteryomni;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
//test for request
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

        checkRootAccess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }

    // Native method
    public native String stringFromJNI(int level, String status, int voltage, int temperature);

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

            String finalText = stringFromJNI((int) percent, statusText, voltage, temp);
            binding.sampleText.setText(finalText);
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
            binding.sampleText.append("\nRoot: OK");
        } catch (Exception e) {
            binding.sampleText.append("\nRoot: ❌ NOT available");
        }
    }
}
