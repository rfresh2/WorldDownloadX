package wdlx;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldDownloadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldDownloadManager.class);
    private DownloadStatus status = DownloadStatus.STOPPED;

    public synchronized void start(String name) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            LOGGER.error("No world loaded");
            return;
        }
//        if (mc.isSingleplayer()) {
//            LOGGER.error("Singleplayer not supported");
//            return;
//        }
        LOGGER.info("Starting world download");
    }

    public synchronized void stop() {
        LOGGER.info("Stopping world download");
        status = DownloadStatus.STOPPED;
    }

    public enum DownloadStatus {
        STOPPED,
        STARTED
    }
}
