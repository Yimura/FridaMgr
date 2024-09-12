package sh.damon.fridamgr;

import android.util.Log;

import java.io.IOException;

import sh.damon.fridamgr.models.github.Release;
import sh.damon.fridamgr.util.HttpClient;

public class FridaRepository {
    private final String mContext = "frida/frida";
    private String mVersion;

    public FridaRepository() {

    }

    public Release getLatestRelease() {
        try {
            return HttpClient.getJson("https://api.github.com/repos/frida/frida/releases/latest", Release.class);
        }
        catch (IOException ex) {
            Log.e("FridaRepository", "Failed to grab latest release.");

            return null;
        }
    }
}
