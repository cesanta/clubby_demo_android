package com.cesanta.clubby.demo.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cesanta.cloud.ProjectService;
import com.cesanta.cloud.auth.CloudAuthenticator;
import com.cesanta.cloud.auth.CloudAuthenticator.Credentials;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "clubby";
    private static final String PREFS_NAME = "clubby";

    private Clubby clubby = null;
    private ProjectService project = null;

    private Button guiBtnConnect;
    private Button guiBtnDisconnect;
    private EditText guiTextLog;
    private LinearLayout guiLayoutCloudControls;
    private Spinner guiSpnProjects;
    private Spinner guiSpnDevices;
    private Button guiBtnPinNumber;
    private Switch guiSwitchPinState;
    private TextView guiTextErrorMsg;

    private List<String> projectNames = new ArrayList<String>();
    private List<String> projectIds = new ArrayList<String>();
    private List<String> deviceIds = new ArrayList<String>();

    private ArrayAdapter<String> projectsAdapter;
    private ArrayAdapter<String> devicesAdapter;

    private int pinNumber = 0;
    private Boolean pinState = null;
    private String selectedDeviceId = null;
    private String errorMsg = null;

    private boolean trackPinState = true;

    private final CloudAuthenticator authenticator = new CloudAuthenticator(this);

    private void loadPrefs() {
        EditText et;

        SharedPreferences sett = getSharedPreferences(PREFS_NAME, 0);

        pinNumber = sett.getInt("pinNumber", 0);
    }

    private void savePrefs() {
        SharedPreferences sett = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sett.edit();

        editor.putInt("pinNumber", pinNumber);

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

    private void initClubby(String id, String key) {
        try {
            if (clubby != null) {
                clubby.removeListener(clubbyListener);
                clubby = null;
            }

            clubby = new Clubby.Builder()
                .id(id)
                .psk(key)
                .backend("//api3.cesanta.com")
                .timeout(5)
                .build();

            clubby.addListener(clubbyListener);

            project = ProjectService.createInstance(clubby);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void guiApply() {
        final ClubbyState clubbyState = clubby.getState();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (clubbyState) {
                    case NOT_CONNECTED:
                        guiLayoutCloudControls.setVisibility(View.GONE);

                        guiBtnConnect.setEnabled(!authenticator.isDialogVisible());
                        guiBtnDisconnect.setEnabled(false);

                        projectNames.clear();
                        projectIds.clear();
                        deviceIds.clear();
                        selectedDeviceId = null;
                        pinState = null;
                        break;

                    case CONNECTING:
                    case DISCONNECTING:
                        guiLayoutCloudControls.setVisibility(View.GONE);

                        guiBtnConnect.setEnabled(false);
                        guiBtnDisconnect.setEnabled(false);
                        break;

                    case CONNECTED:
                        guiLayoutCloudControls.setVisibility(View.VISIBLE);

                        guiBtnConnect.setEnabled(false);
                        guiBtnDisconnect.setEnabled(true);
                        break;
                }

                projectsAdapter.notifyDataSetChanged();
                devicesAdapter.notifyDataSetChanged();

                // Enable or disable projects/devices spinners
                guiSpnProjects.setEnabled(projectIds.size() != 0);
                guiSpnDevices.setEnabled(deviceIds.size() != 0);

                // Apply "Pin number" button and the switcher
                if (selectedDeviceId != null) {
                    // Some device is selected
                    guiBtnPinNumber.setEnabled(true);
                    if (pinState != null) {
                        guiSwitchPinState.setEnabled(true);
                        trackPinState = false;
                        guiSwitchPinState.setChecked(pinState);
                        trackPinState = true;
                    } else {
                        guiSwitchPinState.setEnabled(false);
                    }
                } else {
                    // No device selected: just disable pin controls
                    guiBtnPinNumber.setEnabled(false);
                    guiSwitchPinState.setEnabled(false);
                }

                // Set appropriate pin number on the button
                guiBtnPinNumber.setText(
                        getResources().getString(R.string.pin_number) + ": " + pinNumber
                        );

                // Show or hide error message
                if (errorMsg != null) {
                    guiTextErrorMsg.setText(errorMsg);
                    guiTextErrorMsg.setVisibility(View.VISIBLE);
                } else {
                    guiTextErrorMsg.setText("");
                    guiTextErrorMsg.setVisibility(View.GONE);
                }
            }
        });
    }

    private void cloudAuth() {
        authenticator.auth(new CloudAuthenticator.Listener() {
            @Override
            public void onAuthenticated(Credentials cred) {
                initClubby(cred.id, cred.key);
                connect();
            }

            @Override
            public void onCancelled() {
                guiApply();

                // When auth is cancelled, close the activity immediately
                finish();
            }
        });
        guiApply();
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
     * Arguments for our custom command "/v1/GPIO.Read"
     */
    static class GpioReadArgs {
        public int pin;

        GpioReadArgs(int pin) {
            this.pin = pin;
        }
    }

    /**
     * Arguments for our custom command "/v1/GPIO.Write"
     */
    static class GpioWriteArgs {
        public int pin;
        public boolean state;

        GpioWriteArgs(int pin, boolean state) {
            this.pin = pin;
            this.state = state;
        }
    }

    /**
     * Response of our custom commands "/v1/GPIO.Read" and "/v1/GPIO.Write",
     * will be deserialized from JSON
     */
    static class GpioResp {
        public int pin;
        public int state;
    }

    /**
     * Handler of responses on both "/v1/GPIO.Read" and "/v1/GPIO.Write"
     */
    private class OnGpioResp extends CmdAdapter<GpioResp> {
        @Override
        public void onResponse(final GpioResp resp) {
            if (resp != null) {
                switch (resp.state) {
                    case 0:
                        pinState = false;
                        break;
                    case 1:
                        pinState = true;
                        break;
                    default:
                        pinState = null;
                        errorMsg = getResources().getString(R.string.invalid_pin);
                        break;
                }
                guiApply();
            }
        }

        @Override
        public void onError(int status, String status_msg) {
            println("Got error: " + status_msg);
        }
    }

    /**
     * Performs asynchronous request of the selected GPIO pin state; response
     * is handled by the `OnGpioResp`.
     */
    private void getGpioState() {

        pinState = null;
        errorMsg = null;
        guiApply();

        println("Sending GPIO.Read...");
        clubby.call(
                selectedDeviceId,
                "/v1/GPIO.Read",
                new GpioReadArgs(pinNumber),
                new OnGpioResp(),
                GpioResp.class
            );
    }

    /**
     * Performs asynchronous request to set new GPIO pin state; response is
     * handled by the `OnGpioResp`.
     */
    private void setGpioState(boolean newPinState) {

        pinState = null;
        errorMsg = null;
        guiApply();

        println("Sending GPIO.Write...");
        clubby.call(
                selectedDeviceId,
                "/v1/GPIO.Write",
                new GpioWriteArgs(pinNumber, newPinState),
                new OnGpioResp(),
                GpioResp.class
                );
    }


    /**
     * Fetches list of the projects available to the user, asynchronously. When
     * response is received, GUI is updated accordingly.
     */
    private void getProjectsList() {
        println("Retrieving projects list...");
        project.list(
                new ProjectService.ListArgs(),
                new CmdAdapter<ProjectService.ListResponse>() {
                    @Override
                    public void onResponse(ProjectService.ListResponse resp) {
                        projectNames.clear();
                        projectIds.clear();

                        for (ProjectService.ListResponseItem item : resp) {
                            projectNames.add(item.name);
                            projectIds.add(item.id);
                        }

                        guiApply();
                    }
                }
                );
    }

    /**
     * Fetches list of the devices in the given project, asynchronously. When
     * response is received, GUI is updated accordingly.
     */
    private void getDevicesList(String projectId) {
        println("Retrieving devices list...");
        project.listDevices(
                new ProjectService.ListDevicesArgs()
                .projectid(projectId),
                new CmdAdapter<ProjectService.ListDevicesResponse>() {
                    @Override
                    public void onResponse(ProjectService.ListDevicesResponse resp) {
                        deviceIds.clear();

                        for (String item : resp) {
                            deviceIds.add(item);
                        }

                        guiApply();
                    }
                }
                );
    }

    private void onProjectChanged() {

        // Clear current devices list
        deviceIds.clear();

        // Retrieve a new one
        long pnum = guiSpnProjects.getSelectedItemId();
        if (pnum != AdapterView.INVALID_ROW_ID) {
            getDevicesList(projectIds.get((int)pnum));
        }

        guiApply();
    }

    private void onDeviceChanged() {
        selectedDeviceId = null;

        long dnum = guiSpnDevices.getSelectedItemId();
        if (dnum != AdapterView.INVALID_ROW_ID) {
            // Some device is indeed selected: remember its id, and retrieve
            // the state of the GPIO pin on the newly selected device
            selectedDeviceId = deviceIds.get((int)dnum);
            getGpioState();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        guiBtnConnect = (Button)findViewById(R.id.btn_connect);
        guiBtnDisconnect = (Button)findViewById(R.id.btn_disconnect);
        guiTextLog = (EditText)findViewById(R.id.log);
        guiLayoutCloudControls = (LinearLayout)findViewById(R.id.cloud_controls_form);
        guiSpnProjects = (Spinner)findViewById(R.id.spn_projects);
        guiSpnDevices = (Spinner)findViewById(R.id.spn_devices);
        guiBtnPinNumber = (Button)findViewById(R.id.btn_pin_number);
        guiSwitchPinState = (Switch)findViewById(R.id.btn_pin_toggle);
        guiTextErrorMsg = (TextView)findViewById(R.id.error_msg);

        guiTextLog.setVerticalScrollBarEnabled(true);
        guiTextLog.setKeyListener(null);

        loadPrefs();

        initClubby("", "");

        //-- init projects spinner {{{
        projectsAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                R.layout.spinner,
                projectNames
                );

        guiSpnProjects.setAdapter(projectsAdapter);

        guiSpnProjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                    View itemSelected, int selectedItemPosition, long selectedId) {
                onProjectChanged();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // }}}

        //-- init devices spinner {{{
        devicesAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                R.layout.spinner,
                deviceIds
                );

        guiSpnDevices.setAdapter(devicesAdapter);

        guiSpnDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                    View itemSelected, int selectedItemPosition, long selectedId) {
                onDeviceChanged();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // }}}

        guiBtnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                cloudAuth();
            }
        });

        guiBtnDisconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                authenticator.clearAuthData();
                disconnect();
            }
        });

        guiBtnPinNumber.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                NumberPickerDialog npd = new NumberPickerDialog(
                        MainActivity.this,
                        new NumberPickerDialog.ValueChangeListener(){
                            @Override
                            public void valueChanged(int value){
                                pinNumber = value;
                                getGpioState();
                            }
                        },
                        pinNumber,
                        getResources().getString(R.string.pin_number),
                        null
                        );

                npd.setMin(0);
                npd.setMax(100);

                npd.show();

            }
        });

        guiSwitchPinState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (trackPinState) {
                    setGpioState(isChecked);
                }
            }
        });

        cloudAuth();
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
            getProjectsList();
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
            guiApply();
        }
    };
}

