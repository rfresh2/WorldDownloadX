package wdlx;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.api.Session;
import wdlx.server.WdlxServerSession;

public class WorldDownloadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldDownloadManager.class);
    private DownloadStatus status = DownloadStatus.STOPPED;
    private WdlxServerSession serverSession;

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
//        };

        LOGGER.info("Starting world download server");
        serverSession = new WdlxServerSession(name);
        status = DownloadStatus.STARTED;
    }

    public synchronized void stop() {
        LOGGER.info("Stopping world download");
        try {
            serverSession.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close server session", e);
        }
        status = DownloadStatus.STOPPED;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public @Nullable Session getCurrentSession() {
        return serverSession;
    }

    public enum DownloadStatus {
        STOPPED,
        STARTED
    }
}
