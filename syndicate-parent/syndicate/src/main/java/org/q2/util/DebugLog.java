package org.q2.util;

public final class DebugLog {
    private static boolean enabled = false;

    public static void setEnabled(boolean t) {
        enabled = t;
    }

    public static synchronized void Log(String tag, String msg) {
        if (enabled)
            System.out.println("[" + tag + "] : " + msg);
    }
}
