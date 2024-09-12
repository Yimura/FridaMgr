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

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    final ExecutorService executorService =
            new ThreadPoolExecutor(2, 4, 15_000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    FridaServer server;

    Button btnUpdateInstallFrida;
    Button btnServerStateSwitch;

    TextView fridaStateLbl;
    TextView fridaVersionLbl;

    MaterialSwitch switchStartOnBoot;
    MaterialSwitch switchListenOnNetwork;

    TextInputEditText inputPortNumber;

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

        fridaStateLbl = findViewById(R.id.frida_server_state);
        fridaVersionLbl = findViewById(R.id.frida_version);

        btnUpdateInstallFrida = findViewById(R.id.btn_install_update_server);
        btnUpdateInstallFrida.setOnClickListener((View v) -> {
            if (server.getState() == FridaServer.State.NOT_INSTALLED) {
                executorService.execute(server::install);
            }
            else {
                executorService.execute(server::update);
            }
        });

        btnServerStateSwitch = findViewById(R.id.btn_toggle_server);
        btnServerStateSwitch.setOnClickListener((View v) -> {
            final FridaServer.State state = server.getState();
            if (state == FridaServer.State.RUNNING) {
                executorService.execute(server::kill);
            }
            else if (state == FridaServer.State.STOPPED) {
                executorService.execute(server::start);
            }
        });

        switchStartOnBoot = findViewById(R.id.switch_start_on_boot);
        switchListenOnNetwork = findViewById(R.id.switch_listen_on_network);
        inputPortNumber = findViewById(R.id.input_port_number);

        server.setOnUpdateListener(() -> runOnUiThread(this::updateElements));
        updateElements();
    }

    private void updateElements() {
        fridaStateLbl.setText(String.format(getString(R.string.frida_server_state_label), getString(server.getStateStringId())));
        fridaVersionLbl.setText(String.format(getString(R.string.frida_version), server.getVersion()));

        btnUpdateInstallFrida.setEnabled(true);
        btnServerStateSwitch.setEnabled(true);
        btnUpdateInstallFrida.setText(R.string.btn_update_frida);

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
            case UPDATING: {
                btnUpdateInstallFrida.setEnabled(false);
                btnUpdateInstallFrida.setText(String.format(getString(R.string.btn_download_progress), server.getDownloadState().getProgress()));
            }
        }
    }
}