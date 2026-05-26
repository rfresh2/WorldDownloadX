package wdlx.api;

import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.concurrent.CompletableFuture;

public interface Session {
    boolean active();
    CompletableFuture<LongSet> savedChunks();
    CompletableFuture<LongSet> newlySavedChunks();
}
