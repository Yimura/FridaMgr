package sh.damon.fridamgr.util;

import com.google.gson.Gson;

import java.io.File;

public class Curl {
    public static boolean download(String url, File file) {
        final ShellUtil.ProcessResponse res = ShellUtil.run(String.format("curl -L -o %s %s", file.getAbsolutePath(), url));
        return res.isSuccess();
    }

    public static String get(String url) {
        final ShellUtil.ProcessResponse res = ShellUtil.run(String.format("curl %s", url));
        if (res.isSuccess()) {
            return res.out;
        }
        return null;
    }

    public static <T> T getJson(String url, Class<T> type) {
        return new Gson().fromJson(get(url), type);
    }
}
