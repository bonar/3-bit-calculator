package jp.bonar.threebitsgame;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Gpio buttonA;
    private Gpio buttonB;
    private Gpio buttonC;

    private Gpio ledA;
    private Gpio ledB;
    private Gpio ledC;

    private AlphanumericDisplay display;

    private TextView digitsView;

    private static final String NAME_BUTTON_A = "GPIO6_IO14";
    private static final String NAME_BUTTON_B = "GPIO6_IO15";
    private static final String NAME_BUTTON_C = "GPIO2_IO07";

    private static final String NAME_LED_A = "GPIO2_IO02";
    private static final String NAME_LED_B = "GPIO2_IO00";
    private static final String NAME_LED_C = "GPIO2_IO05";

    private Map<String, Boolean> buttonStatus = new HashMap<>();
    private Map<String, Gpio> button2led = new HashMap<>();

    private int getCurrentButtonValue() {
        int value = 0;
        if (buttonStatus.get(NAME_BUTTON_A)) {
            value += 4;
        }
        if (buttonStatus.get(NAME_BUTTON_B)) {
            value += 2;
        }
        if (buttonStatus.get(NAME_BUTTON_C)) {
            value += 1;
        }
        return value;
    }

    private GpioCallback buttonCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    String buttonName = gpio.getName();
                    Boolean currentValue = buttonStatus.get(buttonName);
                    buttonStatus.put(buttonName, !currentValue);

                    // change led status
                    button2led.get(buttonName).setValue(!currentValue);

                    updateDisplay();
                }
            } catch (IOException e) {
                Log.w(TAG, "Error reading GPIO");
            }

            // Return true to keep callback active.
            return true;
        }
    };

    private void updateDisplay() {
        int currentValue = getCurrentButtonValue();
        Log.i(TAG, "current value = " + currentValue);
        try {
            display.display(String.format("%04d", currentValue));
            digitsView.setText(String.format("%d %d %d = %d",
                    buttonStatus.get(NAME_BUTTON_A) ? 1 : 0,
                    buttonStatus.get(NAME_BUTTON_B) ? 1 : 0,
                    buttonStatus.get(NAME_BUTTON_C) ? 1 : 0,
                    currentValue));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            PeripheralManagerService peripheralManager
                    = new PeripheralManagerService();
            buttonA = createButton(peripheralManager, NAME_BUTTON_A);
            buttonB = createButton(peripheralManager, NAME_BUTTON_B);
            buttonC = createButton(peripheralManager, NAME_BUTTON_C);

            ledA = createLED(peripheralManager, NAME_LED_A);
            ledB = createLED(peripheralManager, NAME_LED_B);
            ledC = createLED(peripheralManager, NAME_LED_C);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        buttonStatus.put(NAME_BUTTON_A, false);
        buttonStatus.put(NAME_BUTTON_B, false);
        buttonStatus.put(NAME_BUTTON_C, false);

        button2led.put(NAME_BUTTON_A, ledA);
        button2led.put(NAME_BUTTON_B, ledB);
        button2led.put(NAME_BUTTON_C, ledC);

        try {
            display= RainbowHat.openDisplay();
            display.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            display.display("0000");
            display.setEnabled(true);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        setupView();
    }

    private Gpio createLED(PeripheralManagerService manager, String name) throws IOException {
        Gpio pin = manager.openGpio(name);
        pin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        return pin;
    }

    private Gpio createButton(PeripheralManagerService manager, String name) throws IOException {
        // Create GPIO connection.
        Gpio pin = manager.openGpio(name);

        // Configure as an input, trigger events on every change.
        pin.setDirection(Gpio.DIRECTION_IN);
        pin.setEdgeTriggerType(Gpio.EDGE_BOTH);

        // Value is true when the pin is LOW
        pin.setActiveType(Gpio.ACTIVE_LOW);

        pin.registerGpioCallback(buttonCallback);

        return pin;
    }

    private void setupView() {
        setContentView(R.layout.activity_main);
        digitsView = findViewById(R.id.digit);
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

        // Close the device when done.
        if (null != display) {
            try {
                display.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

}
