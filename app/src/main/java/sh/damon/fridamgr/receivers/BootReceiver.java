package sh.damon.fridamgr.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import sh.damon.fridamgr.FridaServer;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final FridaServer server = new FridaServer(context.getFilesDir());
            server.start();
        }
    }
}
