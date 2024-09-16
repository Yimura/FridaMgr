package sh.damon.fridamgr.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

import sh.damon.fridamgr.FridaServer;
import sh.damon.fridamgr.preferences.Preferences;
import sh.damon.fridamgr.preferences.UserPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            UserPreferences.load(new File(context.getFilesDir(), "fridaMgr.json"));
            final FridaServer server = FridaServer.init(context.getFilesDir());

            if (UserPreferences.get(Preferences.START_ON_BOOT, false)) {
                server.start();
            }
        }
    }
}
