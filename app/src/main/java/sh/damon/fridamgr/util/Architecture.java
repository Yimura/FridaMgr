package sh.damon.fridamgr.util;

import android.os.Build;

public class Architecture {
    enum ArchType {
        ARM,
        ARM64,
        X86,
        X86_64
    }

    private static ArchType get() {
        final String[] architectures = Build.SUPPORTED_ABIS;

        for (String arch : architectures) {
            if (arch.startsWith("arm64")) {
                return ArchType.ARM64;
            }

            if (arch.startsWith("arm")) {
                return ArchType.ARM;
            }

            if (arch.equals("x86_64")) {
                return ArchType.X86_64;
            }
        }

        return ArchType.X86;
    }

    public static String getString() throws RuntimeException {
        switch (get()) {
            case ARM:
                return "arm";
            case ARM64:
                return "arm64";
            case X86:
                return "x86";
            case X86_64:
                return "x86_64";
        }

        throw new RuntimeException("Unsupported Architecture");
    }
}
