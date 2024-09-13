package sh.damon.fridamgr.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.tukaani.xz.SeekableFileInputStream;
import org.tukaani.xz.SeekableXZInputStream;
import org.tukaani.xz.XZInputStream;

import sh.damon.fridamgr.listener.ProgressListener;

public class Archive {
    public static boolean decompress(File input, File output, ProgressListener listener) {
        if (!input.exists()) {
            return false;
        }

        try {
            final SeekableFileInputStream seekableFileInputStream = new SeekableFileInputStream(input);
            final SeekableXZInputStream in = new SeekableXZInputStream(seekableFileInputStream);

            final FileOutputStream out = new FileOutputStream(output);

            long contentLength = in.length();
            long totalBytesRead = 0;
            final byte[] buffer = new byte[8 * 1024];
            for (int bytesRead; -1 != (bytesRead = in.read(buffer)); ) {
                out.write(buffer, 0, bytesRead);

                totalBytesRead += bytesRead;
                listener.update(totalBytesRead, contentLength, false);
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
