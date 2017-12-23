package jp.bonar.threebitsgame;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Gpio buttonA;
    private Gpio buttonB;
    private Gpio buttonC;

    private Gpio ledA;
    private Gpio ledB;
    private Gpio ledC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PeripheralManagerService peripheralManager
                = new PeripheralManagerService();
        buttonA = setupButton(peripheralManager, "GPIO6_IO14");
        buttonB = setupButton(peripheralManager, "GPIO6_IO15");
        buttonC = setupButton(peripheralManager, "GPIO2_IO07");

        showDisplay();
        setupView();
    }

    private GpioCallback buttonCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                Log.i(TAG, "GPIO changed, button " + gpio.getName());
                Log.i(TAG, "GPIO changed, button " + gpio.getValue());
            } catch (IOException e) {
                Log.w(TAG, "Error reading GPIO");
            }

            // Return true to keep callback active.
            return true;
        }
    };

    private Gpio setupButton(PeripheralManagerService manager, String name) {
        try {
            // Create GPIO connection.
            Gpio pin = manager.openGpio(name);

            // Configure as an input, trigger events on every change.
            pin.setDirection(Gpio.DIRECTION_IN);
            pin.setEdgeTriggerType(Gpio.EDGE_BOTH);

            // Value is true when the pin is LOW
            pin.setActiveType(Gpio.ACTIVE_LOW);

            // pin.registerGpioCallback(buttonCallback);

            return pin;
        } catch (IOException e) {
            Log.w(TAG, "Error opening GPIO", e);
            return null;
        }
    }

    private void showDisplay() {
        // Display a string on the segment display.
        try {
            AlphanumericDisplay segment = RainbowHat.openDisplay();
            segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            segment.display("0123");
            segment.setEnabled(true);

            // Close the device when done.
            segment.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void setupView() {
        setContentView(R.layout.activity_main);
    }

    private void closeButton(Gpio button) {
        // Close the button
        if (button != null) {
            button.unregisterGpioCallback(buttonCallback);
            try {
                button.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing GPIO", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        closeButton(buttonA);
        closeButton(buttonB);
        closeButton(buttonC);
    }

}
