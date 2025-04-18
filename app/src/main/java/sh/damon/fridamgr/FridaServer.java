package sh.damon.fridamgr;

import android.util.Log;

import androidx.core.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sh.damon.fridamgr.http.HttpClient;
import sh.damon.fridamgr.models.github.Release;
import sh.damon.fridamgr.models.github.ReleaseAsset;
import sh.damon.fridamgr.util.Architecture;
import sh.damon.fridamgr.util.Archive;
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
        UPDATING,
        STARTING, STOPPING
    }

    private final FridaRepository repo = new FridaRepository();

    private final String mName;
    private final File mBinary;
    private final FridaArguments mArgs = new FridaArguments();
    private String mVersion;

    private Collection<FridaServerCallback> mCallbacks = null;
    private final DownloadState mDownloadState = new DownloadState();
    private State mState = State.UNKNOWN;

    private int mPid = -1;

    public FridaServer(File baseDir) {
        mName = "frida-server";
        mBinary = new File(baseDir, mName);
        mCallbacks = new LinkedList<>();

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
            if (!HttpClient.download(
                    toDownload.browser_download_url,
                    archive,
                    (bytesRead, total, done) -> mDownloadState.setProgress(Math.round(((float)bytesRead / total) * 50)))) {
                Log.e("FridaServer", "Failed to download Frida Server from Github.");
                return false;
            }

            if (!Archive.decompress(archive, mBinary, (bytesRead, total, done) -> mDownloadState.setProgress(50 + Math.round(((float)bytesRead / total) * 50)))) {
                Log.e("FridaServer", "Failed to decompress Frida Server archive.");
                return false;
            }

            if (!archive.delete()) {
                Log.w("FridaServer", "Failed to remove Frida Server archive file.");
            }
            return ShellUtil.runAsSuperuser(String.format("chmod 755 %s", mBinary)).isSuccess();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
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
        mState = State.STOPPING;
        emit();

        // kill process cleanly
        final ShellUtil.ProcessResponse res =
                ShellUtil.runAsSuperuser(String.format("pkill -15 %s", mName));

        // wait for process to exit
        for (int i = 0; i < 150; ++i) {
            if (ShellUtil.runAsSuperuser(String.format("pgrep %s 1>/dev/null && sleep 0.1", mName)).isFail()) {
                break;
            }
        }

        // die already!!!
        if (ShellUtil.runAsSuperuser(String.format("pkill -9 %s", mName)).isSuccess()) {
            Log.i("FridaServer", "Server process was still running, force killed it.");
        }

        // reset USAP state
        toggleUsap(true);

        updateState();
        return res.isSuccess();
    }

    public boolean start() {
        if (mState == State.NOT_INSTALLED)
            return false;
        mState = State.STARTING;
        emit();

        final StringBuilder cmdStr = new StringBuilder()
                .append(mBinary)
                .append(" -D");

        Optional<String> listenAddress;
        if ((listenAddress = mArgs.getListenAddress()).isPresent()) {
            cmdStr.append(" -l ").append(listenAddress.get());
        }

        // disable USAP so Frida can attach properly
        toggleUsap(false);

        // launch program
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
            case STARTING:
                return R.string.frida_state_starting;
            case STOPPING:
                return R.string.frida_state_stopping;
        }

        return R.string.frida_state_unknown;
    }

    public String getVersion() {
        return mVersion;
    }

    public void registerEventListener(FridaServerCallback callback) {
        mCallbacks.add(callback);

        mDownloadState.setProgressListener(this::emit);
    }

    public void unregisterEventListener(FridaServerCallback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * USAP is responsible for creating empty Zygote process, read to be used by applications.
     * This may prevent Frida from properly starting remote processes.
     * @param toggle True to enable USAP, false to disable.
     * @return True on successful modification, false otherwise.
     */
    private boolean toggleUsap(boolean toggle) {
        // determine usap state
        ShellUtil.ProcessResponse res = ShellUtil.runAsSuperuser("getprop | grep usap | awk -F'[][]' '{print $2 \" \" $4}'");
        if (res.isSuccess() && !res.out.isEmpty()) {
            final String[] tmp = res.out.split(" ");
            final Pair<String, String> prop = new Pair<>(tmp[0], tmp[1]);

            Log.v("FridaServer", String.format("Found %s property, %s.", prop.first, toggle ? "enabling" : "disabling"));
            res = ShellUtil.runAsSuperuser(String.format("setprop %s %s", prop.first, toggle ? "true" : "false"));

            return res.isSuccess();
        }

        return false;
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

    public void emit() {
        for (FridaServerCallback callback : mCallbacks) {
            callback.call();
        }
    }

    public void updateState() {
        try {
            if (ShellUtil.runAsSuperuser(String.format("test -e %s", mBinary)).isFail()) {
                mState = State.NOT_INSTALLED;
                mPid = -1;
                return;
            }

            mVersion = ShellUtil.runAsSuperuser(String.format("%s --version", mBinary)).out;

            final ShellUtil.ProcessResponse res = ShellUtil.runAsSuperuser(String.format("pgrep %s", mName));
            if (res.isFail()) {
                mState = State.STOPPED;
                mPid = -1;
            }
            else {
                mState = State.RUNNING;
                try {
                    mPid = Integer.parseInt(res.out.trim());
                } catch (NumberFormatException e) {
                    mPid = -1;
                }
            }
        } finally {
            emit();
        }
    }

    public int getPid() {
        return mPid;
    }

    private static FridaServer instance = null;
    public static FridaServer init(File baseDir) {
        if (instance != null) {
            Log.v("FridaServer", "Already instantiated.");
            return instance;
        }

        return instance = new FridaServer(baseDir);
    }

    public static FridaServer getInstance() {
        if (instance == null) {
            throw new RuntimeException("Please instantiated FridaServer before calling getInstance.");
        }
        return instance;
    }
}
