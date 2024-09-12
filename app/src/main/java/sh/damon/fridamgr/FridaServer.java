package sh.damon.fridamgr;

import android.util.Log;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sh.damon.fridamgr.models.github.Release;
import sh.damon.fridamgr.models.github.ReleaseAsset;
import sh.damon.fridamgr.util.Architecture;
import sh.damon.fridamgr.util.Archive;
import sh.damon.fridamgr.util.Curl;
import sh.damon.fridamgr.util.ShellUtil;

public class FridaServer {
    public interface FridaServerCallback {
        void call();
    }

    public static class DownloadState {
        FridaServerCallback callback = null;
        int progress = 0;

        int getProgress() {
            return progress;
        }

        void reset () {
            progress = 0;

            if (callback != null) {
                callback.call();
            }
        }

        void setProgress(int newProgress) {
            progress = newProgress;

            if (callback != null) {
                callback.call();
            }
        }

        void setProgressListener(FridaServerCallback cb) {
            callback = cb;
        }
    }

    private static class FridaArguments {
        private String listenAddress = null;

        private Optional<String> getListenAddress() {
            return Optional.ofNullable(listenAddress);
        }

        private void setPortNumber(int number) {
            listenAddress = "0.0.0.0:" + number;
        }

        private void resetListenAddress() {
            listenAddress = null;
        }
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
    private final FridaArguments mArgs = new FridaArguments();
    private String mVersion;

    private FridaServerCallback mCallback = null;
    private final DownloadState mDownloadState = new DownloadState();
    private State mState = State.UNKNOWN;

    public FridaServer(File baseDir) {
        mName = "frida-server";
        mBinary = new File(baseDir, mName);

        updateState();
    }

    public boolean install() {
        try {
            mDownloadState.reset();
            mState = State.UPDATING;

            final Release release = repo.getLatestRelease();
            if (release == null) {
                return false;
            }
            mDownloadState.setProgress(20);

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
            mDownloadState.setProgress(40);

            final File archive = new File(mBinary + ".xz");
            if (!Curl.download(toDownload.browser_download_url, archive)) {
                Log.e("FridaServer", "Failed to download Frida Server from Github.");
                return false;
            }
            mDownloadState.setProgress(60);

            if (!Archive.decompress(archive, mBinary)) {
                Log.e("FridaServer", "Failed to decompress Frida Server archive.");
                return false;
            }
            mDownloadState.setProgress(80);

            if (!archive.delete()) {
                Log.w("FridaServer", "Failed to remove Frida Server archive file.");
            }
            return ShellUtil.runAsSuperuser(String.format("chmod +x %s", mBinary)).isSuccess();
        }
        finally {
            mDownloadState.setProgress(100);

            updateState();
        }
    }

    public void update() {
        boolean isRunning = mState == State.RUNNING;
        if (isRunning) {
            kill();
        }

        install();

        if (isRunning) {
            start();
        }
    }

    public boolean restart() {
        return kill() && start();
    }

    public boolean kill() {
        final ShellUtil.ProcessResponse res =
                ShellUtil.runAsSuperuser(String.format("pkill -15 %s; while pgrep %s 1>/dev/null; do sleep 0.1; done", mName, mName));
        updateState();
        return res.isSuccess();
    }

    public boolean start() {
        if (mState == State.NOT_INSTALLED)
            return false;

        final StringBuilder cmdStr = new StringBuilder()
                .append(mBinary)
                .append(" -D");

        Optional<String> listenAddress;
        if ((listenAddress = mArgs.getListenAddress()).isPresent()) {
            cmdStr.append(" -l ").append(listenAddress);
        }

        final ShellUtil.ProcessResponse res = ShellUtil.runAsSuperuser(cmdStr.toString());

        updateState();
        return res.isSuccess();
    }

    public DownloadState getDownloadState() {
        return mDownloadState;
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

    public void setOnUpdateListener(FridaServerCallback callback) {
        mCallback = callback;

        mDownloadState.setProgressListener(callback);
    }

    public void toggleListenPort(boolean toggle, int port) {
        if (port < 1_000 || port > 65_535) {
            Log.e("FridaServer", "Invalid port range given.");
            return;
        }

        mArgs.resetListenAddress();
        if (toggle) {
            mArgs.setPortNumber(port);
        }

        if (mState == State.RUNNING) {
            restart();
        }
    }

    public void updateState() {
        try {
            if (ShellUtil.runAsSuperuser(String.format("test -e %s", mBinary)).isFail()) {
                mState = State.NOT_INSTALLED;

                return;
            }

            mVersion = ShellUtil.runAsSuperuser(String.format("%s --version", mBinary)).out;

            final ShellUtil.ProcessResponse res = ShellUtil.runAsSuperuser(String.format("pgrep %s", mName));
            if (res.isFail()) {
                mState = State.STOPPED;
            }
            else {
                mState = State.RUNNING;
            }
        }
        finally {
            if (mCallback != null) {
                mCallback.call();
            }
        }
    }
}
