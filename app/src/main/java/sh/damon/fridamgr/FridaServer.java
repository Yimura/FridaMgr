package sh.damon.fridamgr;

import android.util.Log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sh.damon.fridamgr.models.github.Release;
import sh.damon.fridamgr.models.github.ReleaseAsset;
import sh.damon.fridamgr.util.Architecture;
import sh.damon.fridamgr.util.Archive;
import sh.damon.fridamgr.util.Curl;
import sh.damon.fridamgr.util.ShellUtil;

public class FridaServer {
    public interface UpdateCallback {
        void call();
    }

    public enum State {
        UNKNOWN,
        NOT_INSTALLED,
        STOPPED,
        RUNNING,
        UPDATING
    }

    private final FridaRepository repo = new FridaRepository();

    private final String mName;
    private final File mBinary;
    private String mVersion;

    private UpdateCallback mCallback = null;
    private State mState = State.UNKNOWN;

    FridaServer(File baseDir) {
        mName = "frida-server";
        mBinary = new File(baseDir, mName);

        updateState();
    }

    public boolean install() {
        final Release release = repo.getLatestRelease();
        ReleaseAsset toDownload = null;

        final String architecture = Architecture.getString();
        Pattern regexp = Pattern.compile(String.format("frida-server-\\d+[.]\\d+[.]\\d+-android-%s[.]xz", architecture));
        for (ReleaseAsset asset : release.assets) {
            Matcher matcher = regexp.matcher(asset.name);
            if (matcher.find()) {
                toDownload = asset;

                break;
            }
        }

        if (toDownload == null) {
            Log.e("FridaServer", "Failed to find an appropriate Frida Server binary.");
            return false;
        }

        final File archive = new File(mBinary + ".xz");
        if (!Curl.download(toDownload.browser_download_url, archive)) {
            Log.e("FridaServer", "Failed to download Frida Server from Github.");
            return false;
        }

        if (!Archive.decompress(archive, mBinary)) {
            Log.e("FridaServer", "Failed to decompress Frida Server archive.");
            return false;
        }

        if (!archive.delete()) {
            Log.w("FridaServer", "Failed to remove Frida Server archive file.");
        }

        boolean status = ShellUtil.runAsSuperuser(String.format("chmod +x %s", mBinary)).isSuccess();
        updateState();
        return status;
    }

    public boolean kill() {
        final ShellUtil.ProcessResponse res =
                ShellUtil.runAsSuperuser(String.format("pkill -15 %s; while pgrep %s 1>/dev/null; do sleep 0.1; done", mName, mName));
        updateState();
        return res.isSuccess();
    }

    public boolean start() {
        final ShellUtil.ProcessResponse res = ShellUtil.runAsSuperuser(String.format("%s -D", mBinary));

        updateState();
        return res.isSuccess();
    }

    public State getState() {
        return mState;
    }

    public int getStateStringId() {
        switch (mState) {
            case NOT_INSTALLED:
                return R.string.frida_state_not_installed;
            case STOPPED:
                return R.string.frida_state_stopped;
            case RUNNING:
                return R.string.frida_state_running;
            case UPDATING:
                return R.string.frida_state_updating;
        }

        return R.string.frida_state_unknown;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setOnUpdateListener(UpdateCallback callback) {
        mCallback = callback;
    }

    public void updateState() {
        Runnable r = () -> {
            if (ShellUtil.runAsSuperuser(String.format("test -e %s", mBinary)).isFail()) {
                mState = State.NOT_INSTALLED;

                return;
            }

            mVersion = ShellUtil.runAsSuperuser(String.format("%s --version", mBinary)).out;

            final ShellUtil.ProcessResponse res = ShellUtil.runAsSuperuser(String.format("pgrep %s", mName));
            if (res.isFail())
            {
                mState = State.STOPPED;
            }
            else {
                mState = State.RUNNING;
            }
        };
        r.run();

        if (mCallback != null) {
            mCallback.call();
        }
    }
}
