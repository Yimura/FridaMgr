package sh.damon.fridamgr;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sh.damon.fridamgr.models.github.Release;
import sh.damon.fridamgr.models.github.ReleaseAsset;

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

        Pattern regexp = Pattern.compile("frida-server-\\d+[.]\\d+[.]\\d+-android-arm64[.]xz");
        for (ReleaseAsset asset : release.assets) {
            Matcher matcher = regexp.matcher(asset.name);
            if (matcher.find()) {
                toDownload = asset;

                break;
            }
        }

        if (toDownload == null) {
            return false;
        }

        if (!Curl.download(toDownload.browser_download_url, new File(mBinary + ".xz"))) {
            return false;
        }

        if (ShellUtil.runAsSuperuser(String.format("busybox xz -fd %s", mBinary + ".xz")).isFail()) {
            return false;
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
