package sh.damon.fridamgr.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.tukaani.xz.XZInputStream;

public class Archive {
    public static boolean decompress(File input, File output) {
        if (!input.exists()) {
            return false;
        }

        try {
            final XZInputStream in = new XZInputStream(new BufferedInputStream(Files.newInputStream(input.toPath())));
            final FileOutputStream out = new FileOutputStream(output);

            final byte[] buffer = new byte[8192];
            for (int n; -1 != (n = in.read(buffer)); ) {
                out.write(buffer, 0, n);
            }
            out.close();
            in.close();
        }
        catch(IOException e) {
            Log.e("Decompress", "unzip", e);

            return false;
        }

        return true;
    }
}
