package sh.damon.fridamgr;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import sh.damon.fridamgr.preferences.Preferences;
import sh.damon.fridamgr.preferences.UserPreferences;

public class FridaServerTileService extends TileService {
    private FridaServer server = null;

    public FridaServerTileService() {
    }

    // Called when the user adds your tile.
    @Override
    public void onTileAdded() {
        super.onTileAdded();

        final FridaServer server = getServer();
        if (server.getState() == FridaServer.State.NOT_INSTALLED) {
            Toast.makeText(getApplicationContext(), "Installing server so the tile is operational.", Toast.LENGTH_SHORT).show();

            server.install();
        }
    }

    // Called when your app can update your tile.
    @Override
    public void onStartListening() {
        final FridaServer server = getServer();
        Tile tile = getQsTile();
        if (server.getState() == FridaServer.State.NOT_INSTALLED) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.updateTile();
        }

        server.registerEventListener(this::updateTileState);
        server.updateState();

        super.onStartListening();
    }

    // Called when your app can no longer update your tile.
    @Override
    public void onStopListening() {
        super.onStopListening();

        final FridaServer server = getServer();
        server.unregisterEventListener(this::updateTileState);
    }

    // Called when the user taps on your tile in an active or inactive state.
    @Override
    public void onClick() {
        super.onClick();

        final FridaServer server = getServer();
        server.updateState();

        FridaServer.State state = server.getState();
        if (state == FridaServer.State.STOPPED) {
            boolean listenOnNetwork = UserPreferences.get(Preferences.LISTEN_ON_NETWORK, false);
            int portNumber = UserPreferences.get(Preferences.PORT_NUMBER, 27055);

            server.toggleListenPort(listenOnNetwork, portNumber);
            server.start();
        }
        else if (state == FridaServer.State.RUNNING) {
            server.kill();
        }
    }

    // Called when the user removes your tile.
    @Override
    public void onTileRemoved() {
        super.onTileRemoved();

        server.unregisterEventListener(this::updateTileState);
    }

    private FridaServer getServer() {
        if (server == null) {
            UserPreferences.load(getFilesDir());
            server = FridaServer.init(getFilesDir());
        }
        return server;
    }

    private void updateTileState() {
        Tile tile = getQsTile();

        final FridaServer server = getServer();
        switch (server.getState()) {
            case RUNNING: {
                tile.setState(Tile.STATE_ACTIVE);

                break;
            }
            case STOPPED: {
                tile.setState(Tile.STATE_INACTIVE);

                break;
            }
            default: {
                tile.setState(Tile.STATE_UNAVAILABLE);

                break;
            }
        }

        tile.updateTile();
    }
}