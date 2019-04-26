package com.pkmnapps.mnplymoney;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String SERVICE_ID = "com.pkmnapps.mnplymoney_service_id";
    private static final int iStartScreen = 0;
    private static final int iHostScreen = 1;
    private static final int iJoinScreen = 2;
    private static final int iGameScreen = 3;
    private static final int iLoaderScreen = 4;

    private String nickname;
    private TextView idTextView, discoverStatus, advertiseStatus, gameMoneyTextView, gamePlayersTextView;
    private Spinner paySpinner;

    private View startScreen, hostScreen, joinScreen, gameScreen, loaderScreen;

    private List<String> players = new ArrayList<>();
    private int money = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        idTextView = findViewById(R.id.idTextView);
        idTextView.setText(getUserNickname());

        discoverStatus = findViewById(R.id.discoverStatusTextView);
        advertiseStatus = findViewById(R.id.advertStatusTextView);

        // project uses xml onclicks as well

        startScreen = findViewById(R.id.startLayout);
        hostScreen = findViewById(R.id.hostLayout);
        joinScreen = findViewById(R.id.joinLayout);
        gameScreen = findViewById(R.id.gameLayout);
        loaderScreen = findViewById(R.id.loaderLayout);


        gameMoneyTextView = findViewById(R.id.gameMoney);
        gamePlayersTextView = findViewById(R.id.gamePlayers);

        paySpinner = findViewById(R.id.paySpinner);



        //check for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    //common for advertisers and discoverers
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            // format of payload
            // 1. convert byte to string first.
            // 2. strings are comma separated, split to an array
            // 3.   index 0: type

            final String[] result = new String(payload.asBytes()).split(",");

            switch (result[0]) {
                case "start":
                    //[type, starting money, player names]
                    money = Integer.parseInt(result[1]);
                    players.clear();
                    players.addAll(Arrays.asList(result).subList(2, result.length));
                    StringBuilder builder = new StringBuilder();
                    for (int i = 2; i < players.size(); i++) {
                        builder.append(players.get(i));
                        builder.append("\n");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    gamePlayersTextView.setText(builder.toString());
                    gameMoneyTextView.setText(money);

                    showScreen(iGameScreen);
                    break;
                case "get":

                    break;
                case "pay":

                    break;
                default:
                    Toast.makeText(MainActivity.this, "Unknown payload delivered", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            // no need to implement, only required for file or stream.
        }
    };

    //common for advertisers and discoverers
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            // connection was requested by discoverer, and needs to be accepted by both now
            Nearby.getConnectionsClient(MainActivity.this)
                    .acceptConnection(s, payloadCallback);

            stopDiscovery(); //
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    // We're connected! Can now start sending and receiving data.
                    // add to players
                    Toast.makeText(getApplicationContext(), "Connected to: " + s, Toast.LENGTH_SHORT).show();
                    discoverStatus.setText("Connected to:" + s + "'s Game");
                    players.add(s);
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    // The connection was rejected by one or both sides.
                    Toast.makeText(getApplicationContext(), "Connection rejected: " + s, Toast.LENGTH_SHORT).show();
                    discoverStatus.setText("Connection rejected: " + s);

                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    // The connection broke before it was able to be accepted.
                    Toast.makeText(getApplicationContext(), "Connection broke: " + s, Toast.LENGTH_SHORT).show();
                    discoverStatus.setText("Connection broke: " + s);
                    break;
                default:
                    // Unknown status code
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            Toast.makeText(getApplicationContext(), "Disconnected: " + s, Toast.LENGTH_SHORT).show();
            discoverStatus.setText("...");
        }
    };

    public void startAdvertising(View view) {
        showScreen(iLoaderScreen);
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_STAR).build();

        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        getUserNickname(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showScreen(iHostScreen);
                        Toast.makeText(getApplicationContext(), "Advertising started", Toast.LENGTH_SHORT).show();
                        players.clear();
                        players.add(getUserNickname());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showScreen(iStartScreen);
                        Toast.makeText(getApplicationContext(), "Failed to start Advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void stopAdvertising(View view) {
        Nearby.getConnectionsClient(this).stopAdvertising();
        Toast.makeText(getApplicationContext(), "Advertising stopped", Toast.LENGTH_SHORT).show();
    }

    public void startDiscovery(View view) {
        showScreen(iLoaderScreen);
        discoverStatus.setText("...");

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build();
        Nearby.getConnectionsClient(this)
                .startDiscovery(SERVICE_ID, new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                        Nearby.getConnectionsClient(MainActivity.this)
                                .requestConnection(getUserNickname(), s, connectionLifecycleCallback)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getApplicationContext(), "Requested Connection", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(), "Connection Requesting failed:" + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onEndpointLost(@NonNull String s) {
                        Toast.makeText(getApplicationContext(), "An Endpoint Disappeared: " + s, Toast.LENGTH_SHORT).show();
                    }
                }, discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showScreen(iJoinScreen);
                        Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showScreen(iStartScreen);
                        Toast.makeText(getApplicationContext(), "Failed to start Discovery: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void stopDiscovery() {
        Nearby.getConnectionsClient(this).stopDiscovery();
        Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
    }

    public void quitJoining(View view) {
        stopDiscovery();
        Nearby.getConnectionsClient(this).stopAllEndpoints();
        showScreen(iStartScreen);
    }

    public void quitHosting(View view) {
        stopAdvertising(null);
        Nearby.getConnectionsClient(this).stopAllEndpoints();
        showScreen(iStartScreen);
    }

    public void startGame(View view) {
        Nearby.getConnectionsClient(this).stopAdvertising();

        StringBuilder builder = new StringBuilder();
        builder.append("start");
        builder.append(",");
        builder.append("1500"); //initial money
        builder.append(",");
        for (String player : players) {
            builder.append(player);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);

        for (String player : players) {
            Nearby.getConnectionsClient(this).sendPayload(player, Payload.fromBytes(builder.toString().getBytes()));
        }

        showScreen(iLoaderScreen);
    }

    public void getButtonClick(View view) {

    }

    public void payButtonClick(View view) {

    }


    private String getUserNickname() {
        if (nickname == null)
            nickname = "User " + (int) (Math.random() * 100);
        return nickname;
    }

    public void editUserID(View view) {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!userInput.getText().toString().equals(""))
                                    idTextView.setText(userInput.getText());
                            }
                        })
                .create()
                .show();
    }

    private void showScreen(int screenNumber) {
        startScreen.setVisibility(screenNumber == iStartScreen ? View.VISIBLE : View.GONE);
        hostScreen.setVisibility(screenNumber == iHostScreen ? View.VISIBLE : View.GONE);
        joinScreen.setVisibility(screenNumber == iJoinScreen ? View.VISIBLE : View.GONE);
        gameScreen.setVisibility(screenNumber == iGameScreen ? View.VISIBLE : View.GONE);
        loaderScreen.setVisibility(screenNumber == iLoaderScreen ? View.VISIBLE : View.GONE);

        if (screenNumber == iStartScreen) {
            //change all textviews to initial state
            discoverStatus.setText("...");
            advertiseStatus.setText("...");
            gamePlayersTextView.setText("...");
            gameMoneyTextView.setText("...");
        }
    }

}
