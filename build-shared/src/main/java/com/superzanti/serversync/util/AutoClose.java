package com.superzanti.serversync.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class AutoClose {

    @SafeVarargs
    public static <T extends Closeable> boolean closeResource(T... res) {
        //TODO keep an eye on this for issues
        int errors = 0;

        for (T t : res) {
            try {
                if (t != null) {
                    if (t instanceof Socket) {
                        if (((Socket) t).isClosed()) {
                            t.close();
                        }
                    }

                    t.close();
                }
            } catch (IOException e) {
                System.out.println("Failed to close resource");
                e.printStackTrace();
            }
        }

        return true;
    }

}
