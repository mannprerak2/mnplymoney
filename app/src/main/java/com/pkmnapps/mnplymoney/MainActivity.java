package com.pkmnapps.mnplymoney;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String SERVICE_ID = "com.pkmnapps.mnplymoney_service_id";
    private static final String START_MONEY = "1500";
    private static final int iStartScreen = 0;
    private static final int iHostScreen = 1;
    private static final int iJoinScreen = 2;
    private static final int iGameScreen = 3;
    private static final int iLoaderScreen = 4;

    private int currentScreen = 0;

    private Snackbar b;

    private String nickname, hostId;
    private boolean isHost = false;
    private TextView idTextView, discoverStatus, advertiseStatus, gameMoneyTextView, gamePlayersTextView;
    private EditText getEditText, payEditText;
    private Spinner paySpinner;

    private View startScreen, hostScreen, joinScreen, gameScreen, loaderScreen;

    private Map<String, String> playerMap = new HashMap<>();

    private int money = 0;

    private String tempPlayerName = "";

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

        getEditText = findViewById(R.id.getEditText);
        payEditText = findViewById(R.id.payEditText);

        paySpinner = findViewById(R.id.paySpinner);

        //check for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        showScreen(currentScreen);
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
                    //[type, starting money, players[key, value..]]
                    money = Integer.parseInt(result[1]);
                    gameMoneyTextView.setText(String.valueOf(money));

                    playerMap.clear();
                    playerMap.put(hostId, result[3]); //first one is hostID
                    for (int i = 4; i < result.length; i += 2) {
                        playerMap.put(result[i], result[i + 1]);
                    }

                    updatePlayerTextView();

                    List<String> payPlayers = new ArrayList<>();
                    for (String player : (playerMap.values().toArray(new String[0]))) {
                        if (!player.equals(getUserNickname()))
                            payPlayers.add(player);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this, android.R.layout.simple_spinner_item, payPlayers);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    paySpinner.setAdapter(adapter);

                    showScreen(iGameScreen);
                    break;
                case "get":
                    //[type, receiver, amount]

                    final StringBuilder sbuilder = new StringBuilder();
                    sbuilder.append("allowed");
                    sbuilder.append(",");
                    sbuilder.append(result[1]);
                    sbuilder.append(",");
                    sbuilder.append(result[2]);
                    sbuilder.append(",");

                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(result[1] + " asks for " + result[2] + " from bank");
                    builder.setPositiveButton("Give", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sbuilder.append("y");
                            if (isHost)
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(getKey(playerMap, result[1]), Payload.fromBytes(sbuilder.toString().getBytes()));
                            else
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(hostId, Payload.fromBytes(sbuilder.toString().getBytes()));
                        }
                    });
                    builder.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sbuilder.append("n");
                            if (isHost)
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(getKey(playerMap, result[1]), Payload.fromBytes(sbuilder.toString().getBytes()));
                            else
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(hostId, Payload.fromBytes(sbuilder.toString().getBytes()));
                        }
                    });
                    builder.setCancelable(false);
                    builder.create().show();
                    break;

                case "allowed":
                    //[type, receiver, amount, y/n]
                    if (result[1].equals(getUserNickname())) {
                        b.dismiss();
                        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                        if (result[3].equals("y")) {
                            money += Integer.parseInt(result[2]);
                            gameMoneyTextView.setText(String.valueOf(money));
                            alertBuilder.setMessage("Received " + result[2]);
                        } else {
                            alertBuilder.setMessage("Denied " + result[2]);
                        }
                        alertBuilder.create().show();
                    } else if (isHost) { //forward to other players
                        Nearby.getConnectionsClient(MainActivity.this).sendPayload(getKey(playerMap, result[1]), payload);
                    }

                    break;
                case "pay":
                    //[type, sender, receiver, amount]
                    if (result[2].equals(getUserNickname())) {
                        money += Integer.parseInt(result[3]);
                        gameMoneyTextView.setText(String.valueOf(money));
                    } else if (isHost) {
                        //send payload to actual receiver.
                        Nearby.getConnectionsClient(MainActivity.this).sendPayload(getKey(playerMap, result[2]), payload);
                    }
                    break;
                default:
                    Toast.makeText(MainActivity.this, "Unknown payload delivered ", Toast.LENGTH_SHORT).show();
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
            tempPlayerName = connectionInfo.getEndpointName();
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
                    Toast.makeText(getApplicationContext(), "Connected to: " + tempPlayerName + ", id: " + s, Toast.LENGTH_SHORT).show();
                    discoverStatus.setText("Connected to " + tempPlayerName);
                    if (!isHost)
                        hostId = s;
                    playerMap.put(s, tempPlayerName);
                    updateAdvertiseTextView();
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
                    discoverStatus.setText("Connection broke: " + s);
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            Toast.makeText(getApplicationContext(), "Disconnected: " + s, Toast.LENGTH_SHORT).show();
            discoverStatus.setText("...");
            playerMap.remove(s);
            updateAdvertiseTextView();
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
                        playerMap.clear();
                        playerMap.put("0", getUserNickname());
                        isHost = true;
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

    // not related to stopping connection
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

    // not related to stopping connection
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
        showScreen(iGameScreen);

        Nearby.getConnectionsClient(this).stopAdvertising();

        StringBuilder builder = new StringBuilder();
        builder.append("start");
        builder.append(",");
        builder.append(START_MONEY); //initial money
        builder.append(",");
