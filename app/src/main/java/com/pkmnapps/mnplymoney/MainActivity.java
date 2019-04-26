package com.pkmnapps.mnplymoney;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String SERVICE_ID = "com.pkmnapps.mnplymoney_service_id";
    private String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            Nearby.getConnectionsClient(MainActivity.this)
                    .acceptConnection(s, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    // We're connected! Can now start sending and receiving data.
                    Toast.makeText(getApplicationContext(), "Connected to: " + s, Toast.LENGTH_SHORT).show();
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    // The connection was rejected by one or both sides.
                    Toast.makeText(getApplicationContext(), "Connection rejected: " + s, Toast.LENGTH_SHORT).show();
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    // The connection broke before it was able to be accepted.
                    Toast.makeText(getApplicationContext(), "Connection broke: " + s, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    // Unknown status code
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            Toast.makeText(getApplicationContext(), "Disconnected: " + s, Toast.LENGTH_SHORT).show();
        }
    };

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_STAR).build();

        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        getUserNickname(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Advertising started", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to start Advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void stopAdvertising() {
        Nearby.getConnectionsClient(this).stopAdvertising();
        Toast.makeText(getApplicationContext(), "Advertising stopped", Toast.LENGTH_SHORT).show();
    }

    private void startDiscovery() {
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
                        Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to start Discovery: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getUserNickname() {
        if (nickname == null)
            nickname = String.valueOf((int) (Math.random() * 100));
        return nickname;
    }
}
