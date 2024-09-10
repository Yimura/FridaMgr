package sh.damon.fridamgr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    final FridaServer server = new FridaServer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView fridaStateLbl = findViewById(R.id.frida_server_state);
        fridaStateLbl.setText(String.format(getString(R.string.frida_server_state_label), getString(server.getStateStringId())));

        Button btnUpdateServer = findViewById(R.id.btn_update_server);
        btnUpdateServer.setOnClickListener((View v) -> {
            Toast.makeText(this, "Clicked update server.", Toast.LENGTH_SHORT).show();
        });

        Button btnKillServer = findViewById(R.id.btn_kill_server);
        btnKillServer.setOnClickListener((View v) -> {
            Toast.makeText(this, "Clicked kill server.", Toast.LENGTH_SHORT).show();
        });
    }
}