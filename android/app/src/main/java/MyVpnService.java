package com.example.proxy;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import io.flutter.plugin.common.MethodChannel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MyVpnService extends VpnService {
    private ParcelFileDescriptor vpnInterface;
    private MethodChannel channel;
    private Thread readingThread;

    // Allow setting a channel for communication with Flutter
    public void setChannel(MethodChannel channel) {
        this.channel = channel;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MyVpnService", "VPN Service Created");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // Create the VPN interface using the Builder pattern
        VpnService.Builder builder = new VpnService.Builder();

        // Add local VPN interface IP (your device's address on the VPN)
        builder.addAddress("192.168.1.8", 24);  // Modify this if necessary

        // Optional: Set the MTU (Maximum Transmission Unit)
        builder.setMtu(1400);

        // Establish the VPN interface
        try {
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e("MyVpnService", "Failed to establish VPN interface.");
                return START_NOT_STICKY;
            }
            Log.i("MyVpnService", "VPN interface established.");
        } catch (Exception e) {
            Log.e("MyVpnService", "Error establishing VPN: " + e.getMessage(), e);
            return START_NOT_STICKY;
        }

        // Set up OkHttpClient with Interceptor to capture HTTP traffic
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();

                        // Capture the URL, headers, and body of the request
                        String url = request.url().toString();
                        String headers = request.headers().toString();
                        String body = request.body() != null ? request.body().toString() : "No body";

                        // Log before sending data to Flutter
                        Log.d("MyVpnService", "Intercepting request: URL: " + url + ", Headers: " + headers);

                        // Send the captured data to Flutter using MethodChannel
                        if (channel != null) {
                            HashMap<String, String> data = new HashMap<>();
                            data.put("url", url);
                            data.put("headers", headers);
                            channel.invokeMethod("logRequest", data);
                        }

                        // Proceed with the original request
                        return chain.proceed(request);
                    }
                })
                .retryOnConnectionFailure(true)  // Retry on connection failure
                .connectTimeout(10, TimeUnit.SECONDS) // Adjust timeout if needed
                .readTimeout(10, TimeUnit.SECONDS)  // Adjust timeout if needed
                .build();

        // Start inspecting VPN traffic
        startTrafficInspection();

        return START_STICKY;
    }


    private void processPacket(byte[] rawData) {
        try {
            Log.d("Called","Called");
            // Basic parsing: Assume Ethernet + IPv4 + TCP
            // Ethernet header is 14 bytes
            if (rawData.length < 14) return;
            Log.d("Called","Called2");
            // int etherType = ((rawData[12] & 0xFF) << 8) | (rawData[13] & 0xFF);
            // if (etherType != 0x0800) { // IPv4
            //     return;
            // }
            Log.d("Called","Called3");
            // IPv4 header starts at byte 14
            if (rawData.length < 14 + 20) return; // Minimum IPv4 header size
            Log.d("Called","Called4");
            int ipHeaderLength = (rawData[14] & 0x0F) * 4;
            // int protocol = rawData[14 + 9] & 0xFF;
            // if (protocol != 6) { // TCP
            //     return;
            // }
            Log.d("Called","Called5");
            // TCP header starts after IP header
            int tcpHeaderStart = 14 + ipHeaderLength;
            if (rawData.length < tcpHeaderStart + 20) return; // Minimum TCP header size
            Log.d("Called","Called6");
            int tcpHeaderLength = ((rawData[tcpHeaderStart + 12] & 0xF0) >> 4) * 4;
            int dataStart = tcpHeaderStart + tcpHeaderLength;
            if (rawData.length <= dataStart) return;
            Log.d("Called","Called6");
            int dataLength = rawData.length - dataStart;
            if (dataLength <= 0) return;
            Log.d("Called","Called8");
            byte[] payload = new byte[dataLength];
            System.arraycopy(rawData, dataStart, payload, 0, dataLength);
            String payloadStr = new String(payload, "UTF-8");
            Log.d("Called","Called9");
           
                Log.d("MYVPNSERV", "HTTP Request: " + payloadStr);
                Log.d("Called","Called10");
                // TODO: Send this payload to Flutter via Platform Channels or Event Channels
                // Example: sendToFlutter(payloadStr);
            
        } catch (Exception e) {
            Log.e("MYVPNSERV", "Error parsing packet", e);
        }
    }

    // Method to start capturing and inspecting VPN traffic
    private void startTrafficInspection() {
        readingThread = new Thread(() -> {
            FileInputStream input = null;
            FileOutputStream output = null;
            try {
                input = new FileInputStream(vpnInterface.getFileDescriptor());
                output = new FileOutputStream(vpnInterface.getFileDescriptor());

                byte[] packet = new byte[32767];  // Allocate buffer for the VPN packets

                while (true) {
                    int length = input.read(packet);
                    if (length > 0) {
                        // Process the packet here (e.g., logging, filtering)
                        Log.d("MyVpnService", "Captured packet of length: " + length);
                       // Log.d("MyVpnService","Packet:"+packet);

                        // Write back the packet if needed (forward traffic)

                        processPacket(packet);
                        
                        output.write(packet, 0, length);
                       //s String packetContent = new String(packet, 0, length, StandardCharsets.UTF_16);Log.d("MyVpnService", "Captured packet content: " + packetContent);
                    }
                }
            
            } catch (IOException e) {
                Log.e("MyVpnService", "Error capturing traffic: " + e.getMessage(), e);
            } finally {
                // Close the streams if they were opened
                try {
                    if (input != null) {
                        input.close();
                    }
                    if (output != null) {
                        output.close();
                    }
                } catch (IOException e) {
                    Log.e("MyVpnService", "Error closing streams: " + e.getMessage(), e);
                }
            }
        });
        readingThread.start();
    }

    // ... (other code)

@Override
public void onDestroy() {
    super.onDestroy();
    try {
        if (vpnInterface != null) {
            // Wait for the reading thread to finish before closing
            joinReadingThread();
            vpnInterface.close();
        }
    } catch (IOException | InterruptedException e) {
        Log.e("MyVpnService", "Error closing VPN interface/waiting for thread: " + e.getMessage(), e);
    }
    Log.i("MyVpnService", "VPN Service Destroyed");
}

private void joinReadingThread() throws InterruptedException {
    if (readingThread != null) {
        readingThread.join();
    }
}
}