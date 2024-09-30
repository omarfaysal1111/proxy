import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Network Traffic Interceptor')),
        body: const Center(child: VpnControl()),
      ),
    );
  }
}

class VpnControl extends StatelessWidget {
  const VpnControl({Key? key}) : super(key: key);

  Future<void> startVpn() async {
    const platform = MethodChannel('com.example.proxy/vpn');

    platform.setMethodCallHandler((call) async {
      if (call.method == 'logRequest') {
        final Map<String, String> data = call.arguments;
        final String url = data['url'].toString();
        final String headers = data['headers'].toString();

        // Print the URL and headers
        print('Captured URL: $url');
        print('Captured Headers: $headers');
      }
    });

    try {
      final result = await platform.invokeMethod('startVpn');
      //final result2 = await platform.invokeMethod('logRequest');
      print(result);
    } catch (e) {
      print("Error: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: startVpn,
      child: const Text('Start VPN'),
    );
  }
}
