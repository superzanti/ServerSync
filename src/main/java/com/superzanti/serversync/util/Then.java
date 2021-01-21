package com.superzanti.serversync.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Then {
    private static final ExecutorService ex = Executors.newFixedThreadPool(8);
    public static <T> void onComplete(Callable<T> cb, Consumer<T> consumer) {
        ex.submit(() -> {
            T value = null;
            try {
                value = ex.submit(cb).get();
            } catch (InterruptedException | ExecutionException e) {
                ServerSyncLogger.debug(e);
            }
            consumer.accept(value);
        });
    }
}
