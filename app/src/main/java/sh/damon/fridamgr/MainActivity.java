package sh.damon.fridamgr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    FridaServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        server = new FridaServer(getFilesDir());

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        server.setOnUpdateListener(this::updateElements);

        Button btnUpdateServer = findViewById(R.id.btn_update_server);
        btnUpdateServer.setOnClickListener((View v) -> {
            server.install();
        });

        Button btnServerStateSwitch = findViewById(R.id.btn_toggle_server);
        btnServerStateSwitch.setOnClickListener((View v) -> {
            final FridaServer.State state = server.getState();
            if (state == FridaServer.State.RUNNING) {
                server.kill();
            }
            else if (state == FridaServer.State.STOPPED) {
                server.start();
            }
        });

        updateElements();
    }

    private void updateElements() {
        TextView fridaStateLbl = findViewById(R.id.frida_server_state);
        fridaStateLbl.setText(String.format(getString(R.string.frida_server_state_label), getString(server.getStateStringId())));

        Button btnUpdateInstallFrida = findViewById(R.id.btn_update_server);
        btnUpdateInstallFrida.setText(R.string.btn_update_frida); // default, will be overridden if required in NOT_INSTALLED

        Button btnServerStateSwitch = findViewById(R.id.btn_toggle_server);
        btnServerStateSwitch.setEnabled(true);

        switch (server.getState()) {
            case RUNNING: {
                btnServerStateSwitch.setText(R.string.btn_kill_frida_server);

                break;
            }
            case STOPPED: {
                btnServerStateSwitch.setText(R.string.btn_start_frida_server);

                break;
            }
            case NOT_INSTALLED: {
                btnServerStateSwitch.setEnabled(false);
                btnUpdateInstallFrida.setText(R.string.btn_install_frida);

                break;
            }
        }
    }
}