package com.cesanta.clubby.demo.android;

import java.io.IOException;

import com.cesanta.cloud.DispatcherService;
import com.cesanta.cloud.DispatcherService.RouteStatsResponseItem;
import com.cesanta.cloud.MetricsService;
import com.cesanta.cloud.MetricsService.PublishArgs;
import com.cesanta.cloud.MetricsService.PublishArgsVar;
import com.cesanta.cloud.MetricsService.PublishArgsVarFirst;
import com.cesanta.clubby.lib.Clubby;
import com.cesanta.clubby.lib.ClubbyAdapter;
import com.cesanta.clubby.lib.ClubbyListener;
import com.cesanta.clubby.lib.ClubbyState;
import com.cesanta.clubby.lib.CmdAdapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "clubby";
    private static final String PREFS_NAME = "clubby";

    private Clubby clubby = null;
    private MetricsService metrics = null;
    private DispatcherService dispatcher = null;

    private EditText guiEditDeviceId;
    private EditText guiEditDevicePsk;
    private Button guiBtnConnect;
    private Button guiBtnDisconnect;
    private Button guiBtnSendHello;
    private Button guiBtnSendRouteStats;
    private Button guiBtnSendMetricsPub;
    private Button guiBtnLcdAddLine;
    private Button guiBtnLedSet;
    private Button guiBtnLedGet;
    private EditText guiTextLog;
    private TextView guiTextLedGet;
    private LinearLayout guiLayoutIdPskForm;
    private LinearLayout guiLayoutCloudControls;

    private void loadPrefs() {
        EditText et;

        SharedPreferences sett = getSharedPreferences(PREFS_NAME, 0);

        et = (EditText)findViewById(R.id.edit_device_id);
        et.setText(sett.getString("deviceId", "//api.cesanta.com/d/your_device_id"));

        et = (EditText)findViewById(R.id.edit_device_psk);
        et.setText(sett.getString("devicePsk", "your_psk"));

        et = (EditText)findViewById(R.id.edit_lcd_add_line);
        et.setText(sett.getString("lcdAddLine", "Hi from Android!"));
    }

    private void savePrefs() {
        SharedPreferences sett = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sett.edit();

        editor.putString("deviceId", getEnteredText(R.id.edit_device_id));
        editor.putString("devicePsk", getEnteredText(R.id.edit_device_psk));
        editor.putString("lcdAddLine", getEnteredText(R.id.edit_lcd_add_line));

        editor.commit();
    }

    private String getEnteredText(int editTextId) {
        EditText et;
        et = (EditText)findViewById(editTextId);
        return et.getText().toString();
    }

    private void println(final String str) {
        Log.v(TAG, str);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                guiTextLog.append("* ");
                guiTextLog.append(str);
                guiTextLog.append("\n");
                guiTextLog.scrollTo(0, Integer.MAX_VALUE);
            }
        });
    }

    private void initClubby() {
        try {
            if (clubby != null) {
                clubby.removeListener(clubbyListener);
                clubby = null;
            }

            clubby = new Clubby.Builder()
                .device(
                        getEnteredText(R.id.edit_device_id),
                        getEnteredText(R.id.edit_device_psk)
                       )
                .build();

            clubby.addListener(clubbyListener);

            metrics = MetricsService.createInstance(clubby);
            dispatcher = DispatcherService.createInstance(clubby);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void guiApply(final ClubbyState clubbyState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (clubbyState) {
                    case NOT_CONNECTED:
                        guiEditDeviceId.setEnabled(true);
                        guiEditDevicePsk.setEnabled(true);
                        guiLayoutIdPskForm.setVisibility(View.VISIBLE);
                        guiLayoutCloudControls.setVisibility(View.GONE);

                        guiBtnConnect.setEnabled(true);
                        guiBtnDisconnect.setEnabled(false);
                        guiBtnSendHello.setEnabled(false);
                        guiBtnSendRouteStats.setEnabled(false);
                        guiBtnSendMetricsPub.setEnabled(false);
                        guiBtnLcdAddLine.setEnabled(false);
                        guiBtnLedSet.setEnabled(false);
                        guiBtnLedGet.setEnabled(false);
                        break;

                    case CONNECTING:
                    case DISCONNECTING:
                        guiEditDeviceId.setEnabled(false);
                        guiEditDevicePsk.setEnabled(false);
                        guiLayoutIdPskForm.setVisibility(View.VISIBLE);
                        guiLayoutCloudControls.setVisibility(View.GONE);

                        guiBtnConnect.setEnabled(false);
                        guiBtnDisconnect.setEnabled(false);
                        guiBtnSendHello.setEnabled(false);
                        guiBtnSendRouteStats.setEnabled(false);
                        guiBtnSendMetricsPub.setEnabled(false);
                        guiBtnLcdAddLine.setEnabled(false);
                        guiBtnLedSet.setEnabled(false);
                        guiBtnLedGet.setEnabled(false);
                        break;

                    case CONNECTED:
                        guiEditDeviceId.setEnabled(false);
                        guiEditDevicePsk.setEnabled(false);
                        guiLayoutIdPskForm.setVisibility(View.GONE);
                        guiLayoutCloudControls.setVisibility(View.VISIBLE);

                        guiBtnConnect.setEnabled(false);
                        guiBtnDisconnect.setEnabled(true);
                        guiBtnSendHello.setEnabled(true);
                        guiBtnSendRouteStats.setEnabled(true);
                        guiBtnSendMetricsPub.setEnabled(true);
                        guiBtnLcdAddLine.setEnabled(true);
                        guiBtnLedSet.setEnabled(true);
                        guiBtnLedGet.setEnabled(true);
                        break;
                }
            }
        });
    }

    private void connect() {
        println("Connecting..");
        clubby.connect();
    }

    private void disconnect() {
        println("Disconnecting..");
        clubby.disconnect();
    }

    /**
     * Arguments for "/v1/LCD.AddLine", will be serialized into JSON
     */
    static class DemoAddLineArgs {
        public String text;

        DemoAddLineArgs(String text) {
            this.text = text;
        }
    }

    private void sendLcdAddLine() {
        println("Sending LCD.AddLine to demo...");

        clubby.call(
                "//api.cesanta.com/d/demo",
                "/v1/LCD.AddLine",
                new DemoAddLineArgs(
                    getEnteredText(R.id.edit_lcd_add_line)
                    ),
                // We don't care about the response here, so, we use plain
                // `Object` as the response type
                new CmdAdapter<Object>() {
                    @Override
                    public void onResponse(Object resp) {
                        println("Got positive response");

                        if (resp != null) {
                            println("Response data: " + resp);
                        }
                    }

                    @Override
                    public void onError(int status, String status_msg) {
                        println("Got error: " + status_msg);
                    }
                },
                Object.class
        );
    }

    /**
     * Arguments for "/v1/LED.Set", will be serialized into JSON
     */
    static class DemoLedSetArgs {
        public double value;

        DemoLedSetArgs(double value) {
            this.value = value;
        }
    }

    private void sendLedSet() {
        println("Sending LED.Set to demo...");

        try {
            double value = Double.parseDouble(getEnteredText(R.id.edit_led_set));

            clubby.call(
                    "//api.cesanta.com/d/demo",
                    "/v1/LED.Set",
                    new DemoLedSetArgs(value),
                    // We don't care about the response here, so, we use plain
                    // `Object` as the response type
                    new CmdAdapter<Object>() {
                        @Override
                        public void onResponse(Object resp) {
                            println("Got positive response");

                            if (resp != null) {
                                println("Response data: " + resp);
                            }
                        }

                        @Override
                        public void onError(int status, String status_msg) {
                            println("Got error: " + status_msg);
                        }
                    },
                    Object.class
                );
        } catch (NumberFormatException e) {
            println("Error: " + e.toString());
        }

    }

    /**
     * Response of "/v1/LED.Get", will be deserialized from JSON
     */
    static class DemoLedGetResp {
        private double value;

        public double getValue() {
            return value;
        }
    }

    private void sendLedGet() {
        println("Sending LED.Get to demo...");

        guiTextLedGet.setText("...");

        clubby.call(
                "//api.cesanta.com/d/demo",
                "/v1/LED.Get",
                // The method "/v1/LED.Get" doesn't need any arguments, so,
                // we can just pass `null` here
                null,
                // We expect response to match `DemoLedGetResp`
                new CmdAdapter<DemoLedGetResp>() {
                    @Override
                    public void onResponse(final DemoLedGetResp resp) {
                        if (resp != null) {
                            println("Got positive response; value: "
                                    + String.valueOf(resp.getValue()));

                            // Update TextView on the Activity
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    guiTextLedGet.setText(
                                            String.valueOf(resp.getValue())
                                            );
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(int status, String status_msg) {
                        println("Got error: " + status_msg);
                    }
                },
                DemoLedGetResp.class
            );
    }

    private void sendMetricsPublish() {
        println("Sending Metrics.Publish...");
        metrics.publish(
                new PublishArgs()
                .var(
                    new PublishArgsVar(
                        new PublishArgsVarFirst("temperature")
                        .add("sensor", "1")
                        .add("someAnotherLabel", "hey"),
                        123
                        )
                    ),
                new MetricsService.PublishAdapter() {
                    @Override
                    public void onResponse(MetricsService.PublishResponse response) {
                        println("Got response on Metrics.Publish: OK");
                    }

                    @Override
                    public void onError(int status, String status_msg) {
                        println("Error: Status: " + status + ", msg: " + status_msg);
                    }
                }
        );
    }

    private void sendDispatcherHello() {
        println("Sending Dispatcher.Hello...");
        dispatcher.hello(
                new DispatcherService.HelloArgs(),
                new DispatcherService.HelloAdapter() {
                    @Override
                    public void onResponse(DispatcherService.HelloResponse response) {
                        println("Got response on Dispatcher.Hello: OK");
                    }

                    @Override
                    public void onError(int status, String status_msg) {
                        println("Error: Status: " + status + ", msg: " + status_msg);
                    }
                }
                );

    }

    private void sendDispatcherRouteStats() {
        println("Sending Dispatcher.RouteStats...");
        dispatcher.routeStats(
                new DispatcherService.RouteStatsArgs(
                    getEnteredText(R.id.edit_device_id)
                    ),
                new DispatcherService.RouteStatsAdapter() {
                    @Override
                    public void onResponse(
                            DispatcherService.RouteStatsResponse response
                            ) {
                        println("Got response on Dispatcher.RouteStats: OK");

                        RouteStatsResponseItem item
                            = response.get(getEnteredText(R.id.edit_device_id));

                        if (item != null){
                            println("numSent=" + item.numSent);
                        } else {
                            println("no data");
                        }
                    }

                    @Override
                    public void onError(int status, String status_msg) {
                        println("Error: Status: " + status + ", msg: " + status_msg);
                    }
                }
        );
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        guiEditDeviceId = (EditText)findViewById(R.id.edit_device_id);
        guiEditDevicePsk = (EditText)findViewById(R.id.edit_device_psk);
        guiBtnConnect = (Button)findViewById(R.id.btn_connect);
        guiBtnDisconnect = (Button)findViewById(R.id.btn_disconnect);
        guiBtnSendHello = (Button)findViewById(R.id.btn_send_hello);
        guiBtnSendRouteStats = (Button)findViewById(R.id.btn_send_route_stats);
        guiBtnSendMetricsPub = (Button)findViewById(R.id.btn_send_metrics_pub);
        guiBtnLcdAddLine = (Button)findViewById(R.id.btn_lcd_add_line);
        guiBtnLedSet = (Button)findViewById(R.id.btn_led_set);
        guiBtnLedGet = (Button)findViewById(R.id.btn_led_get);
        guiTextLog = (EditText)findViewById(R.id.log);
        guiTextLedGet = (TextView)findViewById(R.id.text_led_get);
        guiLayoutIdPskForm = (LinearLayout)findViewById(R.id.id_psk_form);
        guiLayoutCloudControls = (LinearLayout)findViewById(R.id.cloud_controls_form);

        guiTextLog.setVerticalScrollBarEnabled(true);
        guiTextLog.setKeyListener(null);

        loadPrefs();

        initClubby();

        guiBtnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                initClubby();
                connect();
            }
        });

        guiBtnDisconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        guiBtnSendHello.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDispatcherHello();
            }
        });

        guiBtnSendRouteStats.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDispatcherRouteStats();
            }
        });

        guiBtnSendMetricsPub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMetricsPublish();
            }
        });

        guiBtnLcdAddLine.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLcdAddLine();
            }
        });

        guiBtnLedSet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLedSet();
            }
        });

        guiBtnLedGet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLedGet();
            }
        });

        guiApply(clubby.getState());
    }

    @Override public void onDestroy() {
        disconnect();
        savePrefs();

        super.onDestroy();
    }

    @Override public void onPause() {
        savePrefs();

        super.onPause();
    }

    private ClubbyListener clubbyListener = new ClubbyAdapter() {
        @Override
        public void onConnected(Clubby clubby) {
            println("Connected to server");
        }

        @Override
        public void onDataSending(Clubby clubby, String text) {
            println("Sending text data: " + text);
        }

        @Override
        public void onDataReceived(Clubby clubby, String text) {
            println("Received text data: " + text);
        }

        @Override
        public void onDisconnected(Clubby clubby) {
            println("Disconnected from server");
            clubby = null;
        }

        @Override
        public void onStateChanged(Clubby clubby, ClubbyState newState) {
            guiApply(newState);
        }
    };
}

