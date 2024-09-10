package sh.damon.fridamgr;

public class FridaServer {
    enum State {
        UNKNOWN,
        NOT_INSTALLED,
        STOPPED,
        RUNNING,
        UPDATING
    }

    State mState = State.NOT_INSTALLED;

    FridaServer() {

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
}