//        for (String player : players) {
//            builder.append(player);
//            builder.append(",");
//        }
        for (Map.Entry<String, String> entry : playerMap.entrySet()) {
            builder.append(entry.getKey());
            builder.append(",");
            builder.append(entry.getValue());
            builder.append(",");
        }

        builder.deleteCharAt(builder.length() - 1);

//        for (String player : players) {
//            if (!player.equals(getUserNickname()))
//                Nearby.getConnectionsClient(this).sendPayload(player, Payload.fromBytes(builder.toString().getBytes()));
//        }

        for (Map.Entry<String, String> entry : playerMap.entrySet()) {
            if (!entry.getValue().equals(getUserNickname())) {
                Nearby.getConnectionsClient(this).sendPayload(entry.getKey(), Payload.fromBytes(builder.toString().getBytes()));
            }
        }

        //[type, starting money, player names]
        money = Integer.parseInt(START_MONEY);
        gameMoneyTextView.setText(String.valueOf(money));

        updatePlayerTextView();
        List<String> payPlayers = new ArrayList<>();
        for (String player : (playerMap.values().toArray(new String[0]))) {
            if (!player.equals(getUserNickname()))
                payPlayers.add(player);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                MainActivity.this, android.R.layout.simple_spinner_item, payPlayers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paySpinner.setAdapter(adapter);

    }

    public void getButtonClick(final View view) {
        if (b != null && b.isShown())
            return;
        final int amount = Integer.parseInt(getEditText.getText().toString());
        if (amount <= 0)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Ask for " + amount + " from the bank");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                b = Snackbar.make(view, "Waiting for responses", Snackbar.LENGTH_INDEFINITE);
                b.show();

                StringBuilder builder = new StringBuilder();
                builder.append("get");
                builder.append(",");
                builder.append(getUserNickname());
                builder.append(",");
                builder.append(amount); //initial money

                //send payload to host
                if (isHost) {//send directly

                    Iterator<Map.Entry<String, String>> iterator = playerMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, String> entry = iterator.next();
                        if (!entry.getValue().equals(getUserNickname())) {
                            Nearby.getConnectionsClient(MainActivity.this).sendPayload(entry.getKey(), Payload.fromBytes(builder.toString().getBytes()));
                            break;
                        }
                    }
                } else {
                    Nearby.getConnectionsClient(MainActivity.this).sendPayload(hostId, Payload.fromBytes(builder.toString().getBytes()));
                }
            }
        });
        builder.create().show();

    }

    public void payButtonClick(View view) {
        final String payTo = (String) paySpinner.getSelectedItem();
        if (payTo.equals(getUserNickname())) {
            return;
        }
        final int amount = Integer.parseInt(payEditText.getText().toString());
        if (amount > money) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Pay " + amount + " to " + payTo);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                money -= amount;
                gameMoneyTextView.setText(String.valueOf(money));

                StringBuilder builder = new StringBuilder();
                builder.append("pay");
                builder.append(",");
                builder.append(getUserNickname());
                builder.append(",");
                builder.append(payTo);
                builder.append(",");
                builder.append(amount); //initial money

                //send payload to host
                if (isHost) {//send directly
                    Nearby.getConnectionsClient(MainActivity.this).sendPayload(getKey(playerMap, payTo), Payload.fromBytes(builder.toString().getBytes()));
                } else {
                    Nearby.getConnectionsClient(MainActivity.this).sendPayload(hostId, Payload.fromBytes(builder.toString().getBytes()));
                }

            }
        });
        builder.create().show();
    }

    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
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
                                if (!userInput.getText().toString().equals("")) {
                                    idTextView.setText(userInput.getText());
                                    nickname = userInput.getText().toString();
                                }
                            }
                        })
                .create()
                .show();
    }

    private void showScreen(int screenNumber) {
        currentScreen = screenNumber;

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
            isHost = false;
        }
    }

    private void updatePlayerTextView() {
        StringBuilder builder = new StringBuilder();
        builder.append("----Players----\n");
        for (String player : playerMap.values()) {
            builder.append(player);
            builder.append("\n");
        }
        builder.deleteCharAt(builder.length() - 1);
        gamePlayersTextView.setText(builder.toString());
    }

    private void updateAdvertiseTextView() {
        StringBuilder builder = new StringBuilder();
        builder.append("Connected Players\n");
        for (String player : playerMap.values()) {
            builder.append(player);
            builder.append("\n");
        }
        builder.deleteCharAt(builder.length() - 1);
        advertiseStatus.setText(builder.toString());
    }

}
