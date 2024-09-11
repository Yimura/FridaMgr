package sh.damon.fridamgr;

import sh.damon.fridamgr.models.github.Release;

public class FridaRepository {
    private final String mContext = "frida/frida";
    private String mVersion;

    public FridaRepository() {

    }

    public Release getLatestRelease() {
        return Curl.getJson("https://api.github.com/repos/frida/frida/releases/latest", Release.class);
    }
}
