package sh.damon.fridamgr.util;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClient {
    final static OkHttpClient client = new OkHttpClient();

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
