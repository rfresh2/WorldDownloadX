package wdlx;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.world.WdlSession;

public class WorldDownloadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldDownloadManager.class);
    private DownloadStatus status = DownloadStatus.STOPPED;
    private WdlSession currentSession;

    public synchronized void start(String name) {
        if (status != DownloadStatus.STOPPED) {
            LOGGER.error("World download already in progress");
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            LOGGER.error("No world loaded");
            return;
        }
//        if (mc.isSingleplayer()) {
//            LOGGER.error("Singleplayer not supported");
//            return;
//        }
        currentSession = new WdlSession(name);
        LOGGER.info("Started world download: '{}'", name);
    }

    public synchronized void stop() {
        LOGGER.info("Stopping world download");
        try {
            currentSession.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close session", e);
        }
        status = DownloadStatus.STOPPED;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public @Nullable WdlSession getCurrentSession() {
        return currentSession;
    }

    public enum DownloadStatus {
        STOPPED,
        STARTED
    }
}
