package sh.damon.fridamgr.http;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sh.damon.fridamgr.listener.ProgressListener;

public class HttpClient {
    final static OkHttpClient client = new OkHttpClient();


    public static boolean download(String url, File file, ProgressListener listener) throws IOException {
        final OkHttpClient dlClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response origResponse = chain.proceed(chain.request());

                    return origResponse.newBuilder()
                            .body(new ProgressResponseBody(origResponse.body(), listener))
                            .build();
                })
                .build();

        Request req = new Request.Builder()
                .url(url).build();
        try (
            Response res = dlClient.newCall(req).execute();
            OutputStream outStream = Files.newOutputStream(file.toPath())) {
            byte[] buffer = new byte[8 * 1024];

            InputStream inputStream = res.body().byteStream();
            for (int bytesRead; (bytesRead = inputStream.read(buffer)) != -1;) {
                outStream.write(buffer, 0, bytesRead);
            }

            return true;
        }
    }

    public static Response get(String url) throws IOException {
        Request req = new Request.Builder()
                .url(url).build();

        try (Response res = client.newCall(req).execute()) {
            return res;
        }
    }

    public static <T> T getJson(String url, Class<T> clazz) throws IOException {
        Request req = new Request.Builder()
                .url(url).build();

        try (Response res = client.newCall(req).execute()) {
            return new Gson().fromJson(res.body().charStream(), clazz);
        }
    }
}
