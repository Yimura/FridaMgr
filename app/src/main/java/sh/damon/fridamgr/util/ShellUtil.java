package sh.damon.fridamgr.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellUtil {
    public static class ProcessResponse {
        ProcessResponse() {
            status = -1;
            out = "";
        }

        ProcessResponse(int terminationCode) {
            status = terminationCode;
            out = "";
        }

        ProcessResponse(int terminationCode, String output) {
            status = terminationCode;
            out = output;
        }

        public boolean isFail() {
            return status != 0;
        }

        public boolean isSuccess() {
            return status == 0;
        }

        public int status;
        public String out;
    }

    public static boolean areWeSuperuser() {
        try {
            Process su = Runtime.getRuntime().exec("su -v");
            return su.waitFor() == 0;
        }
        catch (IOException | InterruptedException ex) {
            Log.e("ShellUtil", "Failed to execute 'su' binary.");
        }
        return false;
    }

    public static ProcessResponse run(String cmd) {
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            String out = collectOut(proc);
            int terminationCode = proc.waitFor();

            return new ProcessResponse(terminationCode, out);
        }
        catch (IOException | InterruptedException ex) {
            Log.e("ShellUtil", "Failed to execute command.", ex);
        }
        return new ProcessResponse();
    }

    public static ProcessResponse runAsSuperuser(String cmd) {
        try {
            Process proc = Runtime.getRuntime().exec(String.format("su -c %s", cmd));
            String out = collectOut(proc);
            int terminationCode = proc.waitFor();

            return new ProcessResponse(terminationCode, out);
        }
        catch (IOException | InterruptedException ex) {
            Log.e("ShellUtil", "Failed to execute command.", ex);
        }
        return new ProcessResponse();
    }

    private static String collectOut(Process proc) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        char[] buffer = new char[1024];
        StringBuilder output = new StringBuilder();

        int read;
        while (proc.isAlive()) {
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
        }
        reader.close();

        return output.toString();
    }
}
