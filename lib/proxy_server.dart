import 'package:dio/dio.dart';

class NetworkService {
  final Dio _dio = Dio();

  NetworkService() {
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        print('Request: ${options.method} ${options.uri}');
        print('Headers: ${options.headers}');
        print('Data: ${options.data}');
        return handler.next(options); // Continue the request
      },
      onResponse: (response, handler) {
        print('Response [${response.statusCode}]: ${response.data}');
        return handler.next(response); // Continue the response
      },
      onError: (DioError error, handler) {
        print('Error [${error.response?.statusCode}]: ${error.message}');
        return handler.next(error); // Continue the error
      },
    ));
  }

  // GET request example
  Future<Response> getRequest(String url) async {
    try {
      Response response = await _dio.get(url);
      return response;
    } catch (e) {
      print(e);
      rethrow;
    }
  }

  // POST request example
  Future<Response> postRequest(String url, Map<String, dynamic> data) async {
    try {
      Response response = await _dio.post(url, data: data);
      return response;
    } catch (e) {
      print(e);
      rethrow;
    }
  }
}
