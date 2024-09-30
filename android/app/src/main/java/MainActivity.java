package com.example.proxy;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.proxy/vpn";
    private MyVpnService vpnService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize VpnService
        vpnService = new MyVpnService();

        // Set up MethodChannel
        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    if (call.method.equals("startVpn")) {
                        // Prepare the VPN service
                        Intent intent = VpnService.prepare(getApplicationContext());
                        if (intent != null) {
                            startActivityForResult(intent, 0); // Call this method to request the VPN permission
                        } else {
                            // No permission required, start the service directly
                            startVpnService();
                        }
                        result.success(null);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                // Permission granted, start the VPN service
                startVpnService();
            }
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, MyVpnService.class);
        startService(intent);
        // Set the channel for MyVpnService
        vpnService.setChannel(new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL));
    }
}
